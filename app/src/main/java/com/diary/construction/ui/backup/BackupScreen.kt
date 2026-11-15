package com.diary.construction.ui.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diary.construction.R
import com.diary.construction.core.util.Dates
import com.diary.construction.data.backup.BackupManager
import com.diary.construction.ui.components.AppTopBar
import com.diary.construction.ui.components.InfoCard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val manager: BackupManager,
) : ViewModel() {
    var busy by mutableStateOf(false)
        private set
    var result by mutableStateOf<BackupManager.Result?>(null)
        private set
    var lastWasBackup by mutableStateOf(true)
        private set

    fun fileName() = "construction_diary_backup_${Dates.fileStamp()}.zip"

    fun backup(uri: Uri) = launchTask(true) { manager.backup(uri) }

    fun restore(uri: Uri) = launchTask(false) { manager.restore(uri) }

    fun clearResult() {
        result = null
    }

    private fun launchTask(isBackup: Boolean, block: suspend () -> BackupManager.Result) {
        if (busy) return
        busy = true
        lastWasBackup = isBackup
        viewModelScope.launch {
            result = block()
            busy = false
        }
    }
}

@Composable
fun BackupScreen(
    onBack: () -> Unit,
    viewModel: BackupViewModel = hiltViewModel(),
) {
    var confirmRestore by remember { mutableStateOf<Uri?>(null) }
    val create = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) viewModel.backup(uri)
    }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) confirmRestore = uri
    }

    Scaffold(topBar = { AppTopBar(stringResource(R.string.backup_title), onBack = onBack) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            InfoCard {
                Text(stringResource(R.string.backup_create), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.backup_create_desc), style = MaterialTheme.typography.bodyMedium)
                Button(
                    onClick = { runCatching { create.launch(viewModel.fileName()) } },
                    enabled = !viewModel.busy,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) {
                    Icon(Icons.Outlined.Backup, contentDescription = null)
                    Text("  " + stringResource(R.string.backup_create))
                }
            }
            InfoCard {
                Text(stringResource(R.string.backup_restore), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.backup_restore_desc), style = MaterialTheme.typography.bodyMedium)
                OutlinedButton(
                    onClick = { runCatching { open.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream")) } },
                    enabled = !viewModel.busy,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) {
                    Icon(Icons.Outlined.Restore, contentDescription = null)
                    Text("  " + stringResource(R.string.backup_restore))
                }
            }
            Text(
                stringResource(R.string.backup_tip),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (viewModel.busy) CircularProgressIndicator()
        }
    }

    confirmRestore?.let { uri ->
        AlertDialog(
            onDismissRequest = { confirmRestore = null },
            title = { Text(stringResource(R.string.backup_restore)) },
            text = { Text(stringResource(R.string.backup_restore_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmRestore = null
                    viewModel.restore(uri)
                }) { Text(stringResource(R.string.backup_restore_now)) }
            },
            dismissButton = { TextButton(onClick = { confirmRestore = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }

    viewModel.result?.let { r ->
        val message = when (r) {
            BackupManager.Result.Ok -> stringResource(if (viewModel.lastWasBackup) R.string.backup_done else R.string.backup_restored)
            BackupManager.Result.NotABackup -> stringResource(R.string.backup_not_valid)
            is BackupManager.Result.Failed -> stringResource(R.string.backup_failed, r.message)
        }
        AlertDialog(
            onDismissRequest = viewModel::clearResult,
            text = { Text(message) },
            confirmButton = { TextButton(onClick = viewModel::clearResult) { Text(stringResource(R.string.action_ok)) } },
        )
    }
}
