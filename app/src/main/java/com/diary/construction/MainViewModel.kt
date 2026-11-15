package com.diary.construction

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diary.construction.domain.model.AppSettings
import com.diary.construction.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
) : ViewModel() {

    /** null until the first read finishes. */
    val settings: StateFlow<AppSettings?> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _unlocked = MutableStateFlow(false)

    /** App lock: false until the PIN or fingerprint is given (ignored when the lock is off). */
    val unlocked: StateFlow<Boolean> = _unlocked.asStateFlow()

    private var stoppedAt = 0L

    init {
        // Turning the lock on inside the app should not lock the user out right away.
        viewModelScope.launch {
            var before: Boolean? = null
            settingsRepository.settings.map { it.lockEnabled }.distinctUntilChanged().collect { enabled ->
                if (before == false && enabled) _unlocked.value = true
                before = enabled
            }
        }
    }

    fun onUnlocked() {
        _unlocked.value = true
    }

    fun onStop(changingConfig: Boolean) {
        if (!changingConfig) stoppedAt = SystemClock.elapsedRealtime()
    }

    /** Lock again when the app was in the background for a while (not for a quick camera trip). */
    fun onStart() {
        if (stoppedAt != 0L && SystemClock.elapsedRealtime() - stoppedAt > RELOCK_AFTER_MS) _unlocked.value = false
        stoppedAt = 0L
    }

    private companion object {
        const val RELOCK_AFTER_MS = 60_000L
    }
}
