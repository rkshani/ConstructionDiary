package com.diary.construction.domain.model

/** All user settings (kept in DataStore). */
data class AppSettings(
    /** null until the user picks a role on first launch. */
    val role: UserRole? = null,
    val onboardingDone: Boolean = false,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** Shown before amounts, e.g. "Rs". */
    val currency: String = DEFAULT_CURRENCY,
    val areaUnit: AreaUnit = AreaUnit.MARLA,
    val lockEnabled: Boolean = false,
    val pinHash: String? = null,
    val biometricEnabled: Boolean = false,
    /** Project picked on the Expenses / Reports tabs. */
    val selectedProjectId: Long? = null,
    val labourReminder: Boolean = false,
    /** java.util.Calendar day of week (Sunday = 1). */
    val labourPayDay: Int = 7,
    val supplierReminder: Boolean = false,
    val stageReminder: Boolean = false,
) {
    companion object {
        const val DEFAULT_CURRENCY = "Rs"
        val CURRENCIES = listOf("Rs", "PKR", "₹", "INR", "AED", "SAR", "$", "")
    }
}
