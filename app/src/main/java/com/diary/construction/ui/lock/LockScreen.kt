package com.diary.construction.ui.lock

import androidx.activity.compose.BackHandler
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.diary.construction.R
import com.diary.construction.core.util.Pin

/** True when the phone has a fingerprint / face unlock set up. */
fun canUseBiometric(activity: FragmentActivity): Boolean =
    BiometricManager.from(activity).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS

/** Shows the system fingerprint / face prompt. */
fun showBiometricPrompt(activity: FragmentActivity, title: String, cancel: String, onSuccess: () -> Unit) {
    val prompt = BiometricPrompt(
        activity,
        ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
        },
    )
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle(title)
        .setNegativeButtonText(cancel)
        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
        .build()
    runCatching { prompt.authenticate(info) }
}

/** Full screen PIN lock with an optional fingerprint button. */
@Composable
fun LockScreen(
    pinHash: String?,
    biometricEnabled: Boolean,
    onUnlocked: () -> Unit,
) {
    val activity = LocalContext.current as? FragmentActivity
    val title = stringResource(R.string.lock_title)
    val cancel = stringResource(R.string.lock_use_pin)
    // Set only when the fingerprint / face button should be offered.
    val bioActivity = activity?.takeIf { biometricEnabled && canUseBiometric(it) }
    val useBiometric = bioActivity != null
    val askBiometric = {
        if (bioActivity != null) showBiometricPrompt(bioActivity, title, cancel, onUnlocked)
    }
    LaunchedEffect(Unit) { askBiometric() }
    // Back on the lock screen leaves the app instead of going to the screen below.
    BackHandler { activity?.moveTaskToBack(true) }

    Surface(Modifier.fillMaxSize()) {
        PinEntry(
            title = title,
            onComplete = { pin -> Pin.matches(pin, pinHash).also { if (it) onUnlocked() } },
            extra = {
                if (useBiometric) {
                    IconButton(onClick = askBiometric, modifier = Modifier.size(64.dp)) {
                        Icon(Icons.Outlined.Fingerprint, contentDescription = stringResource(R.string.lock_fingerprint), modifier = Modifier.size(40.dp))
                    }
                }
            },
        )
    }
}

/**
 * PIN pad. [onComplete] gets the typed PIN when it is [Pin.LENGTH] digits long and returns
 * true if it was accepted; on false the dots shake back to empty with an error.
 */
@Composable
fun PinEntry(
    title: String,
    onComplete: (String) -> Boolean,
    subtitle: String? = null,
    extra: @Composable () -> Unit = {},
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().systemBarsPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Outlined.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleLarge)
        if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            repeat(Pin.LENGTH) { i ->
                Box(
                    Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                error -> MaterialTheme.colorScheme.error
                                i < pin.length -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.outlineVariant
                            },
                        ),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            if (error) stringResource(R.string.lock_wrong_pin) else " ",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(16.dp))
        // Keypad stays left-to-right in every language.
        val keys = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("", "0", "<"))
        keys.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                row.forEach { key ->
                    when (key) {
                        "" -> Spacer(Modifier.size(72.dp))
                        "<" -> IconButton(onClick = { pin = pin.dropLast(1); error = false }, modifier = Modifier.size(72.dp)) {
                            Icon(Icons.AutoMirrored.Outlined.Backspace, contentDescription = stringResource(R.string.action_clear))
                        }
                        else -> FilledTonalButton(
                            onClick = {
                                if (pin.length < Pin.LENGTH) {
                                    error = false
                                    pin += key
                                    if (pin.length == Pin.LENGTH) {
                                        val ok = onComplete(pin)
                                        if (!ok) error = true
                                        pin = ""
                                    }
                                }
                            },
                            modifier = Modifier.size(72.dp),
                            shape = CircleShape,
                        ) { Text(key, style = MaterialTheme.typography.titleLarge) }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        extra()
    }
}
