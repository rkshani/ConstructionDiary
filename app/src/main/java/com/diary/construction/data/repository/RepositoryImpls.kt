package com.diary.construction.data.repository

import com.diary.construction.data.local.dao.ContractorDao
import com.diary.construction.data.local.dao.ExpenseDao
import com.diary.construction.data.local.dao.LabourDao
import com.diary.construction.data.local.dao.MaterialDao
import com.diary.construction.data.local.dao.PhotoDao
import com.diary.construction.data.local.dao.ProjectDao
import com.diary.construction.data.local.dao.StageDao
import com.diary.construction.data.local.dao.SupplierDao
import com.diary.construction.domain.model.Attendance
import com.diary.construction.domain.model.Contractor
import com.diary.construction.domain.model.ContractorPayment
import com.diary.construction.domain.model.Expense
import com.diary.construction.domain.model.Labour
import com.diary.construction.domain.model.LabourPayment
import com.diary.construction.domain.model.MaterialEntry
import com.diary.construction.domain.model.MaterialType
import com.diary.construction.domain.model.Photo
import com.diary.construction.domain.model.Project
import com.diary.construction.domain.model.Stage
import com.diary.construction.domain.model.StageType
import com.diary.construction.domain.model.Supplier
import com.diary.construction.domain.model.SupplierPayment
import com.diary.construction.domain.repository.ContractorRepository
import com.diary.construction.domain.repository.ExpenseRepository
import com.diary.construction.domain.repository.LabourDayCount
import com.diary.construction.domain.repository.LabourRepository
import com.diary.construction.domain.repository.MaterialRepository
import com.diary.construction.domain.repository.PhotoRepository
import com.diary.construction.domain.repository.ProjectRepository
import com.diary.construction.domain.repository.StageRepository
import com.diary.construction.domain.repository.SupplierRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectRepositoryImpl @Inject constructor(private val dao: ProjectDao) : ProjectRepository {
    override fun observeProjects() = dao.observeAll()
    override fun observeProject(id: Long) = dao.observe(id)
    override fun observeSpentPerProject() = dao.observeSpentPerProject()
    override suspend fun getProject(id: Long) = dao.get(id)
    override suspend fun getProjects() = dao.getAll()
    override suspend fun save(project: Project): Long =
        if (project.id == 0L) dao.insert(project) else { dao.update(project); project.id }
    override suspend fun delete(project: Project) = dao.delete(project)
}

@Singleton
class StageRepositoryImpl @Inject constructor(private val dao: StageDao) : StageRepository {
    override fun observeStages(projectId: Long) = dao.observeForProject(projectId)
    override suspend fun getStages(projectId: Long) = dao.getForProject(projectId)
    override suspend fun getStage(id: Long) = dao.get(id)
    override suspend fun getOpenStagesEndingBefore(until: Long) = dao.getOpenEndingBefore(until)
    override suspend fun save(stage: Stage): Long = if (stage.id == 0L) {
        dao.insert(stage.copy(sortOrder = dao.maxSortOrder(stage.projectId) + 1))
    } else {
        dao.update(stage); stage.id
    }
    override suspend fun reorder(stages: List<Stage>) =
        dao.updateAll(stages.mapIndexed { index, s -> s.copy(sortOrder = index) })
    override suspend fun delete(stage: Stage) = dao.delete(stage)
    override suspend fun addDefaultStages(projectId: Long) {
        val start = dao.maxSortOrder(projectId) + 1
        val defaults = StageType.entries.filter { it != StageType.CUSTOM }
        dao.insertAll(defaults.mapIndexed { i, type -> Stage(projectId = projectId, type = type, sortOrder = start + i) })
    }
}

@Singleton
class MaterialRepositoryImpl @Inject constructor(private val dao: MaterialDao) : MaterialRepository {
    override fun observeTypes() = dao.observeTypes()
    override suspend fun getTypes() = dao.getTypes()
    override suspend fun saveType(type: MaterialType): Long =
        if (type.id == 0L) dao.insertType(type) else { dao.updateType(type); type.id }
    override suspend fun deleteType(type: MaterialType) = dao.deleteType(type)

    override fun observeEntries(projectId: Long) = dao.observeEntries(projectId)
    override fun observeEntriesForSupplier(supplierId: Long) = dao.observeEntriesForSupplier(supplierId)
    override suspend fun getEntries(projectId: Long) = dao.getEntries(projectId)
    override suspend fun getEntriesForSupplier(supplierId: Long) = dao.getEntriesForSupplier(supplierId)
    override suspend fun getEntry(id: Long) = dao.getEntry(id)
    override suspend fun saveEntry(entry: MaterialEntry): Long =
        if (entry.id == 0L) dao.insertEntry(entry) else { dao.updateEntry(entry); entry.id }
    override suspend fun deleteEntry(entry: MaterialEntry) = dao.deleteEntry(entry)

    override fun observeStock(projectId: Long) = dao.observeStock(projectId)
    override fun observeCost(projectId: Long) = dao.observeCost(projectId)
    override fun observeCostPerStage(projectId: Long) = dao.observeCostPerStage(projectId)
    override fun observeTakenPerSupplier() = dao.observeTakenPerSupplier()
    override fun observeSupplierTakenForProject(projectId: Long) = dao.observeSupplierTakenForProject(projectId)
    override suspend fun billPhotos(projectId: Long) = dao.billPhotos(projectId)
}

@Singleton
class SupplierRepositoryImpl @Inject constructor(private val dao: SupplierDao) : SupplierRepository {
    override fun observeSuppliers() = dao.observeAll()
    override fun observeSupplier(id: Long) = dao.observe(id)
    override suspend fun getSuppliers() = dao.getAll()
    override suspend fun getSupplier(id: Long) = dao.get(id)
    override suspend fun save(supplier: Supplier): Long =
        if (supplier.id == 0L) dao.insert(supplier) else { dao.update(supplier); supplier.id }
    override suspend fun delete(supplier: Supplier) = dao.delete(supplier)

