package com.diary.construction.domain.usecase

import com.diary.construction.domain.model.ContractorSummary
import com.diary.construction.domain.model.LabourSummary
import com.diary.construction.domain.model.ProjectListItem
import com.diary.construction.domain.model.ProjectSummary
import com.diary.construction.domain.model.StageStatus
import com.diary.construction.domain.model.StageSummary
import com.diary.construction.domain.model.SupplierBalance
import com.diary.construction.domain.repository.ContractorRepository
import com.diary.construction.domain.repository.ExpenseRepository
import com.diary.construction.domain.repository.LabourRepository
import com.diary.construction.domain.repository.MaterialRepository
import com.diary.construction.domain.repository.ProjectRepository
import com.diary.construction.domain.repository.StageRepository
import com.diary.construction.domain.repository.SupplierRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Project list rows with the money spent so far. */
class ObserveProjectListUseCase @Inject constructor(
    private val projects: ProjectRepository,
) {
    operator fun invoke(): Flow<List<ProjectListItem>> =
        combine(projects.observeProjects(), projects.observeSpentPerProject()) { list, spent ->
            val byId = spent.associate { it.id to it.amount }
            list.map { ProjectListItem(it, byId[it.id] ?: 0) }
        }
}

/** Labour per project with days, earned wages, paid and balance. */
class ObserveLabourSummariesUseCase @Inject constructor(
    private val labour: LabourRepository,
) {
    operator fun invoke(projectId: Long): Flow<List<LabourSummary>> = combine(
        labour.observeLabours(projectId),
        labour.observeEarnedPerLabour(projectId),
        labour.observePaidPerLabour(projectId),
        labour.observeDaysPerLabour(projectId),
    ) { list, earned, paid, days ->
        val e = earned.associate { it.id to it.amount }
        val p = paid.associate { it.id to it.amount }
        val d = days.associateBy { it.labourId }
        list.map {
            LabourSummary(
                labour = it,
                daysPresent = d[it.id]?.present ?: 0,
                daysHalf = d[it.id]?.half ?: 0,
                earned = e[it.id] ?: 0,
                paid = p[it.id] ?: 0,
            )
        }
    }
}

class ObserveContractorSummariesUseCase @Inject constructor(
    private val contractors: ContractorRepository,
) {
    operator fun invoke(projectId: Long): Flow<List<ContractorSummary>> = combine(
        contractors.observeContractors(projectId),
        contractors.observePaidPerContractor(projectId),
    ) { list, paid ->
        val p = paid.associate { it.id to it.amount }
        list.map { ContractorSummary(it, p[it.id] ?: 0) }
    }
}

/** All suppliers with material taken (all projects), paid and balance. */
class ObserveSupplierBalancesUseCase @Inject constructor(
    private val suppliers: SupplierRepository,
    private val material: MaterialRepository,
) {
    operator fun invoke(): Flow<List<SupplierBalance>> = combine(
        suppliers.observeSuppliers(),
        material.observeTakenPerSupplier(),
        suppliers.observePaidPerSupplier(),
    ) { list, taken, paid ->
        val t = taken.associate { it.id to it.amount }
        val p = paid.associate { it.id to it.amount }
        list.map { SupplierBalance(it, t[it.id] ?: 0, p[it.id] ?: 0) }
    }
}

/** Stages with money spent on each (material + other expenses linked to the stage). */
class ObserveStageSummariesUseCase @Inject constructor(
    private val stages: StageRepository,
    private val material: MaterialRepository,
    private val expenses: ExpenseRepository,
) {
    operator fun invoke(projectId: Long): Flow<List<StageSummary>> = combine(
        stages.observeStages(projectId),
        material.observeCostPerStage(projectId),
        expenses.observePerStage(projectId),
    ) { list, mat, exp ->
        val m = mat.associate { it.id to it.amount }
        val e = exp.associate { it.id to it.amount }
        list.map { StageSummary(it, (m[it.id] ?: 0) + (e[it.id] ?: 0)) }
    }
}

/**
 * Dashboard numbers for one project.
 * Spent = material bought + labour wages earned (or paid, whichever is more)
 *       + contractor payments + other expenses.
 * Pending dues = supplier balance + unpaid labour + contractor work done but not paid.
 */
class ObserveProjectSummaryUseCase @Inject constructor(
    private val projects: ProjectRepository,
    private val stages: StageRepository,
    private val material: MaterialRepository,
    private val suppliers: SupplierRepository,
    private val expenses: ExpenseRepository,
    private val labourSummaries: ObserveLabourSummariesUseCase,
    private val contractorSummaries: ObserveContractorSummariesUseCase,
) {
    private data class Part(val cost: Long, val due: Long)

    operator fun invoke(projectId: Long): Flow<ProjectSummary> {
        val labourPart = labourSummaries(projectId).map { list ->
            Part(
                cost = list.sumOf { maxOf(it.earned, it.paid) },
                due = list.sumOf { it.balance.coerceAtLeast(0) },
            )
        }
        val contractorPart = contractorSummaries(projectId).map { list ->
            Part(cost = list.sumOf { it.paid }, due = list.sumOf { it.dueNow })
        }
        val supplierDue = combine(
            material.observeSupplierTakenForProject(projectId),
            suppliers.observePaidForProject(projectId),
        ) { taken, paid -> (taken - paid).coerceAtLeast(0) }

        val base = combine(
            projects.observeProject(projectId).filterNotNull(),
            material.observeCost(projectId),
            expenses.observeTotal(projectId),
            stages.observeStages(projectId),
        ) { project, materialCost, other, stageList -> Base(project, materialCost, other, stageList) }

        return combine(base, labourPart, contractorPart, supplierDue) { b, l, c, s ->
            ProjectSummary(
                project = b.project,
                material = b.material,
                labour = l.cost,
                contractor = c.cost,
                other = b.other,
                supplierDue = s,
                labourDue = l.due,
                contractorDue = c.due,
                stagesTotal = b.stages.size,
                stagesDone = b.stages.count { it.status == StageStatus.COMPLETED },
                stagesInProgress = b.stages.count { it.status == StageStatus.IN_PROGRESS },
            )
        }
    }

    private data class Base(
        val project: com.diary.construction.domain.model.Project,
        val material: Long,
        val other: Long,
        val stages: List<com.diary.construction.domain.model.Stage>,
    )
}
