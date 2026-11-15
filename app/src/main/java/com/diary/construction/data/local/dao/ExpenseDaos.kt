package com.diary.construction.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.diary.construction.domain.model.Expense
import com.diary.construction.domain.model.IdAmount
import com.diary.construction.domain.model.Photo
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE projectId = :projectId ORDER BY date DESC, id DESC")
    fun observeForProject(projectId: Long): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE projectId = :projectId ORDER BY date, id")
    suspend fun getForProject(projectId: Long): List<Expense>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun get(id: Long): Expense?

    @Insert
    suspend fun insert(expense: Expense): Long

    @Update
    suspend fun update(expense: Expense)

    @Delete
    suspend fun delete(expense: Expense)

    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE projectId = :projectId")
    fun observeTotal(projectId: Long): Flow<Long>

    @Query("SELECT stageId AS id, COALESCE(SUM(amount), 0) AS amount FROM expenses WHERE projectId = :projectId AND stageId IS NOT NULL GROUP BY stageId")
    fun observePerStage(projectId: Long): Flow<List<IdAmount>>

    @Query("SELECT billPhotoPath FROM expenses WHERE projectId = :projectId AND billPhotoPath IS NOT NULL")
    suspend fun billPhotos(projectId: Long): List<String>
}

@Dao
interface PhotoDao {
    @Query("SELECT * FROM photos WHERE projectId = :projectId ORDER BY date DESC, createdAt DESC")
    fun observeForProject(projectId: Long): Flow<List<Photo>>

    @Query("SELECT * FROM photos WHERE id = :id")
    suspend fun get(id: Long): Photo?

    @Query("SELECT path FROM photos WHERE projectId = :projectId")
    suspend fun pathsForProject(projectId: Long): List<String>

    @Insert
    suspend fun insert(photo: Photo): Long

    @Update
    suspend fun update(photo: Photo)

    @Delete
    suspend fun delete(photo: Photo)
}
