package com.diary.construction.ui.labour

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diary.construction.core.util.Dates
import com.diary.construction.domain.model.Attendance
import com.diary.construction.domain.model.AttendanceStatus
import com.diary.construction.domain.model.Labour
import com.diary.construction.domain.model.LabourPayment
import com.diary.construction.domain.model.LabourPaymentType
import com.diary.construction.domain.model.LabourSummary
import com.diary.construction.domain.repository.LabourRepository
import com.diary.construction.domain.usecase.ObserveLabourSummariesUseCase
import com.diary.construction.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One labour on the attendance sheet for a day. */
data class DayRow(val labour: Labour, val status: AttendanceStatus?)

/** One labour in the weekly settlement. */
data class WeekRow(
    val summary: LabourSummary,
    val days: Double,
    val earnedThisWeek: Long,
    val paidThisWeek: Long,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LabourViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val labourRepo: LabourRepository,
    observeSummaries: ObserveLabourSummariesUseCase,
) : ViewModel() {
    val projectId: Long = checkNotNull(savedState.get<Long>(Routes.ARG_PROJECT_ID))

    val day = MutableStateFlow(Dates.today())
    val weekStart = MutableStateFlow(Dates.startOfWeek(Dates.today()))

    val summaries: StateFlow<List<LabourSummary>?> = observeSummaries(projectId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val dayRows: StateFlow<List<DayRow>> = combine(
        labourRepo.observeLabours(projectId),
        day.flatMapLatest { labourRepo.observeAttendanceForDay(projectId, it) },
    ) { labours, attendance ->
        val byLabour = attendance.associateBy { it.labourId }
        labours.filter { it.active || byLabour.containsKey(it.id) }.map { DayRow(it, byLabour[it.id]?.status) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val weekRows: StateFlow<List<WeekRow>> = combine(
        observeSummaries(projectId),
        weekStart.flatMapLatest { from ->
            labourRepo.observeAttendanceInRange(projectId, from, Dates.endOfDay(Dates.addDays(from, 6)))
        },
        weekStart.flatMapLatest { from ->
            labourRepo.observePaymentsInRange(projectId, from, Dates.endOfDay(Dates.addDays(from, 6)))
        },
    ) { summaries, attendance, payments ->
        val att = attendance.groupBy { it.labourId }
        val pay = payments.groupBy { it.labourId }
        summaries.filter { it.labour.active || att.containsKey(it.labour.id) || it.balance != 0L }.map { s ->
            val a = att[s.labour.id].orEmpty()
            WeekRow(
                summary = s,
                days = a.sumOf { it.status.wagePercent } / 100.0,
                earnedThisWeek = a.sumOf { it.earned },
                paidThisWeek = pay[s.labour.id].orEmpty().sumOf { it.amount },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun moveDay(by: Int) {
        val next = Dates.addDays(day.value, by)
        if (next <= Dates.today()) day.value = next
    }

    fun setDay(value: Long) {
        day.value = Dates.startOfDay(value).coerceAtMost(Dates.today())
    }

    fun moveWeek(by: Int) {
        val next = Dates.addDays(weekStart.value, 7 * by)
        if (next <= Dates.today()) weekStart.value = next
    }

    /** Tapping the selected status again clears the day. */
    fun mark(labour: Labour, status: AttendanceStatus, current: AttendanceStatus?) {
        val date = day.value
        viewModelScope.launch {
            if (status == current) {
                labourRepo.clearAttendance(labour.id, date)
            } else {
                labourRepo.setAttendance(Attendance(labourId = labour.id, projectId = projectId, date = date, status = status, wage = labour.dailyWage))
            }
        }
    }

    fun markAllPresent() {
        val date = day.value
        val rows = dayRows.value
        viewModelScope.launch {
            rows.filter { it.status == null && it.labour.active }.forEach {
                labourRepo.setAttendance(
                    Attendance(labourId = it.labour.id, projectId = projectId, date = date, status = AttendanceStatus.PRESENT, wage = it.labour.dailyWage),
                )
            }
        }
    }

    /** Pays the full balance of one labour as a weekly settlement. */
    fun settle(row: WeekRow) {
        val amount = row.summary.balance
        if (amount <= 0) return
        val date = minOf(Dates.today(), Dates.addDays(weekStart.value, 6))
        viewModelScope.launch {
            labourRepo.addPayment(
                LabourPayment(labourId = row.summary.labour.id, projectId = projectId, date = date, amount = amount, type = LabourPaymentType.SETTLEMENT),
            )
        }
    }
}
