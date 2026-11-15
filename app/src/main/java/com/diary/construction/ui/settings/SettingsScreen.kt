package com.diary.construction.ui.settings

import android.app.Activity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.diary.construction.BuildConfig
import com.diary.construction.R
import com.diary.construction.core.util.Pin
import com.diary.construction.domain.model.AppLanguage
import com.diary.construction.domain.model.AppSettings
import com.diary.construction.domain.model.AreaUnit
import com.diary.construction.domain.model.ThemeMode
import com.diary.construction.domain.model.UserRole
import com.diary.construction.domain.repository.SettingsRepository
import com.diary.construction.ui.components.AppTopBar
import com.diary.construction.ui.components.ChipSelector
import com.diary.construction.ui.components.DropdownField
import com.diary.construction.ui.components.InfoCard
import com.diary.construction.ui.components.SectionHeader
import com.diary.construction.ui.components.labelRes
import com.diary.construction.ui.lock.PinEntry
import com.diary.construction.ui.lock.canUseBiometric
import com.diary.construction.ui.reminders.SwitchRow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
) : ViewModel() {
    val state: StateFlow<AppSettings?> = settings.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setRole(role: UserRole) = io { settings.setRole(role) }
    fun setTheme(mode: ThemeMode) = io { settings.setThemeMode(mode) }
    fun setCurrency(currency: String) = io { settings.setCurrency(currency) }
    fun setAreaUnit(unit: AreaUnit) = io { settings.setAreaUnit(unit) }
    fun setBiometric(enabled: Boolean) = io { settings.setBiometric(enabled) }
    fun setPin(pin: String) = io { settings.setLock(true, Pin.hash(pin)) }
    fun disableLock() = io { settings.setLock(false, null) }

    /** Saves the language, then [onSaved] recreates the screen in the new language. */
    fun setLanguage(language: AppLanguage, onSaved: () -> Unit) {
        viewModelScope.launch {
            settings.setLanguage(language)
            onSaved()
        }
    }

    private fun io(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}

@Composable
fun SettingsScreen(
    onOpenReminders: () -> Unit,
    onOpenCalculator: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenSuppliers: () -> Unit,
    onOpenMaterials: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var setPin by remember { mutableStateOf(false) }

    Scaffold(topBar = { AppTopBar(stringResource(R.string.tab_settings)) }) { padding ->
        val s = state ?: return@Scaffold
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SectionHeader(stringResource(R.string.settings_profile))
            ChipSelector(
                options = UserRole.entries,
                selected = s.role ?: UserRole.OWNER,
                optionLabel = { stringResource(if (it == UserRole.OWNER) R.string.role_owner else R.string.role_thekedar) },
                onSelect = viewModel::setRole,
            )

            SectionHeader(stringResource(R.string.settings_appearance))
            Text(stringResource(R.string.settings_language), style = MaterialTheme.typography.bodyMedium)
            ChipSelector(
                options = AppLanguage.entries,
                selected = s.language,
                optionLabel = { stringResource(it.labelRes) },
                onSelect = { lang ->
                    if (lang != s.language) viewModel.setLanguage(lang) { (context as? Activity)?.recreate() }
                },
            )
            Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.bodyMedium)
            ChipSelector(
                options = ThemeMode.entries,
                selected = s.themeMode,
                optionLabel = { stringResource(it.labelRes) },
                onSelect = viewModel::setTheme,
            )

            SectionHeader(stringResource(R.string.settings_money_units))
            DropdownField(
                label = stringResource(R.string.settings_currency),
                options = AppSettings.CURRENCIES,
                selected = s.currency,
                optionLabel = { it.ifBlank { stringResource(R.string.settings_no_symbol) } },
                onSelect = viewModel::setCurrency,
            )
            DropdownField(
                label = stringResource(R.string.settings_area_unit),
                options = AreaUnit.entries,
                selected = s.areaUnit,
                optionLabel = { stringResource(it.labelRes) },
                onSelect = viewModel::setAreaUnit,
            )
            Text(
                stringResource(R.string.settings_area_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SectionHeader(stringResource(R.string.settings_security))
            InfoCard {
                SwitchRow(stringResource(R.string.settings_app_lock), stringResource(R.string.settings_app_lock_desc), s.lockEnabled) {
                    if (it) {
                        setPin = true
                    } else {
                        viewModel.disableLock()
                    }
                }
                val activity = context as? FragmentActivity
                if (s.lockEnabled && activity != null && canUseBiometric(activity)) {
                    SwitchRow(stringResource(R.string.settings_biometric), null, s.biometricEnabled, viewModel::setBiometric)
                }
                if (s.lockEnabled) {
                    Text(
                        stringResource(R.string.settings_change_pin),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { setPin = true }.padding(vertical = 8.dp),
                    )
                }
            }

            SectionHeader(stringResource(R.string.settings_tools))
            NavRow(Icons.Outlined.Notifications, stringResource(R.string.reminders), onOpenReminders)
            NavRow(Icons.Outlined.Calculate, stringResource(R.string.section_calculator), onOpenCalculator)
            NavRow(Icons.Outlined.LocalShipping, stringResource(R.string.suppliers_khata), onOpenSuppliers)
            NavRow(Icons.Outlined.Category, stringResource(R.string.material_types), onOpenMaterials)
            NavRow(Icons.Outlined.Backup, stringResource(R.string.backup_title), onOpenBackup)

            SectionHeader(stringResource(R.string.settings_about))
            Text(
                stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(R.string.settings_offline_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    if (setPin) {
        SetPinDialog(
            onDone = { pin -> viewModel.setPin(pin); setPin = false },
            onDismiss = { setPin = false },
        )
    }
}

/** Two steps: type a new PIN, then type it again. */
@Composable
private fun SetPinDialog(onDone: (String) -> Unit, onDismiss: () -> Unit) {
    var first by remember { mutableStateOf<String?>(null) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) {
            val firstPin = first
            if (firstPin == null) {
                PinEntry(
                    title = stringResource(R.string.pin_new),
                    subtitle = stringResource(R.string.pin_new_hint),
                    onComplete = { first = it; true },
                    extra = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
                )
            } else {
                PinEntry(
                    title = stringResource(R.string.pin_confirm),
                    onComplete = { pin ->
                        val same = pin == firstPin
                        if (same) {
                            onDone(pin)
                        } else {
                            first = null
                        }
                        same
                    },
                    extra = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
                )
            }
        }
    }
}

@Composable
fun NavRow(icon: ImageVector, title: String, onClick: () -> Unit, subtitle: String? = null) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}
