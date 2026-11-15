package com.diary.construction.domain.repository

import com.diary.construction.domain.model.Attendance
import com.diary.construction.domain.model.Contractor
import com.diary.construction.domain.model.ContractorPayment
import com.diary.construction.domain.model.Expense
import com.diary.construction.domain.model.IdAmount
import com.diary.construction.domain.model.Labour
import com.diary.construction.domain.model.LabourPayment
import com.diary.construction.domain.model.MaterialEntry
import com.diary.construction.domain.model.MaterialStock
import com.diary.construction.domain.model.MaterialType
import com.diary.construction.domain.model.Photo
import com.diary.construction.domain.model.Project
import com.diary.construction.domain.model.Stage
import com.diary.construction.domain.model.Supplier
import com.diary.construction.domain.model.SupplierPayment
import kotlinx.coroutines.flow.Flow

/** "save" inserts when id == 0, else updates. It returns the row id. */

interface ProjectRepository {
    fun observeProjects(): Flow<List<Project>>
    fun observeProject(id: Long): Flow<Project?>
    fun observeSpentPerProject(): Flow<List<IdAmount>>
    suspend fun getProject(id: Long): Project?
    suspend fun getProjects(): List<Project>
    suspend fun save(project: Project): Long
    suspend fun delete(project: Project)
}

interface StageRepository {
    fun observeStages(projectId: Long): Flow<List<Stage>>
    suspend fun getStages(projectId: Long): List<Stage>
    suspend fun getStage(id: Long): Stage?
    suspend fun getOpenStagesEndingBefore(until: Long): List<Stage>
    suspend fun save(stage: Stage): Long
    suspend fun reorder(stages: List<Stage>)
    suspend fun delete(stage: Stage)
    /** Adds the 9 standard stages (foundation ... finishing). */
    suspend fun addDefaultStages(projectId: Long)
}

interface MaterialRepository {
    fun observeTypes(): Flow<List<MaterialType>>
    suspend fun getTypes(): List<MaterialType>
    suspend fun saveType(type: MaterialType): Long
    suspend fun deleteType(type: MaterialType)

    fun observeEntries(projectId: Long): Flow<List<MaterialEntry>>
    fun observeEntriesForSupplier(supplierId: Long): Flow<List<MaterialEntry>>
    suspend fun getEntries(projectId: Long): List<MaterialEntry>
    suspend fun getEntriesForSupplier(supplierId: Long): List<MaterialEntry>
    suspend fun getEntry(id: Long): MaterialEntry?
    suspend fun saveEntry(entry: MaterialEntry): Long
    suspend fun deleteEntry(entry: MaterialEntry)

    fun observeStock(projectId: Long): Flow<List<MaterialStock>>
    fun observeCost(projectId: Long): Flow<Long>
    fun observeCostPerStage(projectId: Long): Flow<List<IdAmount>>
    fun observeTakenPerSupplier(): Flow<List<IdAmount>>
    fun observeSupplierTakenForProject(projectId: Long): Flow<Long>
    suspend fun billPhotos(projectId: Long): List<String>
}

interface SupplierRepository {
    fun observeSuppliers(): Flow<List<Supplier>>
    fun observeSupplier(id: Long): Flow<Supplier?>
    suspend fun getSuppliers(): List<Supplier>
    suspend fun getSupplier(id: Long): Supplier?
    suspend fun save(supplier: Supplier): Long
    suspend fun delete(supplier: Supplier)

    fun observePayments(supplierId: Long): Flow<List<SupplierPayment>>
    suspend fun getPayments(supplierId: Long): List<SupplierPayment>
    suspend fun getPaymentsForProject(projectId: Long): List<SupplierPayment>
    suspend fun savePayment(payment: SupplierPayment): Long
    suspend fun deletePayment(id: Long)
    fun observePaidPerSupplier(): Flow<List<IdAmount>>
    fun observePaidForProject(projectId: Long): Flow<Long>
}

/** Days worked by one labour. */
data class LabourDayCount(val labourId: Long, val present: Int, val half: Int)

interface LabourRepository {
    fun observeLabours(projectId: Long): Flow<List<Labour>>
    fun observeLabour(id: Long): Flow<Labour?>
    suspend fun getLabours(projectId: Long): List<Labour>
    suspend fun getLabour(id: Long): Labour?
    suspend fun save(labour: Labour): Long
    suspend fun delete(labour: Labour)

    suspend fun setAttendance(attendance: Attendance)
    suspend fun clearAttendance(labourId: Long, date: Long)
    fun observeAttendanceForDay(projectId: Long, date: Long): Flow<List<Attendance>>
    fun observeAttendanceInRange(projectId: Long, from: Long, to: Long): Flow<List<Attendance>>
    fun observeAttendanceForLabour(labourId: Long): Flow<List<Attendance>>
    suspend fun getAttendance(projectId: Long): List<Attendance>
    fun observeEarnedPerLabour(projectId: Long): Flow<List<IdAmount>>
    fun observeDaysPerLabour(projectId: Long): Flow<List<LabourDayCount>>

    fun observePayments(labourId: Long): Flow<List<LabourPayment>>
    fun observePaymentsInRange(projectId: Long, from: Long, to: Long): Flow<List<LabourPayment>>
    suspend fun getPayments(projectId: Long): List<LabourPayment>
    suspend fun addPayment(payment: LabourPayment): Long
    suspend fun deletePayment(id: Long)
    fun observePaidPerLabour(projectId: Long): Flow<List<IdAmount>>
}

interface ContractorRepository {
    fun observeContractors(projectId: Long): Flow<List<Contractor>>
    fun observeContractor(id: Long): Flow<Contractor?>
    suspend fun getContractors(projectId: Long): List<Contractor>
    suspend fun getContractor(id: Long): Contractor?
    suspend fun save(contractor: Contractor): Long
    suspend fun delete(contractor: Contractor)

    fun observePayments(contractorId: Long): Flow<List<ContractorPayment>>
    suspend fun getPayments(projectId: Long): List<ContractorPayment>
    suspend fun addPayment(payment: ContractorPayment): Long
    suspend fun deletePayment(id: Long)
    fun observePaidPerContractor(projectId: Long): Flow<List<IdAmount>>
}

interface ExpenseRepository {
    fun observeExpenses(projectId: Long): Flow<List<Expense>>
    suspend fun getExpenses(projectId: Long): List<Expense>
    suspend fun getExpense(id: Long): Expense?
    suspend fun save(expense: Expense): Long
    suspend fun delete(expense: Expense)
    fun observeTotal(projectId: Long): Flow<Long>
    fun observePerStage(projectId: Long): Flow<List<IdAmount>>
    suspend fun billPhotos(projectId: Long): List<String>
}

interface PhotoRepository {
    fun observePhotos(projectId: Long): Flow<List<Photo>>
    suspend fun getPhoto(id: Long): Photo?
    suspend fun photoPaths(projectId: Long): List<String>
    suspend fun save(photo: Photo): Long
    suspend fun delete(photo: Photo)
}
