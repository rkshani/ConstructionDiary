package com.diary.construction.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.diary.construction.domain.model.IdAmount
import com.diary.construction.domain.model.Project
import com.diary.construction.domain.model.Stage
import kotlinx.coroutines.flow.Flow

/*
 * Parent rows are never written with REPLACE: SQLite REPLACE deletes the old row first and
 * that would cascade-delete all of its children. Use insert for new rows and update for old.
 */

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY CASE status WHEN 'COMPLETED' THEN 1 ELSE 0 END, createdAt DESC")
    fun observeAll(): Flow<List<Project>>

    @Query("SELECT * FROM projects WHERE id = :id")
    fun observe(id: Long): Flow<Project?>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun get(id: Long): Project?

    @Query("SELECT * FROM projects ORDER BY createdAt DESC")
    suspend fun getAll(): List<Project>

    @Insert
    suspend fun insert(project: Project): Long

    @Update
    suspend fun update(project: Project)

    @Delete
    suspend fun delete(project: Project)

    /** Cost per project for the list: material + labour (earned) + contractor paid + expenses. */
    @Query(
        """
        SELECT p.id AS id,
            COALESCE((SELECT SUM(total) FROM material_entries m WHERE m.projectId = p.id AND m.entryType = 'RECEIVED'), 0)
          + COALESCE((SELECT SUM(wage * (CASE status WHEN 'PRESENT' THEN 100 WHEN 'HALF_DAY' THEN 50 ELSE 0 END) / 100)
                FROM attendance a WHERE a.projectId = p.id), 0)
          + COALESCE((SELECT SUM(amount) FROM contractor_payments c WHERE c.projectId = p.id), 0)
          + COALESCE((SELECT SUM(amount) FROM expenses e WHERE e.projectId = p.id), 0) AS amount
        FROM projects p
        """,
    )
    fun observeSpentPerProject(): Flow<List<IdAmount>>
}

@Dao
interface StageDao {
    @Query("SELECT * FROM stages WHERE projectId = :projectId ORDER BY sortOrder, id")
    fun observeForProject(projectId: Long): Flow<List<Stage>>

    @Query("SELECT * FROM stages WHERE projectId = :projectId ORDER BY sortOrder, id")
    suspend fun getForProject(projectId: Long): List<Stage>

    @Query("SELECT * FROM stages WHERE id = :id")
    suspend fun get(id: Long): Stage?

    @Query("SELECT * FROM stages WHERE status != 'COMPLETED' AND endDate IS NOT NULL AND endDate <= :until")
    suspend fun getOpenEndingBefore(until: Long): List<Stage>

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM stages WHERE projectId = :projectId")
    suspend fun maxSortOrder(projectId: Long): Int

    @Insert
    suspend fun insert(stage: Stage): Long

    @Insert
    suspend fun insertAll(stages: List<Stage>)

    @Update
    suspend fun update(stage: Stage)

    @Update
    suspend fun updateAll(stages: List<Stage>)

    @Delete
    suspend fun delete(stage: Stage)
}
