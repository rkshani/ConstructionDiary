package com.diary.construction.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.diary.construction.domain.model.Attendance
import com.diary.construction.domain.model.Contractor
import com.diary.construction.domain.model.ContractorPayment
import com.diary.construction.domain.model.IdAmount
import com.diary.construction.domain.model.Labour
import com.diary.construction.domain.model.LabourPayment
import kotlinx.coroutines.flow.Flow

/** Days worked by one labour. */
data class LabourDays(val labourId: Long, val present: Int, val half: Int)

@Dao
interface LabourDao {
    @Query("SELECT * FROM labours WHERE projectId = :projectId ORDER BY active DESC, name COLLATE NOCASE")
    fun observeForProject(projectId: Long): Flow<List<Labour>>

    @Query("SELECT * FROM labours WHERE projectId = :projectId ORDER BY active DESC, name COLLATE NOCASE")
    suspend fun getForProject(projectId: Long): List<Labour>

    @Query("SELECT * FROM labours WHERE id = :id")
    fun observe(id: Long): Flow<Labour?>

    @Query("SELECT * FROM labours WHERE id = :id")
    suspend fun get(id: Long): Labour?

    @Insert
    suspend fun insert(labour: Labour): Long

    @Update
    suspend fun update(labour: Labour)

    @Delete
    suspend fun delete(labour: Labour)

    // --- attendance (a leaf table, so REPLACE on the labour+date unique index is safe) ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAttendance(attendance: Attendance)

    @Query("DELETE FROM attendance WHERE labourId = :labourId AND date = :date")
    suspend fun deleteAttendance(labourId: Long, date: Long)

    @Query("SELECT * FROM attendance WHERE projectId = :projectId AND date = :date")
    fun observeAttendanceForDay(projectId: Long, date: Long): Flow<List<Attendance>>

    @Query("SELECT * FROM attendance WHERE projectId = :projectId AND date BETWEEN :from AND :to ORDER BY date")
    fun observeAttendanceInRange(projectId: Long, from: Long, to: Long): Flow<List<Attendance>>

    @Query("SELECT * FROM attendance WHERE labourId = :labourId ORDER BY date DESC")
    fun observeAttendanceForLabour(labourId: Long): Flow<List<Attendance>>

    @Query("SELECT * FROM attendance WHERE projectId = :projectId ORDER BY date")
    suspend fun getAttendanceForProject(projectId: Long): List<Attendance>

    @Query(
        """
        SELECT labourId AS id,
            COALESCE(SUM(wage * (CASE status WHEN 'PRESENT' THEN 100 WHEN 'HALF_DAY' THEN 50 ELSE 0 END) / 100), 0) AS amount
        FROM attendance WHERE projectId = :projectId GROUP BY labourId
        """,
    )
    fun observeEarnedPerLabour(projectId: Long): Flow<List<IdAmount>>

    @Query(
        """
        SELECT labourId,
            COALESCE(SUM(CASE WHEN status = 'PRESENT' THEN 1 ELSE 0 END), 0) AS present,
            COALESCE(SUM(CASE WHEN status = 'HALF_DAY' THEN 1 ELSE 0 END), 0) AS half
        FROM attendance WHERE projectId = :projectId GROUP BY labourId
        """,
    )
    fun observeDaysPerLabour(projectId: Long): Flow<List<LabourDays>>

    // --- payments ---
    @Query("SELECT * FROM labour_payments WHERE labourId = :labourId ORDER BY date DESC, id DESC")
    fun observePayments(labourId: Long): Flow<List<LabourPayment>>

    @Query("SELECT * FROM labour_payments WHERE projectId = :projectId AND date BETWEEN :from AND :to ORDER BY date")
    fun observePaymentsInRange(projectId: Long, from: Long, to: Long): Flow<List<LabourPayment>>

    @Query("SELECT * FROM labour_payments WHERE projectId = :projectId ORDER BY date, id")
    suspend fun getPaymentsForProject(projectId: Long): List<LabourPayment>

    @Insert
    suspend fun insertPayment(payment: LabourPayment): Long

    @Query("DELETE FROM labour_payments WHERE id = :id")
    suspend fun deletePayment(id: Long)

    @Query("SELECT labourId AS id, COALESCE(SUM(amount), 0) AS amount FROM labour_payments WHERE projectId = :projectId GROUP BY labourId")
    fun observePaidPerLabour(projectId: Long): Flow<List<IdAmount>>
}

@Dao
interface ContractorDao {
    @Query("SELECT * FROM contractors WHERE projectId = :projectId ORDER BY createdAt")
    fun observeForProject(projectId: Long): Flow<List<Contractor>>

    @Query("SELECT * FROM contractors WHERE projectId = :projectId ORDER BY createdAt")
    suspend fun getForProject(projectId: Long): List<Contractor>

    @Query("SELECT * FROM contractors WHERE id = :id")
    fun observe(id: Long): Flow<Contractor?>

    @Query("SELECT * FROM contractors WHERE id = :id")
    suspend fun get(id: Long): Contractor?

    @Insert
    suspend fun insert(contractor: Contractor): Long

    @Update
    suspend fun update(contractor: Contractor)

    @Delete
    suspend fun delete(contractor: Contractor)

    @Query("SELECT * FROM contractor_payments WHERE contractorId = :contractorId ORDER BY date DESC, id DESC")
    fun observePayments(contractorId: Long): Flow<List<ContractorPayment>>

    @Query("SELECT * FROM contractor_payments WHERE projectId = :projectId ORDER BY date, id")
    suspend fun getPaymentsForProject(projectId: Long): List<ContractorPayment>

    @Insert
    suspend fun insertPayment(payment: ContractorPayment): Long

    @Query("DELETE FROM contractor_payments WHERE id = :id")
    suspend fun deletePayment(id: Long)

    @Query(
        """
        SELECT contractorId AS id, COALESCE(SUM(amount), 0) AS amount FROM contractor_payments
        WHERE projectId = :projectId GROUP BY contractorId
        """,
    )
    fun observePaidPerContractor(projectId: Long): Flow<List<IdAmount>>
}
