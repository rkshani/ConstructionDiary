package com.diary.construction.domain.model

/** Small query results and computed summaries used by the screens. */

/** A sum for one id (supplier, labour, stage ...). */
data class IdAmount(val id: Long, val amount: Long)

/** Stock of one material in one project. Quantities scale 100. */
data class MaterialStock(
    val materialTypeId: Long,
    val received: Long,
    val used: Long,
    val cost: Long,
) {
    val remaining: Long get() = received - used
}

/** One material entry with names joined in. */
data class MaterialEntryItem(
    val entry: MaterialEntry,
    val type: MaterialType,
    val supplierName: String?,
    val stage: Stage?,
)

data class SupplierBalance(
    val supplier: Supplier,
    /** Value of material taken (paisa). */
    val taken: Long,
    val paid: Long,
) {
    val balance: Long get() = taken - paid
}

/** One line in a khata (ledger): debit = what we owe more, credit = what we paid. */
data class LedgerLine(
    val date: Long,
    val title: String,
    val detail: String,
    val debit: Long,
    val credit: Long,
    /** Set for payments that can be deleted from the khata screen. */
    val paymentId: Long? = null,
    val entryId: Long? = null,
)

data class LabourSummary(
    val labour: Labour,
    val daysPresent: Int,
    val daysHalf: Int,
    val earned: Long,
    val paid: Long,
) {
    val balance: Long get() = earned - paid
}

data class ContractorSummary(
    val contractor: Contractor,
    val paid: Long,
) {
    val agreed: Long get() = contractor.agreedAmount
    /** Still to pay on the whole agreement. */
    val remaining: Long get() = agreed - paid
    /** Due right now = value of work done - paid (never negative). */
    val dueNow: Long get() = (contractor.workDoneValue - paid).coerceAtLeast(0)
}

data class StageSummary(
    val stage: Stage,
    val spent: Long,
)

/** Everything the project dashboard shows. */
data class ProjectSummary(
    val project: Project,
    val material: Long,
    val labour: Long,
    val contractor: Long,
    val other: Long,
    val supplierDue: Long,
    val labourDue: Long,
    val contractorDue: Long,
    val stagesTotal: Int,
    val stagesDone: Int,
    val stagesInProgress: Int,
) {
    val spent: Long get() = material + labour + contractor + other
    val remaining: Long get() = project.budget - spent
    val pendingDues: Long get() = supplierDue + labourDue + contractorDue

    /** 0..100. Completed stages count fully, running stages count half. */
    val progressPercent: Int
        get() = if (stagesTotal == 0) 0 else ((stagesDone * 2 + stagesInProgress) * 50 / stagesTotal).coerceIn(0, 100)

    /** 0..100+ share of the budget used. */
    val budgetUsedPercent: Int
        get() = if (project.budget <= 0) 0 else (spent * 100 / project.budget).toInt()

    val budgetLevel: BudgetLevel
        get() = when {
            project.budget <= 0 -> BudgetLevel.NONE
            spent > project.budget -> BudgetLevel.OVER
            budgetUsedPercent >= 90 -> BudgetLevel.WARNING
            else -> BudgetLevel.OK
        }

    fun amountFor(category: SpendCategory): Long = when (category) {
        SpendCategory.MATERIAL -> material
        SpendCategory.LABOUR -> labour
        SpendCategory.CONTRACTOR -> contractor
        SpendCategory.OTHER -> other
    }
}

enum class BudgetLevel { NONE, OK, WARNING, OVER }

/** A row in the project list. */
data class ProjectListItem(
    val project: Project,
    val spent: Long,
)
