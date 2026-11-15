package com.diary.construction.data.report

import android.content.Context
import com.diary.construction.R
import com.diary.construction.core.util.Dates
import com.diary.construction.core.util.Money
import com.diary.construction.core.util.Quantity
import com.diary.construction.data.settings.LocaleStore
import com.diary.construction.domain.model.DateRange
import com.diary.construction.domain.model.MaterialEntryType
import com.diary.construction.domain.model.MaterialKind
import com.diary.construction.domain.model.MaterialType
import com.diary.construction.domain.model.Project
import com.diary.construction.domain.model.Report
import com.diary.construction.domain.model.ReportSection
import com.diary.construction.domain.model.ReportType
import com.diary.construction.domain.model.Stage
import com.diary.construction.domain.model.StageType
import com.diary.construction.domain.model.UserRole
import com.diary.construction.domain.repository.ContractorRepository
import com.diary.construction.domain.repository.ExpenseRepository
import com.diary.construction.domain.repository.LabourRepository
import com.diary.construction.domain.repository.MaterialRepository
import com.diary.construction.domain.repository.ProjectRepository
import com.diary.construction.domain.repository.SettingsRepository
import com.diary.construction.domain.repository.StageRepository
import com.diary.construction.domain.repository.SupplierRepository
import com.diary.construction.ui.components.labelRes
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Builds [Report]s from the database. Texts use the app language. */
@Singleton
class ReportBuilder @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val projects: ProjectRepository,
    private val stages: StageRepository,
    private val material: MaterialRepository,
    private val suppliers: SupplierRepository,
    private val labour: LabourRepository,
    private val contractors: ContractorRepository,
    private val expenses: ExpenseRepository,
    private val settings: SettingsRepository,
) {
    private lateinit var ctx: Context
    private var currency = ""
    private var role: UserRole? = null

    private fun s(id: Int, vararg args: Any): String = ctx.getString(id, *args)
    private fun m(paisa: Long) = Money.format(paisa, currency)

    private fun Stage.label() = if (type == StageType.CUSTOM || name.isNotBlank()) name.ifBlank { s(type.labelRes) } else s(type.labelRes)
    private fun MaterialType.label() = if (kind == MaterialKind.CUSTOM || name.isNotBlank()) name.ifBlank { s(kind.labelRes) } else s(kind.labelRes)

    suspend fun build(projectId: Long, type: ReportType, range: DateRange): Report? {
        ctx = LocaleStore.wrap(appContext)
        val st = settings.current()
        currency = st.currency
        role = st.role
        val project = projects.getProject(projectId) ?: return null
        val sections = when (type) {
            ReportType.SUMMARY -> summary(project, range)
            ReportType.STAGES -> stageSections(project, range)
            ReportType.MATERIAL -> materialSections(project, range)
            ReportType.LABOUR -> labourSections(project, range)
            ReportType.SUPPLIERS -> supplierSections(project, range)
            ReportType.CONTRACTORS -> contractorSections(project, range)
            ReportType.EXPENSES -> expenseSections(project, range)
        }
        return Report(
            title = s(type.titleRes()),
            projectName = project.name,
            period = periodText(range),
            generatedOn = Dates.format(Dates.now()),
            sections = sections,
        )
    }

    private fun periodText(range: DateRange): String {
        val from = range.from
        val to = range.to
        return when {
            from == null && to == null -> s(R.string.range_all)
            from == null -> s(R.string.range_until, Dates.format(to!!))
            to == null -> s(R.string.range_from, Dates.format(from))
            else -> "${Dates.format(from)} – ${Dates.format(to)}"
        }
    }

    private fun contractorWord() = if (role == UserRole.THEKEDAR) s(R.string.cat_contractor_thekedar) else s(R.string.cat_contractor_owner)

    // ---------------------------------------------------------------- summary

    private suspend fun summary(p: Project, r: DateRange): List<ReportSection> {
        val entries = material.getEntries(p.id)
        val attendance = labour.getAttendance(p.id)
        val labourPay = labour.getPayments(p.id)
        val contractorList = contractors.getContractors(p.id)
        val contractorPay = contractors.getPayments(p.id)
        val expenseList = expenses.getExpenses(p.id)
        val supplierPay = suppliers.getPaymentsForProject(p.id)
        val stageList = stages.getStages(p.id)

        val materialCost = entries.filter { it.entryType == MaterialEntryType.RECEIVED && it.date in r }.sumOf { it.total }
        val labourEarned = attendance.filter { it.date in r }.sumOf { it.earned }
        val labourPaid = labourPay.filter { it.date in r }.sumOf { it.amount }
        val labourCost = maxOf(labourEarned, labourPaid)
        val contractorPaid = contractorPay.filter { it.date in r }.sumOf { it.amount }
        val other = expenseList.filter { it.date in r }.sumOf { it.amount }
        val spent = materialCost + labourCost + contractorPaid + other

        // Dues are always "as of today" (all dates).
        val supplierDue = (entries.filter { it.entryType == MaterialEntryType.RECEIVED && it.supplierId != null }.sumOf { it.total } -
            supplierPay.sumOf { it.amount }).coerceAtLeast(0)
        val labourDue = attendance.groupBy { it.labourId }.map { (id, list) ->
            (list.sumOf { it.earned } - labourPay.filter { it.labourId == id }.sumOf { it.amount }).coerceAtLeast(0)
        }.sum()
        val contractorDue = contractorList.sumOf { c ->
            (c.workDoneValue - contractorPay.filter { it.contractorId == c.id }.sumOf { it.amount }).coerceAtLeast(0)
        }
        val done = stageList.count { it.status == com.diary.construction.domain.model.StageStatus.COMPLETED }
        val running = stageList.count { it.status == com.diary.construction.domain.model.StageStatus.IN_PROGRESS }
        val progress = if (stageList.isEmpty()) 0 else (done * 2 + running) * 50 / stageList.size

        val budgetLabel = if (role == UserRole.THEKEDAR) s(R.string.budget_thekedar) else s(R.string.budget_owner)
        val clientLabel = if (role == UserRole.THEKEDAR) s(R.string.project_client_name) else s(R.string.project_owner_name)
        val info = ReportSection(
            title = s(R.string.r_project_info),
            facts = listOfNotNull(
                s(R.string.project_name) to p.name,
                p.siteAddress.takeIf { it.isNotBlank() }?.let { s(R.string.project_address) to it },
                p.clientName.takeIf { it.isNotBlank() }?.let { clientLabel to (it + if (p.clientPhone.isNotBlank()) " (${p.clientPhone})" else "") },
                s(R.string.project_start_date) to Dates.format(p.startDate),
                p.area.takeIf { it > 0 }?.let { s(R.string.project_area) to "${Quantity.format(it)} ${s(p.areaUnit.labelRes)}" },
                s(R.string.project_status) to s(p.status.labelRes),
                s(R.string.work_progress) to "$progress%",
            ),
        )
        val pct = { amount: Long -> if (spent > 0) "${amount * 100 / spent}%" else "0%" }
        val money = ReportSection(
            title = s(R.string.dashboard_by_category),
            columns = listOf(s(R.string.category), s(R.string.amount), "%"),
            rows = listOf(
                listOf(s(R.string.cat_material), m(materialCost), pct(materialCost)),
                listOf(s(R.string.cat_labour), m(labourCost), pct(labourCost)),
                listOf(contractorWord(), m(contractorPaid), pct(contractorPaid)),
                listOf(s(R.string.cat_other), m(other), pct(other)),
            ),
            totalRow = listOf(s(R.string.total), m(spent), if (spent > 0) "100%" else "0%"),
            numericColumns = setOf(1, 2),
        )
        val budget = ReportSection(
            title = s(R.string.r_budget),
            facts = listOf(
                budgetLabel to m(p.budget),
                s(R.string.spent) to m(spent),
                (if (role == UserRole.THEKEDAR) s(R.string.expected_profit) else s(R.string.remaining)) to m(p.budget - spent),
            ),
        )
        val dues = ReportSection(
            title = s(R.string.r_dues_today),
            facts = listOf(
                s(R.string.dues_suppliers) to m(supplierDue),
                s(R.string.dues_labour) to m(labourDue),
                contractorWord() to m(contractorDue),
                s(R.string.total) to m(supplierDue + labourDue + contractorDue),
            ),
        )
        return listOf(info, budget, money, dues)
    }

    // ---------------------------------------------------------------- stages

    private suspend fun stageSections(p: Project, r: DateRange): List<ReportSection> {
        val stageList = stages.getStages(p.id)
        val entries = material.getEntries(p.id).filter { it.entryType == MaterialEntryType.RECEIVED && it.date in r }
        val exp = expenses.getExpenses(p.id).filter { it.date in r }
        val rows = stageList.map { st ->
            val spent = entries.filter { it.stageId == st.id }.sumOf { it.total } + exp.filter { it.stageId == st.id }.sumOf { it.amount }
            Triple(st, spent, st.budget)
        }
        val unassigned = entries.filter { it.stageId == null }.sumOf { it.total } + exp.filter { it.stageId == null }.sumOf { it.amount }
        return listOf(
            ReportSection(
                title = s(R.string.section_stages),
                columns = listOf(s(R.string.stage_type), s(R.string.project_status), s(R.string.stage_budget), s(R.string.spent), s(R.string.stage_start), s(R.string.stage_end)),
                rows = rows.map { (st, spent, budget) ->
                    listOf(st.label(), s(st.status.labelRes), m(budget), m(spent), st.startDate?.let(Dates::format).orEmpty(), st.endDate?.let(Dates::format).orEmpty())
                } + listOf(listOf(s(R.string.r_no_stage), "", "", m(unassigned), "", "")),
                totalRow = listOf(s(R.string.total), "", m(rows.sumOf { it.third }), m(rows.sumOf { it.second } + unassigned), "", ""),
                numericColumns = setOf(2, 3),
            ),
            ReportSection(title = "", facts = listOf(s(R.string.r_note) to s(R.string.r_stage_note))),
        )
    }

    // ---------------------------------------------------------------- material

    private suspend fun materialSections(p: Project, r: DateRange): List<ReportSection> {
        val types = material.getTypes().associateBy { it.id }
        val supplierNames = suppliers.getSuppliers().associate { it.id to it.name }
        val entries = material.getEntries(p.id).filter { it.date in r }
        val byType = entries.groupBy { it.materialTypeId }
        val stock = ReportSection(
            title = s(R.string.r_material_stock),
            columns = listOf(s(R.string.material), s(R.string.stock_received), s(R.string.stock_used), s(R.string.stock_remaining), s(R.string.stock_cost)),
            rows = byType.mapNotNull { (typeId, list) ->
                val t = types[typeId] ?: return@mapNotNull null
                val unit = s(t.unit.labelRes)
                val rec = list.filter { it.entryType == MaterialEntryType.RECEIVED }.sumOf { it.quantity }
                val used = list.filter { it.entryType == MaterialEntryType.USED }.sumOf { it.quantity }
                listOf(
                    t.label(),
                    "${Quantity.format(rec)} $unit",
                    "${Quantity.format(used)} $unit",
                    "${Quantity.format(rec - used)} $unit",
                    m(list.filter { it.entryType == MaterialEntryType.RECEIVED }.sumOf { it.total }),
                )
            },
            totalRow = listOf(s(R.string.total), "", "", "", m(entries.filter { it.entryType == MaterialEntryType.RECEIVED }.sumOf { it.total })),
            numericColumns = setOf(1, 2, 3, 4),
        )
        val list = ReportSection(
            title = s(R.string.r_material_entries),
            columns = listOf(s(R.string.date), s(R.string.material), "", s(R.string.quantity), s(R.string.r_rate), s(R.string.total), s(R.string.supplier)),
            rows = entries.sortedBy { it.date }.map { e ->
                val t = types[e.materialTypeId]
                listOf(
                    Dates.format(e.date),
                    t?.label().orEmpty(),
                    s(e.entryType.labelRes),
                    "${Quantity.format(e.quantity)} ${t?.let { s(it.unit.labelRes) }.orEmpty()}",
                    if (e.entryType == MaterialEntryType.RECEIVED) m(e.rate) else "",
                    if (e.entryType == MaterialEntryType.RECEIVED) m(e.total) else "",
                    e.supplierId?.let { supplierNames[it] }.orEmpty(),
                )
            },
            numericColumns = setOf(3, 4, 5),
        )
        return listOf(stock, list)
    }

    // ---------------------------------------------------------------- labour

    private suspend fun labourSections(p: Project, r: DateRange): List<ReportSection> {
        val people = labour.getLabours(p.id)
        val attendance = labour.getAttendance(p.id).filter { it.date in r }
        val payments = labour.getPayments(p.id).filter { it.date in r }
        val names = people.associate { it.id to it.name }
        val rows = people.map { l ->
            val a = attendance.filter { it.labourId == l.id }
            val days = a.sumOf { it.status.wagePercent } / 100.0
            val earned = a.sumOf { it.earned }
            val paid = payments.filter { it.labourId == l.id }.sumOf { it.amount }
            listOf(l.name, s(l.role.labelRes), m(l.dailyWage), formatDays(days), m(earned), m(paid), m(earned - paid))
        }
        val totalEarned = attendance.sumOf { it.earned }
        val totalPaid = payments.sumOf { it.amount }
        return listOf(
            ReportSection(
                title = s(R.string.section_labour),
                columns = listOf(s(R.string.labour_name), s(R.string.labour_role), s(R.string.r_wage), s(R.string.r_days), s(R.string.earned), s(R.string.paid), s(R.string.r_balance)),
                rows = rows,
                totalRow = listOf(s(R.string.total), "", "", formatDays(attendance.sumOf { it.status.wagePercent } / 100.0), m(totalEarned), m(totalPaid), m(totalEarned - totalPaid)),
                numericColumns = setOf(2, 3, 4, 5, 6),
            ),
            ReportSection(
                title = s(R.string.payment_history),
                columns = listOf(s(R.string.date), s(R.string.labour_name), s(R.string.r_type), s(R.string.notes), s(R.string.amount)),
                rows = payments.sortedBy { it.date }.map { listOf(Dates.format(it.date), names[it.labourId].orEmpty(), s(it.type.labelRes), it.note, m(it.amount)) },
                totalRow = listOf(s(R.string.total), "", "", "", m(totalPaid)),
                numericColumns = setOf(4),
            ),
        )
    }

    // ---------------------------------------------------------------- suppliers

    private suspend fun supplierSections(p: Project, r: DateRange): List<ReportSection> {
        val types = material.getTypes().associateBy { it.id }
        val entries = material.getEntries(p.id).filter { it.entryType == MaterialEntryType.RECEIVED && it.supplierId != null }
        val payments = suppliers.getPaymentsForProject(p.id)
        val ids = (entries.mapNotNull { it.supplierId } + payments.map { it.supplierId }).toSet()
        val list = suppliers.getSuppliers().filter { it.id in ids }
        if (list.isEmpty()) return listOf(ReportSection(title = s(R.string.suppliers_khata), facts = listOf(s(R.string.r_nothing) to "")))

        return list.map { sup ->
            data class Line(val date: Long, val detail: String, val debit: Long, val credit: Long)
            val lines = (
                entries.filter { it.supplierId == sup.id }.map { e ->
                    val t = types[e.materialTypeId]
                    Line(e.date, "${t?.label().orEmpty()} ${Quantity.format(e.quantity)} ${t?.let { s(it.unit.labelRes) }.orEmpty()}", e.total, 0)
                } + payments.filter { it.supplierId == sup.id }.map { Line(it.date, s(R.string.payment_given) + if (it.note.isNotBlank()) " – ${it.note}" else "", 0, it.amount) }
                ).sortedBy { it.date }
            // Opening balance = everything before the range.
            val opening = lines.filter { r.from != null && it.date < r.from }.sumOf { it.debit - it.credit }
            var running = opening
            val shown = lines.filter { it.date in r }
            val rows = mutableListOf<List<String>>()
            if (r.from != null) rows += listOf(Dates.format(r.from), s(R.string.r_opening), "", "", m(opening))
            shown.forEach {
                running += it.debit - it.credit
                rows += listOf(Dates.format(it.date), it.detail, if (it.debit > 0) m(it.debit) else "", if (it.credit > 0) m(it.credit) else "", m(running))
            }
            ReportSection(
                title = sup.name + if (sup.phone.isNotBlank()) " (${sup.phone})" else "",
                columns = listOf(s(R.string.date), s(R.string.r_detail), s(R.string.r_debit), s(R.string.r_credit), s(R.string.r_balance)),
                rows = rows,
                totalRow = listOf(s(R.string.total), "", m(shown.sumOf { it.debit }), m(shown.sumOf { it.credit }), m(running)),
                numericColumns = setOf(2, 3, 4),
            )
        }
    }

    // ---------------------------------------------------------------- contractors

    private suspend fun contractorSections(p: Project, r: DateRange): List<ReportSection> {
        val list = contractors.getContractors(p.id)
        if (list.isEmpty()) return listOf(ReportSection(title = contractorWord(), facts = listOf(s(R.string.r_nothing) to "")))
        val payments = contractors.getPayments(p.id)
        return list.map { c ->
            val all = payments.filter { it.contractorId == c.id }
            val paidTotal = all.sumOf { it.amount }
            val shown = all.filter { it.date in r }.sortedBy { it.date }
            ReportSection(
                title = c.name + if (c.work.isNotBlank()) " – ${c.work}" else "",
                facts = listOf(
                    s(R.string.agreement) to s(c.agreementType.labelRes),
                    s(R.string.agreed_amount) to m(c.agreedAmount),
                    s(R.string.work_done) to "${c.workDonePercent}% (${m(c.workDoneValue)})",
                    s(R.string.r_paid_all_time) to m(paidTotal),
                    s(R.string.due_now) to m((c.workDoneValue - paidTotal).coerceAtLeast(0)),
                    s(R.string.remaining_on_contract) to m(c.agreedAmount - paidTotal),
                ),
                columns = listOf(s(R.string.date), s(R.string.notes), s(R.string.amount)),
                rows = shown.map { listOf(Dates.format(it.date), it.note, m(it.amount)) },
                totalRow = listOf(s(R.string.total), "", m(shown.sumOf { it.amount })),
                numericColumns = setOf(2),
            )
        }
    }

    // ---------------------------------------------------------------- expenses

    private suspend fun expenseSections(p: Project, r: DateRange): List<ReportSection> {
        val stageNames = stages.getStages(p.id).associate { it.id to it.label() }
        val list = expenses.getExpenses(p.id).filter { it.date in r }.sortedBy { it.date }
        val total = list.sumOf { it.amount }
        return listOf(
            ReportSection(
                title = s(R.string.dashboard_by_category),
                columns = listOf(s(R.string.category), s(R.string.amount)),
                rows = list.groupBy { it.category }.map { (c, l) -> listOf(s(c.labelRes), m(l.sumOf { it.amount })) },
                totalRow = listOf(s(R.string.total), m(total)),
                numericColumns = setOf(1),
            ),
            ReportSection(
                title = s(R.string.section_expenses),
                columns = listOf(s(R.string.date), s(R.string.category), s(R.string.stage_type), s(R.string.notes), s(R.string.amount)),
                rows = list.map { e -> listOf(Dates.format(e.date), s(e.category.labelRes), e.stageId?.let { stageNames[it] }.orEmpty(), e.note, m(e.amount)) },
                totalRow = listOf(s(R.string.total), "", "", "", m(total)),
                numericColumns = setOf(4),
            ),
        )
    }

    private fun formatDays(days: Double): String =
        if (days % 1.0 == 0.0) days.toInt().toString() else String.format(java.util.Locale.US, "%.1f", days)

    companion object {
        fun ReportType.titleRes(): Int = when (this) {
            ReportType.SUMMARY -> R.string.report_summary
            ReportType.STAGES -> R.string.report_stages
            ReportType.MATERIAL -> R.string.report_material
            ReportType.LABOUR -> R.string.report_labour
            ReportType.SUPPLIERS -> R.string.report_suppliers
            ReportType.CONTRACTORS -> R.string.report_contractors
            ReportType.EXPENSES -> R.string.report_expenses
        }
    }
}
