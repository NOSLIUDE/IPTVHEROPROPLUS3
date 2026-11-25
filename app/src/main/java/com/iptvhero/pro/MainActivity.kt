package com.iptvhero.pro

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iptvhero.pro.ui.screens.FullPlayerScreen
import com.iptvhero.pro.ui.screens.MainScreen
import com.iptvhero.pro.ui.screens.MediaDetailsScreen
import com.iptvhero.pro.ui.screens.ProfileScreen
import com.iptvhero.pro.ui.screens.SplashScreen
import com.iptvhero.pro.ui.theme.HeroThemeType
import com.iptvhero.pro.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val mainViewModel: MainViewModel = viewModel()
            val uiState by mainViewModel.uiState.collectAsState()

            var currentRoute by remember { mutableStateOf("splash") }
            var currentTheme by remember { mutableStateOf(HeroThemeType.DUEL) }
            var backPressCount by remember { mutableIntStateOf(0) }
            var lastBackPressTime by remember { mutableLongStateOf(0L) }

            LaunchedEffect(uiState.navigateToMainScreen, currentRoute) {
                if (uiState.navigateToMainScreen && currentRoute != "main") {
                    mainViewModel.consumeNavigateToMainScreen()
                    currentRoute = "main"
                }
            }

            fun cycleTheme() {
                currentTheme = when (currentTheme) {
                    HeroThemeType.DUEL -> HeroThemeType.MARVEL
                    HeroThemeType.MARVEL -> HeroThemeType.DC
                    HeroThemeType.DC -> HeroThemeType.DUEL
                }
            }

            BackHandler(enabled = currentRoute != "splash") {
                when (currentRoute) {
                    "full_player" -> currentRoute = "main"
                    "details" -> currentRoute = "main"
                    "profiles" -> {
                        if (mainViewModel.activeProfile != null) {
                            currentRoute = "main"
                        } else {
                            finish()
                        }
                    }
                    "main" -> {
                        val now = System.currentTimeMillis()
                        if (now - lastBackPressTime < 2200L) {
                            backPressCount++
                        } else {
                            backPressCount = 1
                        }
                        lastBackPressTime = now
                        if (backPressCount >= 3) {
                            finish()
                        } else {
                            Toast.makeText(
                                this,
                                "Pressione Voltar mais ${3 - backPressCount}x para sair",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }

            when (currentRoute) {
                "splash" -> SplashScreen(
                    viewModel = mainViewModel,
                    currentTheme = currentTheme,
                    onFinishedToProfiles = {
                        currentRoute = "profiles"
                    }
                )

                "profiles" -> ProfileScreen(
                    viewModel = mainViewModel,
                    currentTheme = currentTheme,
                    onCycleTheme = { cycleTheme() },
                    onSelectProfile = { profileId ->
                        mainViewModel.selectProfileAndEnterApp(profileId)
                    },
                    onLoadHeroList = { url ->
                        mainViewModel.loadHeroListAndEnterApp(url = url, force = true)
                    },
                    onImportXtream = { server, user, pass ->
                        mainViewModel.importXtreamAndEnterApp(server, user, pass)
                    },
                    onImportBatchTxt = { txt ->
                        mainViewModel.importBatchTxtAndEnterApp(txt)
                    }
                )

                "main" -> MainScreen(
                    viewModel = mainViewModel,
                    currentTheme = currentTheme,
                    onCycleTheme = { cycleTheme() },
                    onBackToProfiles = {
                        mainViewModel.consumeNavigateToMainScreen()
                        currentRoute = "profiles"
                    },
                    onNavigateToFullPlayer = {
                        currentRoute = "full_player"
                    },
                    onNavigateToDetails = {
                        currentRoute = "details"
                    },
                    onExitApp = { finish() }
                )

                "details" -> MediaDetailsScreen(
                    viewModel = mainViewModel,
                    currentTheme = currentTheme,
                    onBack = { currentRoute = "main" },
                    onPlayStream = {
                        currentRoute = "full_player"
                    }
                )

                "full_player" -> FullPlayerScreen(
                    viewModel = mainViewModel,
                    onBack = { currentRoute = "main" }
                )
            }
        }
    }
}
