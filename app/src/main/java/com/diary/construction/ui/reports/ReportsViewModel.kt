package com.diary.construction.ui.reports

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diary.construction.core.util.Dates
import com.diary.construction.data.report.PdfReportWriter
import com.diary.construction.data.report.ReportBuilder
import com.diary.construction.domain.model.DateRange
import com.diary.construction.domain.model.Project
import com.diary.construction.domain.model.Report
import com.diary.construction.domain.model.ReportType
import com.diary.construction.domain.repository.SettingsRepository
import com.diary.construction.domain.usecase.ObserveSelectedProjectUseCase
import com.diary.construction.domain.usecase.SelectedProject
import com.diary.construction.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

enum class RangePreset { ALL, THIS_MONTH, LAST_MONTH, LAST_30_DAYS, CUSTOM }

data class RangeState(
    val preset: RangePreset = RangePreset.ALL,
    val customFrom: Long = Dates.startOfMonth(Dates.today()),
    val customTo: Long = Dates.today(),
) {
    fun toRange(): DateRange {
        val today = Dates.today()
        return when (preset) {
            RangePreset.ALL -> DateRange()
            RangePreset.THIS_MONTH -> DateRange(Dates.startOfMonth(today), Dates.endOfDay(today))
            RangePreset.LAST_MONTH -> {
                val thisMonth = Dates.startOfMonth(today)
                val lastMonth = Dates.startOfMonth(Dates.addDays(thisMonth, -1))
                DateRange(lastMonth, thisMonth - 1)
            }
            RangePreset.LAST_30_DAYS -> DateRange(Dates.addDays(today, -29), Dates.endOfDay(today))
            RangePreset.CUSTOM -> DateRange(minOf(customFrom, customTo), Dates.endOfDay(maxOf(customFrom, customTo)))
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ReportsViewModel @Inject constructor(
    savedState: SavedStateHandle,
    selected: ObserveSelectedProjectUseCase,
    private val builder: ReportBuilder,
    private val pdf: PdfReportWriter,
    private val settings: SettingsRepository,
) : ViewModel() {
    private val routeProjectId: Long = savedState.get<Long>(Routes.ARG_PROJECT_ID) ?: 0L
    val canSwitchProject = routeProjectId == 0L

    val type = MutableStateFlow(ReportType.SUMMARY)
    val range = MutableStateFlow(RangeState())

    val selection: StateFlow<SelectedProject?> = selected(routeProjectId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** null while building. */
    val report: StateFlow<Report?> = combine(selected(routeProjectId), type, range) { sel, t, r -> Triple(sel.project, t, r) }
        .mapLatest { (project, t, r) -> project?.let { builder.build(it.id, t, r.toRange()) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun selectProject(project: Project) {
        viewModelScope.launch { settings.setSelectedProject(project.id) }
    }

    /** Builds the PDF for the current report. */
    suspend fun makePdf(): File? {
        val r = report.value ?: return null
        val safeName = r.projectName.replace(Regex("[^A-Za-z0-9]+"), "_").trim('_').ifBlank { "project" }
        return pdf.write(r, "${safeName}_${type.value.name.lowercase()}_${Dates.fileStamp()}.pdf")
    }
}
