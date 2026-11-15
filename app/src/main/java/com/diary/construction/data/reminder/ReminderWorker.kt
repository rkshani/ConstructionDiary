package com.diary.construction.data.reminder

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.diary.construction.R
import com.diary.construction.core.util.Dates
import com.diary.construction.core.util.Money
import com.diary.construction.data.settings.LocaleStore
import com.diary.construction.domain.model.MaterialEntryType
import com.diary.construction.domain.model.ProjectStatus
import com.diary.construction.domain.model.StageType
import com.diary.construction.domain.repository.LabourRepository
import com.diary.construction.domain.repository.MaterialRepository
import com.diary.construction.domain.repository.ProjectRepository
import com.diary.construction.domain.repository.SettingsRepository
import com.diary.construction.domain.repository.StageRepository
import com.diary.construction.domain.repository.SupplierRepository
import com.diary.construction.ui.components.labelRes
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Runs once a day and shows reminders:
 * - labour weekly payment (on the chosen pay day) when wages are due,
 * - supplier payments whose promised date has come and still have a balance,
 * - stages whose deadline is within 2 days or already passed.
 */
@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val settings: SettingsRepository,
    private val projects: ProjectRepository,
    private val labour: LabourRepository,
    private val suppliers: SupplierRepository,
    private val material: MaterialRepository,
    private val stages: StageRepository,
    private val notifier: ReminderNotifier,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val st = settings.current()
        val ctx = LocaleStore.wrap(applicationContext)
        val today = Dates.today()
        runCatching { if (st.labourReminder && Dates.dayOfWeek(today) == st.labourPayDay) labourReminder(ctx, st.currency) }
        runCatching { if (st.supplierReminder) supplierReminder(ctx, st.currency, today) }
        runCatching { if (st.stageReminder) stageReminder(ctx, today) }
        return Result.success()
    }

    private suspend fun labourReminder(ctx: Context, currency: String) {
        val lines = projects.getProjects().filter { it.status != ProjectStatus.COMPLETED }.mapNotNull { p ->
            val attendance = labour.getAttendance(p.id).groupBy { it.labourId }
            val paid = labour.getPayments(p.id).groupBy { it.labourId }
            val due = labour.getLabours(p.id).sumOf { l ->
                (attendance[l.id].orEmpty().sumOf { it.earned } - paid[l.id].orEmpty().sumOf { it.amount }).coerceAtLeast(0)
            }
            if (due > 0) "${p.name}: ${Money.format(due, currency)}" else null
        }
        if (lines.isNotEmpty()) {
            notifier.show(ReminderNotifier.ID_LABOUR, ctx.getString(R.string.reminder_labour_title), lines.joinToString("\n"))
        }
    }

    private suspend fun supplierReminder(ctx: Context, currency: String, today: Long) {
        val lines = suppliers.getSuppliers().filter { it.paymentDueDate != null && it.paymentDueDate <= today }.mapNotNull { s ->
            val taken = material.getEntriesForSupplier(s.id).filter { it.entryType == MaterialEntryType.RECEIVED }.sumOf { it.total }
            val balance = taken - suppliers.getPayments(s.id).sumOf { it.amount }
            if (balance > 0) "${s.name}: ${Money.format(balance, currency)}" else null
        }
        if (lines.isNotEmpty()) {
            notifier.show(ReminderNotifier.ID_SUPPLIER, ctx.getString(R.string.reminder_supplier_title), lines.joinToString("\n"))
        }
    }

    private suspend fun stageReminder(ctx: Context, today: Long) {
        val names = projects.getProjects().associate { it.id to it.name }
        val list = stages.getOpenStagesEndingBefore(Dates.endOfDay(Dates.addDays(today, 2)))
        if (list.isEmpty()) return
        val lines = list.sortedBy { it.endDate }.map { st ->
            val stageName = if (st.type == StageType.CUSTOM || st.name.isNotBlank()) st.name.ifBlank { ctx.getString(st.type.labelRes) } else ctx.getString(st.type.labelRes)
            val end = st.endDate ?: today
            val whenText = if (end < today) ctx.getString(R.string.reminder_overdue) else Dates.format(end)
            "${names[st.projectId].orEmpty()} – $stageName: $whenText"
        }
        notifier.show(ReminderNotifier.ID_STAGE, ctx.getString(R.string.reminder_stage_title), lines.joinToString("\n"))
    }
}
