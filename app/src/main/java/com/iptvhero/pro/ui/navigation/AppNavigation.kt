package com.iptvhero.pro.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.iptvhero.pro.ui.screens.MainScreen
import com.iptvhero.pro.ui.screens.MediaDetailsScreen
import com.iptvhero.pro.ui.screens.PlayerScreen
import com.iptvhero.pro.ui.screens.ProfilesScreen
import com.iptvhero.pro.ui.screens.SplashScreen
import com.iptvhero.pro.ui.theme.HeroThemeType
import com.iptvhero.pro.ui.viewmodel.MainViewModel

@Composable
fun AppNavigation(
    viewModel: MainViewModel,
    onExitApp: () -> Unit = {}
) {
    var currentScreen by remember { mutableStateOf("splash") }
    var currentTheme by remember { mutableStateOf(HeroThemeType.DUEL) }
    val navigationHistory = remember { mutableStateListOf<String>() }

    var backPressCount by remember { mutableStateOf(0) }
    var lastBackTimestamp by remember { mutableLongStateOf(0L) }
    var exitHintText by remember { mutableStateOf<String?>(null) }

    val profiles by viewModel.profiles.collectAsState(initial = emptyList())
    val uiState by viewModel.uiState.collectAsState()
    val downloadStates by viewModel.downloadStates.collectAsState()

    fun navigateTo(targetScreen: String) {
        if (targetScreen != currentScreen) {
            navigationHistory.add(currentScreen)
            currentScreen = targetScreen
            backPressCount = 0
            exitHintText = null
        }
    }

    fun handleBackAction(): Boolean {
        if (currentScreen == "player") {
            val prev = if (navigationHistory.isNotEmpty()) navigationHistory.removeAt(navigationHistory.lastIndex) else "main"
            currentScreen = if (prev == "details") "details" else "main"
            backPressCount = 0
            exitHintText = null
            return true
        }
        if (currentScreen == "details") {
            currentScreen = "main"
            backPressCount = 0
            exitHintText = null
            return true
        }

        if (navigationHistory.isNotEmpty()) {
            val previous = navigationHistory.removeAt(navigationHistory.lastIndex)
            if (previous != "splash") {
                currentScreen = previous
                backPressCount = 0
                exitHintText = null
                return true
            }
        }

        val now = System.currentTimeMillis()
        if (now - lastBackTimestamp > 2500L) {
            backPressCount = 0
        }
        lastBackTimestamp = now
        backPressCount += 1

        if (backPressCount >= 3) {
            onExitApp()
            return true
        }

        val remaining = 3 - backPressCount
        exitHintText = "Pressione Voltar mais $remaining vez(es) para sair do aplicativo"
        return true
    }

    BackHandler(enabled = currentScreen != "splash") {
        handleBackAction()
    }

    fun cycleTheme() {
        val values = HeroThemeType.entries.toTypedArray()
        currentTheme = values[(currentTheme.ordinal + 1) % values.size]
    }

    val statusText = uiState.errorMessage ?: uiState.successMessage

    if (currentScreen == "splash" || uiState.isHeroSplashActive) {
        SplashScreen(
            statusText = uiState.heroSplashMessage.uppercase(),
            waitForHeroLoad = !viewModel.hasSavedHeroHash(),
            onStartInitialHeroLoad = {
                viewModel.loadHeroListFromGithub()
            },
            onFinished = {
                if (!uiState.isHeroSplashActive && currentScreen == "splash") {
                    currentScreen = "profiles"
                }
            }
        )
        return
    }

    when (currentScreen) {
        "profiles" -> {
            ProfilesScreen(
                profiles = profiles,
                activeProfileId = viewModel.activeProfile?.id,
                statusMessage = statusText,
                pendingHeroMessage = uiState.pendingHeroMessage,
                currentTheme = currentTheme,
                onCycleTheme = ::cycleTheme,
                onOpenImportTxt = { navigateTo("main") },
                onLoadHeroGithub = {
                    viewModel.loadHeroListFromGithub()
                },
                onConfirmPendingHero = {
                    viewModel.confirmPendingHeroList()
                },
                onDismissPendingHero = {
                    viewModel.dismissPendingHeroList()
                },
                onSelectProfile = { profileId ->
                    viewModel.selectProfile(profileId) {
                        navigateTo("main")
                    }
                },
                onDeleteProfile = { profileId ->
                    viewModel.deleteProfile(profileId)
                },
                onClearAllProfiles = { viewModel.clearAllProfiles() },
                onEnterApp = { navigateTo("main") }
            )
        }
        "main" -> {
            MainScreen(
                viewModel = viewModel,
                currentTheme = currentTheme,
                onCycleTheme = ::cycleTheme,
                onBackToProfiles = { navigateTo("profiles") },
                onNavigateToFullPlayer = { navigateTo("player") },
                onNavigateToDetails = { navigateTo("details") },
                exitHintText = exitHintText,
                onTriggerBack = { handleBackAction() },
                onExitApp = onExitApp
            )
        }
        "details" -> {
            val mergedDownloads = remember(downloadStates, uiState.selectedMediaDetails, uiState.selectedMovieDownloadKey) {
                val movieOffline = viewModel.getMovieOfflineState()
                if (movieOffline.isCompleted && uiState.selectedMovieDownloadKey.isNotBlank()) {
                    downloadStates + (uiState.selectedMovieDownloadKey to movieOffline)
                } else downloadStates
            }
            MediaDetailsScreen(
                details = uiState.selectedMediaDetails,
                isLoading = uiState.isLoadingMediaDetails,
                seasonsMap = uiState.seasonsMap,
                downloadStates = mergedDownloads,
                movieDownloadKey = uiState.selectedMovieDownloadKey,
                currentTheme = currentTheme,
                onWatchMovie = { streamUrl, title ->
                    viewModel.startPlaylistPlayback(
                        titles = listOf(title),
                        urls = listOf(streamUrl),
                        startIndex = 0,
                        tab = uiState.lastSelectedTab,
                        category = uiState.lastSelectedCategory,
                        search = uiState.lastSearchQuery
                    )
                    navigateTo("player")
                },
                onDownloadMovie = { viewModel.downloadSelectedMovie() },
                onDeleteMovieOffline = { viewModel.deleteSelectedMovieOffline() },
                onWatchEpisode = { episode, seasonKey, seasonEpisodes, idx ->
                    val offlineEp = viewModel.checkEpisodeOffline(episode)
                    val epUrl = offlineEp.localUri ?: viewModel.buildEpisodeStreamUrl(episode) ?: return@MediaDetailsScreen
                    val seriesName = uiState.selectedMediaDetails?.title ?: uiState.selectedSeriesTitle
                    val fullTitle = "$seriesName • T${seasonKey}E${episode.episodeNum} - ${episode.title}"
                    val titles = seasonEpisodes.map { "$seriesName • T${seasonKey}E${it.episodeNum} - ${it.title}" }
                    val urls = seasonEpisodes.mapNotNull { epItem ->
                        viewModel.checkEpisodeOffline(epItem).localUri ?: viewModel.buildEpisodeStreamUrl(epItem)
                    }
                    viewModel.startPlaylistPlayback(
                        titles = if (urls.size == titles.size) titles else listOf(fullTitle),
                        urls = if (urls.size == titles.size) urls else listOf(epUrl),
                        startIndex = if (idx in urls.indices) idx else 0,
                        tab = uiState.lastSelectedTab,
                        category = uiState.lastSelectedCategory,
                        search = uiState.lastSearchQuery
                    )
                    navigateTo("player")
                },
                onDownloadEpisode = { episode, _ -> viewModel.downloadEpisode(episode) },
                onDeleteEpisodeOffline = { episode -> viewModel.deleteEpisodeOffline(episode) },
                onCheckEpisodeOffline = { episode -> viewModel.checkEpisodeOffline(episode) },
                onBack = {
                    currentScreen = "main"
                }
            )
        }
        "player" -> {
            PlayerScreen(
                streamUrl = uiState.currentStreamUrl,
                channelTitle = uiState.currentChannelTitle,
                epgItems = uiState.currentEpgItems,
                isLoadingEpg = uiState.isLoadingEpg,
                onStepChannel = { delta -> viewModel.stepChannel(delta) },
                onBack = {
                    viewModel.setStreamUrl(null, "")
                    currentScreen = "main"
                }
            )
        }
    }
}
