package com.diary.construction.ui.photos

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.diary.construction.R
import com.diary.construction.core.util.Dates
import com.diary.construction.data.photo.PhotoStorage
import com.diary.construction.domain.model.Photo
import com.diary.construction.domain.model.Stage
import com.diary.construction.domain.repository.PhotoRepository
import com.diary.construction.domain.repository.StageRepository
import com.diary.construction.ui.components.AppTopBar
import com.diary.construction.ui.components.ConfirmDeleteDialog
import com.diary.construction.ui.components.DateField
import com.diary.construction.ui.components.DropdownField
import com.diary.construction.ui.components.EmptyState
import com.diary.construction.ui.components.FormTextField
import com.diary.construction.ui.components.FullImageDialog
import com.diary.construction.ui.components.LocalImage
import com.diary.construction.ui.components.PhotoSourceButtons
import com.diary.construction.ui.components.displayName
import com.diary.construction.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Photos of one day. */
data class PhotoDay(val date: Long, val photos: List<Photo>)

data class PhotosState(val stages: List<Stage>, val days: List<PhotoDay>, val filter: Long?)

@HiltViewModel
class PhotosViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val photoRepo: PhotoRepository,
    stageRepo: StageRepository,
    private val storage: PhotoStorage,
) : ViewModel() {
    val projectId: Long = checkNotNull(savedState.get<Long>(Routes.ARG_PROJECT_ID))
    private val stageFilter = MutableStateFlow<Long?>(null)

    /** A photo copied into storage but not saved yet (waiting for the details dialog). */
    var pendingPath by mutableStateOf<String?>(null)
        private set

    val state: StateFlow<PhotosState?> = combine(
        photoRepo.observePhotos(projectId),
        stageRepo.observeStages(projectId),
        stageFilter,
    ) { photos, stages, filter ->
        val days = photos.filter { filter == null || it.stageId == filter }
            .groupBy { it.date }
            .toSortedMap(compareByDescending { it })
            .map { (date, list) -> PhotoDay(date, list) }
        PhotosState(stages, days, filter)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setFilter(stageId: Long?) {
        stageFilter.value = stageId
    }

    fun newCameraUri(): Uri = storage.newCameraUri()

    fun onPicked(uri: Uri) {
        viewModelScope.launch { pendingPath = storage.import(uri) }
    }

    fun savePending(date: Long, stageId: Long?, caption: String) {
        val path = pendingPath ?: return
        pendingPath = null
        viewModelScope.launch {
            photoRepo.save(Photo(projectId = projectId, stageId = stageId, date = date, path = path, caption = caption.trim()))
        }
    }

    fun discardPending() {
        val path = pendingPath ?: return
        pendingPath = null
        viewModelScope.launch { storage.delete(listOf(path)) }
    }

    fun update(photo: Photo) {
        viewModelScope.launch { photoRepo.save(photo) }
    }

    fun delete(photo: Photo) {
        viewModelScope.launch {
            photoRepo.delete(photo)
            storage.delete(listOf(photo.path))
        }
    }
}

@Composable
fun PhotosScreen(
    onBack: () -> Unit,
    viewModel: PhotosViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var viewing by remember { mutableStateOf<Photo?>(null) }
    var editing by remember { mutableStateOf<Photo?>(null) }
    var deleting by remember { mutableStateOf<Photo?>(null) }
    val s = state

    Scaffold(
        topBar = { AppTopBar(stringResource(R.string.section_photos), onBack = onBack) },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                PhotoSourceButtons(
                    onPicked = viewModel::onPicked,
                    newCameraUri = viewModel::newCameraUri,
                    modifier = Modifier.fillMaxWidth().padding(12.dp).navigationBarsPadding(),
                )
            }
        },
    ) { padding ->
        if (s == null) return@Scaffold
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (s.stages.isNotEmpty()) {
                item {
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = s.filter == null, onClick = { viewModel.setFilter(null) }, label = { Text(stringResource(R.string.filter_all)) })
                        s.stages.forEach { st ->
                            FilterChip(selected = s.filter == st.id, onClick = { viewModel.setFilter(st.id) }, label = { Text(st.displayName()) })
                        }
                    }
                }
            }
            if (s.days.isEmpty()) {
                item {
                    EmptyState(
                        title = stringResource(R.string.photos_empty),
                        message = stringResource(R.string.photos_empty_message),
                        icon = Icons.Outlined.PhotoLibrary,
                        modifier = Modifier.padding(top = 32.dp),
                    )
                }
            }
            items(s.days, key = { it.date }) { day ->
                TimelineDay(day, s.stages, onOpen = { viewing = it })
            }
        }
    }

    viewModel.pendingPath?.let { path ->
        PhotoDetailsDialog(
            path = path,
            stages = s?.stages.orEmpty(),
            initial = Photo(projectId = viewModel.projectId, stageId = s?.filter, date = Dates.today(), path = path),
            onSave = { viewModel.savePending(it.date, it.stageId, it.caption) },
            onDismiss = viewModel::discardPending,
        )
    }
    viewing?.let { photo ->
        PhotoViewer(
            photo = photo,
            stageName = s?.stages?.firstOrNull { it.id == photo.stageId }?.displayName(),
            onEdit = { editing = photo; viewing = null },
            onDelete = { deleting = photo; viewing = null },
            onDismiss = { viewing = null },
        )
    }
    editing?.let { photo ->
        PhotoDetailsDialog(
            path = photo.path,
            stages = s?.stages.orEmpty(),
            initial = photo,
            onSave = { viewModel.update(photo.copy(date = it.date, stageId = it.stageId, caption = it.caption.trim())); editing = null },
            onDismiss = { editing = null },
        )
    }
    deleting?.let { photo ->
        ConfirmDeleteDialog(
            message = stringResource(R.string.photo_delete_message),
            onConfirm = { viewModel.delete(photo) },
            onDismiss = { deleting = null },
        )
    }
}

