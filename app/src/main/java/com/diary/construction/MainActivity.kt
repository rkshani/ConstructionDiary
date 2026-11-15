package com.diary.construction

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.diary.construction.ui.navigation.ConstructionAppUi
import com.diary.construction.ui.navigation.Routes
import com.diary.construction.ui.theme.ConstructionTheme
import dagger.hilt.android.AndroidEntryPoint

/** FragmentActivity (not ComponentActivity) so the biometric prompt can be shown later. */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ConstructionTheme {
                ConstructionAppUi(startDestination = Routes.PROJECTS)
            }
        }
    }
}
