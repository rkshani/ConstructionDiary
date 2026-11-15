package com.diary.construction

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.diary.construction.data.settings.LocaleStore
import com.diary.construction.domain.model.ThemeMode
import com.diary.construction.ui.components.LocalAppSettings
import com.diary.construction.ui.lock.LockScreen
import com.diary.construction.ui.navigation.ConstructionAppUi
import com.diary.construction.ui.navigation.Routes
import com.diary.construction.ui.theme.ConstructionTheme
import dagger.hilt.android.AndroidEntryPoint

/** FragmentActivity (not ComponentActivity) so the biometric prompt can be shown. */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleStore.wrap(newBase))
    }

    override fun onStart() {
        super.onStart()
        viewModel.onStart()
    }

    override fun onStop() {
        super.onStop()
        viewModel.onStop(isChangingConfigurations)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Keep the splash until the settings (theme, language, first launch, lock) are read.
        installSplashScreen().setKeepOnScreenCondition { viewModel.settings.value == null }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val loaded = settings ?: return@setContent
            val dark = when (loaded.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
            }
            ConstructionTheme(darkTheme = dark) {
                CompositionLocalProvider(LocalAppSettings provides loaded) {
                    // Decided once per launch so finishing the first-launch screens does not rebuild the graph.
                    val start = remember {
                        when {
                            !loaded.onboardingDone -> Routes.ONBOARDING
                            loaded.role == null -> Routes.ROLE
                            else -> Routes.PROJECTS
                        }
                    }
                    val unlocked by viewModel.unlocked.collectAsStateWithLifecycle()
                    Box {
                        // The app stays underneath the lock so picker / camera results are not lost.
                        ConstructionAppUi(startDestination = start)
                        if (loaded.lockEnabled && !unlocked) {
                            LockScreen(
                                pinHash = loaded.pinHash,
                                biometricEnabled = loaded.biometricEnabled,
                                onUnlocked = viewModel::onUnlocked,
                            )
                        }
                    }
                }
            }
        }
    }
}