/** A dot + date on the left line, then the day's photos. */
@Composable
private fun TimelineDay(day: PhotoDay, stages: List<Stage>, onOpen: (Photo) -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(12.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
            Text(
                Dates.formatDay(day.date) + "  ·  " + stringResource(R.string.photos_count, day.photos.size),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        val stageNames = day.photos.mapNotNull { p -> stages.firstOrNull { it.id == p.stageId } }.distinct()
        if (stageNames.isNotEmpty()) {
            Text(
                stageNames.map { it.displayName() }.joinToString(", "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 20.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        day.photos.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { photo ->
                    LocalImage(
                        photo.path,
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onOpen(photo) },
                        maxSide = 400,
                    )
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun PhotoViewer(
    photo: Photo,
    stageName: String?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var full by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Dates.format(photo.date)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LocalImage(
                    photo.path,
                    Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp)).clickable { full = true },
                    maxSide = 900,
                )
                if (stageName != null) Text(stageName, style = MaterialTheme.typography.labelLarge)
                if (photo.caption.isNotBlank()) Text(photo.caption)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) } },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) { Text(stringResource(R.string.action_delete)) }
                TextButton(onClick = onEdit) { Text(stringResource(R.string.action_edit)) }
            }
        },
    )
    if (full) FullImageDialog(photo.path) { full = false }
}

@Composable
private fun PhotoDetailsDialog(
    path: String,
    stages: List<Stage>,
    initial: Photo,
    onSave: (Photo) -> Unit,
    onDismiss: () -> Unit,
) {
    var date by remember { mutableStateOf(initial.date) }
    var stageId by remember { mutableStateOf(initial.stageId) }
    var caption by remember { mutableStateOf(initial.caption) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.photo_details)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LocalImage(path, Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(12.dp)))
                DateField(label = stringResource(R.string.date), value = date, onValueChange = { if (it != null) date = it })
                if (stages.isNotEmpty()) {
                    DropdownField(
                        label = stringResource(R.string.stage_type),
                        options = listOf<Stage?>(null) + stages,
                        selected = stages.firstOrNull { it.id == stageId },
                        optionLabel = { it?.displayName() ?: stringResource(R.string.none) },
                        onSelect = { stageId = it?.id },
                    )
                }
                FormTextField(value = caption, onValueChange = { caption = it }, label = stringResource(R.string.photo_caption))
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(initial.copy(date = date, stageId = stageId, caption = caption)) }) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
