package com.diary.construction.domain.repository

/** Deletes files the app saved (photos, bills). */
interface FileStore {
    suspend fun delete(paths: List<String>)
}
