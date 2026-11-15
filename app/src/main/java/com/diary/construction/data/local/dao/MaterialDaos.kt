package com.diary.construction.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.diary.construction.domain.model.IdAmount
import com.diary.construction.domain.model.MaterialEntry
import com.diary.construction.domain.model.MaterialStock
import com.diary.construction.domain.model.MaterialType
import com.diary.construction.domain.model.Supplier
import com.diary.construction.domain.model.SupplierPayment
import kotlinx.coroutines.flow.Flow

@Dao
interface MaterialDao {
    // --- types ---
    @Query("SELECT * FROM material_types ORDER BY isBuiltIn DESC, id")
    fun observeTypes(): Flow<List<MaterialType>>

    @Query("SELECT * FROM material_types ORDER BY isBuiltIn DESC, id")
    suspend fun getTypes(): List<MaterialType>

    @Insert
    suspend fun insertType(type: MaterialType): Long

    @Update
    suspend fun updateType(type: MaterialType)

    @Delete
    suspend fun deleteType(type: MaterialType)

    // --- entries ---
    @Query("SELECT * FROM material_entries WHERE projectId = :projectId ORDER BY date DESC, id DESC")
    fun observeEntries(projectId: Long): Flow<List<MaterialEntry>>

    @Query("SELECT * FROM material_entries WHERE projectId = :projectId ORDER BY date, id")
    suspend fun getEntries(projectId: Long): List<MaterialEntry>

    @Query("SELECT * FROM material_entries WHERE supplierId = :supplierId ORDER BY date DESC, id DESC")
    fun observeEntriesForSupplier(supplierId: Long): Flow<List<MaterialEntry>>

    @Query("SELECT * FROM material_entries WHERE supplierId = :supplierId ORDER BY date, id")
    suspend fun getEntriesForSupplier(supplierId: Long): List<MaterialEntry>

    @Query("SELECT * FROM material_entries WHERE id = :id")
    suspend fun getEntry(id: Long): MaterialEntry?

    @Insert
    suspend fun insertEntry(entry: MaterialEntry): Long

    @Update
    suspend fun updateEntry(entry: MaterialEntry)

    @Delete
    suspend fun deleteEntry(entry: MaterialEntry)

    @Query(
        """
        SELECT materialTypeId,
            COALESCE(SUM(CASE WHEN entryType = 'RECEIVED' THEN quantity ELSE 0 END), 0) AS received,
            COALESCE(SUM(CASE WHEN entryType = 'USED' THEN quantity ELSE 0 END), 0) AS used,
            COALESCE(SUM(CASE WHEN entryType = 'RECEIVED' THEN total ELSE 0 END), 0) AS cost
        FROM material_entries WHERE projectId = :projectId GROUP BY materialTypeId
        """,
    )
    fun observeStock(projectId: Long): Flow<List<MaterialStock>>

    @Query("SELECT COALESCE(SUM(total), 0) FROM material_entries WHERE projectId = :projectId AND entryType = 'RECEIVED'")
    fun observeCost(projectId: Long): Flow<Long>

    @Query(
        """
        SELECT supplierId AS id, COALESCE(SUM(total), 0) AS amount FROM material_entries
        WHERE supplierId IS NOT NULL AND entryType = 'RECEIVED' GROUP BY supplierId
        """,
    )
    fun observeTakenPerSupplier(): Flow<List<IdAmount>>

    @Query(
        """
        SELECT COALESCE(SUM(total), 0) FROM material_entries
        WHERE projectId = :projectId AND supplierId IS NOT NULL AND entryType = 'RECEIVED'
        """,
    )
    fun observeSupplierTakenForProject(projectId: Long): Flow<Long>

    @Query(
        """
        SELECT stageId AS id, COALESCE(SUM(total), 0) AS amount FROM material_entries
        WHERE projectId = :projectId AND stageId IS NOT NULL AND entryType = 'RECEIVED' GROUP BY stageId
        """,
    )
    fun observeCostPerStage(projectId: Long): Flow<List<IdAmount>>

    @Query("SELECT billPhotoPath FROM material_entries WHERE projectId = :projectId AND billPhotoPath IS NOT NULL")
    suspend fun billPhotos(projectId: Long): List<String>
}

@Dao
interface SupplierDao {
    @Query("SELECT * FROM suppliers ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<Supplier>>

    @Query("SELECT * FROM suppliers ORDER BY name COLLATE NOCASE")
    suspend fun getAll(): List<Supplier>

    @Query("SELECT * FROM suppliers WHERE id = :id")
    fun observe(id: Long): Flow<Supplier?>

    @Query("SELECT * FROM suppliers WHERE id = :id")
    suspend fun get(id: Long): Supplier?

    @Insert
    suspend fun insert(supplier: Supplier): Long

    @Update
    suspend fun update(supplier: Supplier)

    @Delete
    suspend fun delete(supplier: Supplier)

    @Query("SELECT * FROM supplier_payments WHERE supplierId = :supplierId ORDER BY date DESC, id DESC")
    fun observePayments(supplierId: Long): Flow<List<SupplierPayment>>

    @Query("SELECT * FROM supplier_payments WHERE supplierId = :supplierId ORDER BY date, id")
    suspend fun getPayments(supplierId: Long): List<SupplierPayment>

    @Query("SELECT * FROM supplier_payments WHERE projectId = :projectId ORDER BY date, id")
    suspend fun getPaymentsForProject(projectId: Long): List<SupplierPayment>

    @Insert
    suspend fun insertPayment(payment: SupplierPayment): Long

    @Update
    suspend fun updatePayment(payment: SupplierPayment)

    @Query("DELETE FROM supplier_payments WHERE id = :id")
    suspend fun deletePayment(id: Long)

    @Query("SELECT supplierId AS id, COALESCE(SUM(amount), 0) AS amount FROM supplier_payments GROUP BY supplierId")
    fun observePaidPerSupplier(): Flow<List<IdAmount>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM supplier_payments WHERE projectId = :projectId")
    fun observePaidForProject(projectId: Long): Flow<Long>
}
