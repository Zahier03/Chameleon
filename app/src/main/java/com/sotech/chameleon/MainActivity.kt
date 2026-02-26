package com.sotech.chameleon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.sotech.chameleon.data.ThemeSettings
import com.sotech.chameleon.ui.ChameleonApp
import com.sotech.chameleon.ui.MainViewModel
import com.sotech.chameleon.ui.theme.ChameleonTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val viewModel: MainViewModel = hiltViewModel()
            val themeSettings by viewModel.themeSettings.collectAsState()

            ChameleonTheme(
                themeSettings = themeSettings
            ) {
                // Configure system bars
                ConfigureSystemBars()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ChameleonApp()
                }
            }
        }
    }
}

@Composable
private fun ConfigureSystemBars() {
    val systemUiController = rememberSystemUiController()
    val useDarkIcons = !androidx.compose.foundation.isSystemInDarkTheme()
    val backgroundColor = MaterialTheme.colorScheme.background

    SideEffect {
        systemUiController.setSystemBarsColor(
            color = Color.Transparent,
            darkIcons = useDarkIcons,
            isNavigationBarContrastEnforced = false
        )

        // Make navigation bar transparent with scrim
        systemUiController.setNavigationBarColor(
            color = backgroundColor.copy(alpha = 0.8f),
            darkIcons = useDarkIcons,
            navigationBarContrastEnforced = false
        )
    }
}