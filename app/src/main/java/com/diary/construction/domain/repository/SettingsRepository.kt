package com.diary.construction.domain.repository

import com.diary.construction.domain.model.AppLanguage
import com.diary.construction.domain.model.AppSettings
import com.diary.construction.domain.model.AreaUnit
import com.diary.construction.domain.model.ThemeMode
import com.diary.construction.domain.model.UserRole
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<AppSettings>

    suspend fun current(): AppSettings

    suspend fun setRole(role: UserRole)
    suspend fun setOnboardingDone(done: Boolean)
    suspend fun setLanguage(language: AppLanguage)
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setCurrency(currency: String)
    suspend fun setAreaUnit(unit: AreaUnit)
    suspend fun setLock(enabled: Boolean, pinHash: String?)
    suspend fun setBiometric(enabled: Boolean)
    suspend fun setSelectedProject(projectId: Long?)
    suspend fun setLabourReminder(enabled: Boolean, payDay: Int)
    suspend fun setSupplierReminder(enabled: Boolean)
    suspend fun setStageReminder(enabled: Boolean)
}