    override fun observePayments(supplierId: Long) = dao.observePayments(supplierId)
    override suspend fun getPayments(supplierId: Long) = dao.getPayments(supplierId)
    override suspend fun getPaymentsForProject(projectId: Long) = dao.getPaymentsForProject(projectId)
    override suspend fun savePayment(payment: SupplierPayment): Long =
        if (payment.id == 0L) dao.insertPayment(payment) else { dao.updatePayment(payment); payment.id }
    override suspend fun deletePayment(id: Long) = dao.deletePayment(id)
    override fun observePaidPerSupplier() = dao.observePaidPerSupplier()
    override fun observePaidForProject(projectId: Long) = dao.observePaidForProject(projectId)
}

@Singleton
class LabourRepositoryImpl @Inject constructor(private val dao: LabourDao) : LabourRepository {
    override fun observeLabours(projectId: Long) = dao.observeForProject(projectId)
    override fun observeLabour(id: Long) = dao.observe(id)
    override suspend fun getLabours(projectId: Long) = dao.getForProject(projectId)
    override suspend fun getLabour(id: Long) = dao.get(id)
    override suspend fun save(labour: Labour): Long =
        if (labour.id == 0L) dao.insert(labour) else { dao.update(labour); labour.id }
    override suspend fun delete(labour: Labour) = dao.delete(labour)

    override suspend fun setAttendance(attendance: Attendance) = dao.upsertAttendance(attendance.copy(id = 0))
    override suspend fun clearAttendance(labourId: Long, date: Long) = dao.deleteAttendance(labourId, date)
    override fun observeAttendanceForDay(projectId: Long, date: Long) = dao.observeAttendanceForDay(projectId, date)
    override fun observeAttendanceInRange(projectId: Long, from: Long, to: Long) =
        dao.observeAttendanceInRange(projectId, from, to)
    override fun observeAttendanceForLabour(labourId: Long) = dao.observeAttendanceForLabour(labourId)
    override suspend fun getAttendance(projectId: Long) = dao.getAttendanceForProject(projectId)
    override fun observeEarnedPerLabour(projectId: Long) = dao.observeEarnedPerLabour(projectId)
    override fun observeDaysPerLabour(projectId: Long): Flow<List<LabourDayCount>> =
        dao.observeDaysPerLabour(projectId).map { list -> list.map { LabourDayCount(it.labourId, it.present, it.half) } }

    override fun observePayments(labourId: Long) = dao.observePayments(labourId)
    override fun observePaymentsInRange(projectId: Long, from: Long, to: Long) =
        dao.observePaymentsInRange(projectId, from, to)
    override suspend fun getPayments(projectId: Long) = dao.getPaymentsForProject(projectId)
    override suspend fun addPayment(payment: LabourPayment) = dao.insertPayment(payment)
    override suspend fun deletePayment(id: Long) = dao.deletePayment(id)
    override fun observePaidPerLabour(projectId: Long) = dao.observePaidPerLabour(projectId)
}

@Singleton
class ContractorRepositoryImpl @Inject constructor(private val dao: ContractorDao) : ContractorRepository {
    override fun observeContractors(projectId: Long) = dao.observeForProject(projectId)
    override fun observeContractor(id: Long) = dao.observe(id)
    override suspend fun getContractors(projectId: Long) = dao.getForProject(projectId)
    override suspend fun getContractor(id: Long) = dao.get(id)
    override suspend fun save(contractor: Contractor): Long =
        if (contractor.id == 0L) dao.insert(contractor) else { dao.update(contractor); contractor.id }
    override suspend fun delete(contractor: Contractor) = dao.delete(contractor)

    override fun observePayments(contractorId: Long) = dao.observePayments(contractorId)
    override suspend fun getPayments(projectId: Long) = dao.getPaymentsForProject(projectId)
    override suspend fun addPayment(payment: ContractorPayment) = dao.insertPayment(payment)
    override suspend fun deletePayment(id: Long) = dao.deletePayment(id)
    override fun observePaidPerContractor(projectId: Long) = dao.observePaidPerContractor(projectId)
}

@Singleton
class ExpenseRepositoryImpl @Inject constructor(private val dao: ExpenseDao) : ExpenseRepository {
    override fun observeExpenses(projectId: Long) = dao.observeForProject(projectId)
    override suspend fun getExpenses(projectId: Long) = dao.getForProject(projectId)
    override suspend fun getExpense(id: Long) = dao.get(id)
    override suspend fun save(expense: Expense): Long =
        if (expense.id == 0L) dao.insert(expense) else { dao.update(expense); expense.id }
    override suspend fun delete(expense: Expense) = dao.delete(expense)
    override fun observeTotal(projectId: Long) = dao.observeTotal(projectId)
    override fun observePerStage(projectId: Long) = dao.observePerStage(projectId)
    override suspend fun billPhotos(projectId: Long) = dao.billPhotos(projectId)
}

@Singleton
class PhotoRepositoryImpl @Inject constructor(private val dao: PhotoDao) : PhotoRepository {
    override fun observePhotos(projectId: Long) = dao.observeForProject(projectId)
    override suspend fun getPhoto(id: Long) = dao.get(id)
    override suspend fun photoPaths(projectId: Long) = dao.pathsForProject(projectId)
    override suspend fun save(photo: Photo): Long =
        if (photo.id == 0L) dao.insert(photo) else { dao.update(photo); photo.id }
    override suspend fun delete(photo: Photo) = dao.delete(photo)
}
