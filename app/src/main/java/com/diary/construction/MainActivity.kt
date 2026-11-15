package com.diary.construction

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.diary.construction.data.settings.LocaleStore
import com.diary.construction.domain.model.ThemeMode
import com.diary.construction.ui.components.LocalAppSettings
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

    override fun onCreate(savedInstanceState: Bundle?) {
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
                    // Decided once per launch so picking a role does not rebuild the graph.
                    val start = remember { if (loaded.role == null) Routes.ROLE else Routes.PROJECTS }
                    ConstructionAppUi(startDestination = start)
                }
            }
        }
    }
}
