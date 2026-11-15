package com.diary.construction.ui.reports

import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.diary.construction.R
import com.diary.construction.core.util.ShareFiles
import com.diary.construction.data.report.ReportBuilder.Companion.titleRes
import com.diary.construction.domain.model.Report
import com.diary.construction.domain.model.ReportSection
import com.diary.construction.domain.model.ReportType
import com.diary.construction.ui.components.AppTopBar
import com.diary.construction.ui.components.DateField
import com.diary.construction.ui.components.EmptyState
import com.diary.construction.ui.components.InfoCard
import com.diary.construction.ui.components.LabelValue
import com.diary.construction.ui.components.ProjectPicker
import kotlinx.coroutines.launch

@Composable
fun ReportsScreen(
    onBack: (() -> Unit)?,
    onAddProject: () -> Unit,
    viewModel: ReportsViewModel = hiltViewModel(),
    extraActions: @Composable (report: Report) -> Unit = {},
) {
    val selection by viewModel.selection.collectAsStateWithLifecycle()
    val type by viewModel.type.collectAsStateWithLifecycle()
    val range by viewModel.range.collectAsStateWithLifecycle()
    val report by viewModel.report.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    val shareTitle = stringResource(R.string.share_report)
    val whatsappMissing = stringResource(R.string.whatsapp_missing)

    fun withPdf(action: (java.io.File) -> Unit) {
        if (busy) return
        busy = true
        scope.launch {
            val file = runCatching { viewModel.makePdf() }.getOrNull()
            busy = false
            if (file != null) action(file)
        }
    }

    Scaffold(
        topBar = { AppTopBar(stringResource(R.string.tab_reports), onBack = onBack) },
        bottomBar = {
            val r = report
            if (r != null) {
                Surface(tonalElevation = 3.dp) {
                    Row(
                        Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Button(onClick = { withPdf { ShareFiles.share(context, it, "application/pdf", shareTitle) } }, enabled = !busy) {
                            Icon(Icons.Outlined.Share, contentDescription = null)
                            Text("  PDF")
                        }
                        OutlinedButton(onClick = {
                            withPdf {
                                if (!ShareFiles.shareToWhatsApp(context, it, "application/pdf")) {
                                    Toast.makeText(context, whatsappMissing, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }, enabled = !busy) {
                            Icon(Icons.AutoMirrored.Outlined.Chat, contentDescription = null)
                            Text("  " + stringResource(R.string.action_whatsapp))
                        }
                        OutlinedButton(onClick = { withPdf { ShareFiles.open(context, it, "application/pdf") } }, enabled = !busy) {
                            Icon(Icons.Outlined.PictureAsPdf, contentDescription = stringResource(R.string.open_pdf))
                        }
                        extraActions(r)
                        if (busy) CircularProgressIndicator(Modifier.width(24.dp))
                    }
                }
            }
        },
    ) { padding ->
        val sel = selection ?: return@Scaffold
        val project = sel.project
        if (project == null) {
            EmptyState(
                title = stringResource(R.string.no_project_title),
                message = stringResource(R.string.no_project_message),
                actionLabel = stringResource(R.string.project_new),
                onAction = onAddProject,
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (viewModel.canSwitchProject && sel.projects.size > 1) {
                item { ProjectPicker(sel.projects, project, onSelect = viewModel::selectProject) }
            }
            item {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReportType.entries.forEach { t ->
                        FilterChip(selected = type == t, onClick = { viewModel.type.value = t }, label = { Text(stringResource(t.titleRes())) })
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RangePreset.entries.forEach { p ->
                        FilterChip(
                            selected = range.preset == p,
                            onClick = { viewModel.range.value = range.copy(preset = p) },
                            label = { Text(stringResource(p.labelRes())) },
                        )
                    }
                }
            }
            if (range.preset == RangePreset.CUSTOM) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DateField(
                            label = stringResource(R.string.range_from_label),
                            value = range.customFrom,
                            onValueChange = { v -> if (v != null) viewModel.range.value = range.copy(customFrom = v) },
                            modifier = Modifier.weight(1f),
                        )
                        DateField(
                            label = stringResource(R.string.range_to_label),
                            value = range.customTo,
                            onValueChange = { v -> if (v != null) viewModel.range.value = range.copy(customTo = v) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            val r = report
            if (r == null) {
                item { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
            } else {
                item {
                    Column {
                        Text(r.title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                        Text(r.projectName + " · " + r.period, style = MaterialTheme.typography.bodySmall)
                    }
                }
                items(r.sections) { section -> SectionPreview(section) }
            }
        }
    }
}

@Composable
private fun SectionPreview(section: ReportSection) {
    InfoCard {
        if (section.title.isNotBlank()) Text(section.title, style = MaterialTheme.typography.titleSmall)
        section.facts.forEach { (label, value) -> LabelValue(label, value) }
        if (section.columns.isNotEmpty()) {
            Column(Modifier.horizontalScroll(rememberScrollState())) {
                TableRow(section.columns, section.numericColumns, bold = true)
                HorizontalDivider()
                if (section.rows.isEmpty()) {
                    Text(stringResource(R.string.r_nothing), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 6.dp))
                }
                section.rows.forEach { TableRow(it, section.numericColumns) }
                section.totalRow?.let {
                    HorizontalDivider()
                    TableRow(it, section.numericColumns, bold = true)
                }
            }
        }
    }
}

@Composable
private fun TableRow(cells: List<String>, numeric: Set<Int>, bold: Boolean = false) {
    Row(Modifier.padding(vertical = 3.dp)) {
        cells.forEachIndexed { i, text ->
            Text(
                text,
                modifier = Modifier.width(if (i == 0) 130.dp else 96.dp).padding(end = 6.dp),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
                textAlign = if (i in numeric) TextAlign.End else TextAlign.Start,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun RangePreset.labelRes(): Int = when (this) {
    RangePreset.ALL -> R.string.range_all
    RangePreset.THIS_MONTH -> R.string.range_this_month
    RangePreset.LAST_MONTH -> R.string.range_last_month
    RangePreset.LAST_30_DAYS -> R.string.range_last_30
    RangePreset.CUSTOM -> R.string.range_custom
}
