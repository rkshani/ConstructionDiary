package com.diary.construction.ui.reminders

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.diary.construction.R
import com.diary.construction.domain.model.AppSettings
import com.diary.construction.domain.repository.SettingsRepository
import com.diary.construction.ui.components.AppTopBar
import com.diary.construction.ui.components.DropdownField
import com.diary.construction.ui.components.InfoCard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.DateFormatSymbols
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class RemindersViewModel @Inject constructor(
    private val settings: SettingsRepository,
) : ViewModel() {
    val state: StateFlow<AppSettings?> = settings.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setLabour(enabled: Boolean, day: Int) {
        viewModelScope.launch { settings.setLabourReminder(enabled, day) }
    }

    fun setSupplier(enabled: Boolean) {
        viewModelScope.launch { settings.setSupplierReminder(enabled) }
    }

    fun setStage(enabled: Boolean) {
        viewModelScope.launch { settings.setStageReminder(enabled) }
    }
}

@Composable
fun RemindersScreen(
    onBack: () -> Unit,
    viewModel: RemindersViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Ask for the notification permission (Android 13+) the first time a reminder is turned on.
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val askPermission = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runCatching { permission.launch(Manifest.permission.POST_NOTIFICATIONS) }
        }
    }
    Scaffold(topBar = { AppTopBar(stringResource(R.string.reminders), onBack = onBack) }) { padding ->
        val s = state ?: return@Scaffold
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(R.string.reminders_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            InfoCard {
                SwitchRow(stringResource(R.string.reminder_labour), stringResource(R.string.reminder_labour_desc), s.labourReminder) {
                    if (it) askPermission()
                    viewModel.setLabour(it, s.labourPayDay)
                }
                if (s.labourReminder) {
                    val days = remember7Days()
                    DropdownField(
                        label = stringResource(R.string.pay_day),
                        options = days.map { it.first },
                        selected = s.labourPayDay,
                        optionLabel = { d -> days.first { it.first == d }.second },
                        onSelect = { viewModel.setLabour(true, it) },
                    )
                }
            }
            InfoCard {
                SwitchRow(stringResource(R.string.reminder_supplier), stringResource(R.string.reminder_supplier_desc), s.supplierReminder) {
                    if (it) askPermission()
                    viewModel.setSupplier(it)
                }
            }
            InfoCard {
                SwitchRow(stringResource(R.string.reminder_stage), stringResource(R.string.reminder_stage_desc), s.stageReminder) {
                    if (it) askPermission()
                    viewModel.setStage(it)
                }
            }
        }
    }
}

/** (Calendar day, local name) starting Monday. */
private fun remember7Days(): List<Pair<Int, String>> {
    val names = DateFormatSymbols.getInstance().weekdays
    return listOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY)
        .map { it to names[it] }
}

@Composable
fun SwitchRow(title: String, detail: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 8.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            if (detail != null) {
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
