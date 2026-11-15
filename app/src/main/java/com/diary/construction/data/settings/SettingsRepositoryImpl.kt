package com.diary.construction.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.diary.construction.domain.model.AppLanguage
import com.diary.construction.domain.model.AppSettings
import com.diary.construction.domain.model.AreaUnit
import com.diary.construction.domain.model.ThemeMode
import com.diary.construction.domain.model.UserRole
import com.diary.construction.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val store: DataStore<Preferences>,
    @ApplicationContext private val context: Context,
) : SettingsRepository {

    private object Keys {
        val ROLE = stringPreferencesKey("role")
        val ONBOARDING = booleanPreferencesKey("onboarding_done")
        val LANGUAGE = stringPreferencesKey("language")
        val THEME = stringPreferencesKey("theme")
        val CURRENCY = stringPreferencesKey("currency")
        val AREA_UNIT = stringPreferencesKey("area_unit")
        val LOCK = booleanPreferencesKey("lock_enabled")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val BIOMETRIC = booleanPreferencesKey("biometric")
        val SELECTED_PROJECT = longPreferencesKey("selected_project")
        val LABOUR_REMINDER = booleanPreferencesKey("labour_reminder")
        val LABOUR_PAY_DAY = intPreferencesKey("labour_pay_day")
        val SUPPLIER_REMINDER = booleanPreferencesKey("supplier_reminder")
        val STAGE_REMINDER = booleanPreferencesKey("stage_reminder")
    }

    override val settings: Flow<AppSettings> = store.data.map { p ->
        AppSettings(
            role = p[Keys.ROLE]?.let { name -> UserRole.entries.firstOrNull { it.name == name } },
            onboardingDone = p[Keys.ONBOARDING] ?: false,
            language = enumOr(p[Keys.LANGUAGE], AppLanguage.SYSTEM),
            themeMode = enumOr(p[Keys.THEME], ThemeMode.SYSTEM),
            currency = p[Keys.CURRENCY] ?: AppSettings.DEFAULT_CURRENCY,
            areaUnit = enumOr(p[Keys.AREA_UNIT], AreaUnit.MARLA),
            lockEnabled = p[Keys.LOCK] ?: false,
            pinHash = p[Keys.PIN_HASH],
            biometricEnabled = p[Keys.BIOMETRIC] ?: false,
            selectedProjectId = p[Keys.SELECTED_PROJECT],
            labourReminder = p[Keys.LABOUR_REMINDER] ?: false,
            labourPayDay = p[Keys.LABOUR_PAY_DAY] ?: 7,
            supplierReminder = p[Keys.SUPPLIER_REMINDER] ?: false,
            stageReminder = p[Keys.STAGE_REMINDER] ?: false,
        )
    }

    override suspend fun current(): AppSettings = settings.first()

    override suspend fun setRole(role: UserRole) { store.edit { it[Keys.ROLE] = role.name } }

    override suspend fun setOnboardingDone(done: Boolean) { store.edit { it[Keys.ONBOARDING] = done } }

    override suspend fun setLanguage(language: AppLanguage) {
        store.edit { it[Keys.LANGUAGE] = language.name }
        LocaleStore.save(context, language)
    }

    override suspend fun setThemeMode(mode: ThemeMode) { store.edit { it[Keys.THEME] = mode.name } }

    override suspend fun setCurrency(currency: String) { store.edit { it[Keys.CURRENCY] = currency } }

    override suspend fun setAreaUnit(unit: AreaUnit) { store.edit { it[Keys.AREA_UNIT] = unit.name } }

    override suspend fun setLock(enabled: Boolean, pinHash: String?) {
        store.edit {
            it[Keys.LOCK] = enabled
            if (pinHash != null) {
                it[Keys.PIN_HASH] = pinHash
            } else {
                it.remove(Keys.PIN_HASH)
            }
            if (!enabled) it[Keys.BIOMETRIC] = false
        }
    }

    override suspend fun setBiometric(enabled: Boolean) { store.edit { it[Keys.BIOMETRIC] = enabled } }

    override suspend fun setSelectedProject(projectId: Long?) {
        store.edit {
            if (projectId != null) {
                it[Keys.SELECTED_PROJECT] = projectId
            } else {
                it.remove(Keys.SELECTED_PROJECT)
            }
        }
    }

    override suspend fun setLabourReminder(enabled: Boolean, payDay: Int) {
        store.edit {
            it[Keys.LABOUR_REMINDER] = enabled
            it[Keys.LABOUR_PAY_DAY] = payDay
        }
    }

    override suspend fun setSupplierReminder(enabled: Boolean) { store.edit { it[Keys.SUPPLIER_REMINDER] = enabled } }

    override suspend fun setStageReminder(enabled: Boolean) { store.edit { it[Keys.STAGE_REMINDER] = enabled } }

    private inline fun <reified T : Enum<T>> enumOr(name: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: default
}
