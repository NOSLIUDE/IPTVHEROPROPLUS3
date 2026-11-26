package com.iptvhero.pro.ui.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.iptvhero.pro.data.local.ChannelEntity
import com.iptvhero.pro.data.local.ProfileEntity
import com.iptvhero.pro.data.repository.BulkProgress
import com.iptvhero.pro.data.repository.EpgProgramItem
import com.iptvhero.pro.data.repository.IptvRepository
import com.iptvhero.pro.data.download.OfflineDownloadManager
import com.iptvhero.pro.data.download.OfflineDownloadState
import com.iptvhero.pro.data.repository.MediaDetails
import com.iptvhero.pro.data.repository.SeriesEpisode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.URLEncoder

data class MainUiState(
    val currentStreamUrl: String? = null,
    val currentChannelTitle: String = "",
    val isLoading: Boolean = false,
    val loadingProgress: Float = 0f,
    val loadingStage: String = "",
    val profileLoadProgress: Float = 0f,
    val profileLoadStage: String = "",
    val isProfileLoading: Boolean = false,
    val bulkProgress: BulkProgress? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val pendingHeroUrls: List<String>? = null,
    val pendingHeroMessage: String? = null,
    val pendingHeroHash: String? = null,
    val seasonsMap: Map<String, List<SeriesEpisode>> = emptyMap(),
    val isLoadingSeasons: Boolean = false,
    val selectedSeriesTitle: String = "",
    val selectedMediaDetails: MediaDetails? = null,
    val isLoadingMediaDetails: Boolean = false,
    val selectedMovieDownloadKey: String = "",
    val lastSelectedTab: String = "CANAIS",
    val lastSelectedCategory: String = "",
    val lastSearchQuery: String = "",
    val lastFocusedIndex: Int = 0,
    val lastOpenedChannelTitle: String = "",
    val playbackPlaylistTitles: List<String> = emptyList(),
    val playbackPlaylistUrls: List<String> = emptyList(),
    val playbackPlaylistRemoteIds: List<String?> = emptyList(),
    val playbackIndex: Int = -1,
    val currentEpgItems: List<EpgProgramItem> = emptyList(),
    val isLoadingEpg: Boolean = false,
    // SplashScreen: ativa na 1ª inicialização e quando há atualização da Hero Lista até criar os perfis
    val showSplashScreen: Boolean = true,
    val splashStatusText: String = "Preparando Hero Lista...",
    val splashProgress: Float = 0.05f,
    val splashErrorMessage: String? = null
)

class MainViewModel(
    private val repository: IptvRepository,
    context: Context
) : ViewModel() {

    private val prefs = context.getSharedPreferences("iptv_hero_prefs", Context.MODE_PRIVATE)
    val downloadManager = OfflineDownloadManager(context.applicationContext)
    val downloadStates: StateFlow<Map<String, OfflineDownloadState>> = downloadManager.states

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    val profiles: Flow<List<ProfileEntity>> = repository.allProfiles

    var activeProfile by mutableStateOf<ProfileEntity?>(null)
        private set

    private val activeProfileIdFlow = MutableStateFlow<Long?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val channels: Flow<List<ChannelEntity>> = activeProfileIdFlow.flatMapLatest { profileId ->
        if (profileId != null) repository.getChannelsByProfile(profileId) else flowOf(emptyList())
    }

    init {
        val savedTab = prefs.getString("last_selected_tab", "CANAIS") ?: "CANAIS"
        val savedCat = prefs.getString("last_selected_category", "") ?: ""
        val savedSearch = prefs.getString("last_search_query", "") ?: ""
        val savedIndex = prefs.getInt("last_focused_index", 0)
        val savedTitle = prefs.getString("last_opened_channel_title", "") ?: ""
        _uiState.update {
            it.copy(
                lastSelectedTab = savedTab,
                lastSelectedCategory = savedCat,
                lastSearchQuery = savedSearch,
                lastFocusedIndex = savedIndex,
                lastOpenedChannelTitle = savedTitle
            )
        }
        runStartupHeroFlow()
    }


    fun runStartupHeroFlow() {
        viewModelScope.launch {
            val savedHash = prefs.getString("hero_list_hash", null)
            val existingProfiles = runCatching { repository.allProfiles.first() }.getOrDefault(emptyList())
            val isFirstLaunch = savedHash.isNullOrBlank() || existingProfiles.isEmpty()

            _uiState.update {
                it.copy(
                    showSplashScreen = true,
                    splashErrorMessage = null,
                    splashProgress = 0.10f,
                    splashStatusText = if (isFirstLaunch) {
                        "Primeira inicialização: carregando Hero Lista..."
                    } else {
                        "Verificando atualização da Hero Lista..."
                    }
                )
            }

            try {
                val (rawText, urls) = repository.fetchHeroListRaw(IptvRepository.HERO_LIST_URL)
                val newHash = repository.sha256Hex(rawText)
                val hasUpdate = savedHash != null && savedHash != newHash

                if (isFirstLaunch || hasUpdate) {
                    _uiState.update {
                        it.copy(
                            showSplashScreen = true,
                            splashProgress = 0.20f,
                            splashStatusText = if (isFirstLaunch) {
                                "Processando ${urls.size} listas e criando perfis..."
                            } else {
                                "Atualização da Hero Lista detectada! Processando ${urls.size} listas..."
                            }
                        )
                    }
                    processHeroAndCreateProfilesOnSplash(urls = urls, hash = newHash)
                } else {
                    val active = repository.getActiveProfile() ?: existingProfiles.firstOrNull()
                    activeProfile = active
                    activeProfileIdFlow.value = active?.id
                    _uiState.update {
                        it.copy(
                            showSplashScreen = false,
                            splashProgress = 1f,
                            splashErrorMessage = null
                        )
                    }
                }
            } catch (e: Exception) {
                val active = repository.getActiveProfile() ?: existingProfiles.firstOrNull()
                activeProfile = active
                activeProfileIdFlow.value = active?.id

                if (!isFirstLaunch && existingProfiles.isNotEmpty()) {
                    _uiState.update {
                        it.copy(
                            showSplashScreen = false,
                            splashErrorMessage = null,
                            errorMessage = "Não foi possível verificar atualização da Hero Lista agora: ${e.message ?: "Falha de rede"}"
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            showSplashScreen = true,
                            splashErrorMessage = "Falha ao carregar Hero Lista: ${e.message ?: "Verifique a conexão"}"
                        )
                    }
                }
            }
        }
    }

    private suspend fun processHeroAndCreateProfilesOnSplash(urls: List<String>, hash: String) {
        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessage = null,
                splashErrorMessage = null,
                showSplashScreen = true,
                splashProgress = 0.20f,
                splashStatusText = "Processando ${urls.size} listas e criando perfis..."
            )
        }

        val (ok, fail) = repository.processBulkLines(urls, isHeroOrigin = true) { prog ->
            val ratio = if (prog.total > 0) prog.current.toFloat() / prog.total.toFloat() else 0f
            val mappedProgress = (0.20f + (ratio * 0.78f)).coerceIn(0.20f, 0.98f)
            val status = "Criando perfis: ${prog.current}/${prog.total} (OK: ${prog.success} | Falhas: ${prog.failed})"
            _uiState.update {
                it.copy(
                    bulkProgress = prog,
                    splashProgress = mappedProgress,
                    splashStatusText = status,
                    successMessage = status
                )
            }
        }

        prefs.edit()
            .putString("hero_list_hash", hash)
            .putLong("hero_list_checked_at", System.currentTimeMillis())
            .apply()

        val createdProfiles = runCatching { repository.allProfiles.first() }.getOrDefault(emptyList())
        val active = repository.getActiveProfile() ?: createdProfiles.firstOrNull()
        activeProfile = active
        activeProfileIdFlow.value = active?.id

        _uiState.update {
            it.copy(
                isLoading = false,
                bulkProgress = null,
                showSplashScreen = false,
                splashProgress = 1f,
                splashStatusText = "Perfis criados!",
                splashErrorMessage = null,
                successMessage = "Hero Lista concluída! Perfis criados: $ok | Falhas: $fail"
            )
        }
    }

    fun dismissSplashIfPossible() {
        _uiState.update {
            it.copy(showSplashScreen = false, splashErrorMessage = null)
        }
    }

    fun saveNavigationHistory(
        tab: String,
        category: String,
        search: String,
        focusedIndex: Int,
        openedTitle: String? = null
    ) {
        val titleToSave = openedTitle ?: _uiState.value.lastOpenedChannelTitle
        prefs.edit()
            .putString("last_selected_tab", tab)
            .putString("last_selected_category", category)
            .putString("last_search_query", search)
            .putInt("last_focused_index", focusedIndex.coerceAtLeast(0))
            .putString("last_opened_channel_title", titleToSave)
            .apply()
        _uiState.update {
            it.copy(
                lastSelectedTab = tab,
                lastSelectedCategory = category,
                lastSearchQuery = search,
                lastFocusedIndex = focusedIndex.coerceAtLeast(0),
                lastOpenedChannelTitle = titleToSave
            )
        }
    }

    fun loadActiveProfile() {
        viewModelScope.launch {
            val active = repository.getActiveProfile()
            activeProfile = active
            activeProfileIdFlow.value = active?.id
        }
    }

    /**
     * Identico ao activateProfile(id) do index_2.html:
     * Ativa o perfil e busca os canais/filmes/series da fonte (Xtream ou M3U) se necessario.
     */
    fun selectProfile(profileId: Long, onReady: (() -> Unit)? = null) {
        _uiState.update {
            it.copy(
                isLoading = true,
                loadingProgress = 0.12f,
                loadingStage = "Conectando perfil e autenticando...",
                isProfileLoading = true,
                profileLoadProgress = 0.12f,
                profileLoadStage = "Conectando perfil e carregando canais, filmes e séries...",
                errorMessage = null,
                successMessage = "Conectando perfil..."
            )
        }
        viewModelScope.launch {
            try {
                val count = repository.activateAndSyncProfile(
                    profileId = profileId,
                    onStage = { progress, stageText ->
                        val clamped = progress.coerceIn(0.05f, 0.98f)
                        _uiState.update { st ->
                            st.copy(
                                isLoading = true,
                                loadingProgress = clamped,
                                loadingStage = stageText,
                                isProfileLoading = true,
                                profileLoadProgress = clamped,
                                profileLoadStage = stageText,
                                successMessage = stageText
                            )
                        }
                    }
                )
                val active = repository.getActiveProfile()
                activeProfile = active
                activeProfileIdFlow.value = active?.id
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadingProgress = 1f,
                        loadingStage = "Concluído ($count itens carregados)",
                        isProfileLoading = false,
                        profileLoadProgress = 1f,
                        profileLoadStage = "Concluído ($count itens carregados)",
                        successMessage = "Perfil ${active?.name ?: ""} ativado ($count itens)."
                    )
                }
                onReady?.invoke()
            } catch (e: Exception) {
                loadActiveProfile()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadingProgress = 0f,
                        loadingStage = "",
                        isProfileLoading = false,
                        profileLoadProgress = 0f,
                        profileLoadStage = "",
                        successMessage = null,
                        errorMessage = "Erro ao carregar perfil: ${e.message ?: "Falha"}"
                    )
                }
                onReady?.invoke()
            }
        }
    }

    fun deleteProfile(profileId: Long) {
        viewModelScope.launch {
            repository.deleteProfile(profileId)
            loadActiveProfile()
        }
    }

    fun clearAllProfiles() {
        viewModelScope.launch {
            repository.clearAllProfiles()
            loadActiveProfile()
            _uiState.update { it.copy(successMessage = "Todos os perfis removidos.", errorMessage = null) }
        }
    }

    /**
     * Identico ao loadHeroList() do index_2.html:
     * Baixa o arquivo all_in_one.txt com cache-bust, calcula SHA-256 e compara com a ultima versao.
     */
    fun loadHeroListFromGithub(url: String = IptvRepository.HERO_LIST_URL, force: Boolean = false) {
        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = "Verificando atualizacoes da Hero Lista..."
            )
        }
        viewModelScope.launch {
            try {
                val (rawText, urls) = repository.fetchHeroListRaw(url)
                val hash = repository.sha256Hex(rawText)
                val prevHash = prefs.getString("hero_list_hash", null)

                if (!force && prevHash != null && prevHash == hash) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            pendingHeroUrls = urls,
                            pendingHeroHash = hash,
                            pendingHeroMessage = "Hero Lista sem alteracoes (${urls.size} listas). Deseja reprocessar mesmo assim?"
                        )
                    }
                    return@launch
                }

                executeHeroBulk(urls, hash)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = null,
                        errorMessage = "Falha Hero Lista: ${e.message ?: e.javaClass.simpleName}"
                    )
                }
            }
        }
    }

    fun confirmPendingHeroList() {
        val urls = _uiState.value.pendingHeroUrls ?: return
        val hash = _uiState.value.pendingHeroHash.orEmpty()
        _uiState.update { it.copy(pendingHeroUrls = null, pendingHeroMessage = null) }
        viewModelScope.launch {
            processHeroAndCreateProfilesOnSplash(urls = urls, hash = hash)
        }
    }

    fun dismissPendingHeroList() {
        _uiState.update { it.copy(pendingHeroUrls = null, pendingHeroMessage = null, isLoading = false, showSplashScreen = false) }
    }

    private suspend fun executeHeroBulk(urls: List<String>, hash: String) {
        processHeroAndCreateProfilesOnSplash(urls = urls, hash = hash)
    }

    /**
     * Identico ao loadBulkTXT / processBulk do index_2.html:
     * Cada linha vira um perfil de heroi individual.
     */
    fun importBatchTxt(fileContent: String) {
        val lines = fileContent.split(Regex("\r?\n"))
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }

        if (lines.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Nenhuma linha valida encontrada.") }
            return
        }

        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = "Processando ${lines.size} entradas..."
            )
        }
        viewModelScope.launch {
            try {
                val (ok, fail) = repository.processBulkLines(lines, isHeroOrigin = true) { prog ->
                    _uiState.update {
                        it.copy(
                            bulkProgress = prog,
                            successMessage = "Lote: ${prog.current}/${prog.total} (OK: ${prog.success})"
                        )
                    }
                }
                val active = repository.getActiveProfile()
                activeProfile = active
                activeProfileIdFlow.value = active?.id
                if (active != null) {
                    runCatching { repository.activateAndSyncProfile(active.id) }
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        bulkProgress = null,
                        successMessage = "Lote concluido! $ok perfis criados ($fail falhas)."
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        bulkProgress = null,
                        errorMessage = "Erro ao importar: ${e.message ?: "Falha"}"
                    )
                }
            }
        }
    }

    fun importXtream(server: String, user: String, pass: String) {
        _uiState.update {
            it.copy(
                isLoading = true,
                loadingProgress = 0.2f,
                loadingStage = "Autenticando Xtream e buscando canais, filmes e séries...",
                isProfileLoading = true,
                profileLoadProgress = 0.2f,
                profileLoadStage = "Autenticando Xtream e buscando canais, filmes e séries...",
                errorMessage = null,
                successMessage = "Autenticando Xtream..."
            )
        }
        viewModelScope.launch {
            try {
                _uiState.update {
                    it.copy(
                        loadingProgress = 0.55f,
                        loadingStage = "Baixando canais, filmes e séries...",
                        profileLoadProgress = 0.55f,
                        profileLoadStage = "Baixando canais, filmes e séries..."
                    )
                }
                val total = repository.loadXtreamData(server, user, pass)
                val active = repository.getActiveProfile()
                activeProfile = active
                activeProfileIdFlow.value = active?.id
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadingProgress = 1f,
                        loadingStage = "Concluído ($total itens)",
                        isProfileLoading = false,
                        profileLoadProgress = 1f,
                        profileLoadStage = "Concluído ($total itens)",
                        successMessage = "Xtream conectado (${active?.name})! $total itens."
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadingProgress = 0f,
                        loadingStage = "",
                        isProfileLoading = false,
                        profileLoadProgress = 0f,
                        profileLoadStage = "",
                        successMessage = null,
                        errorMessage = "Erro Xtream: ${e.message ?: e.javaClass.simpleName}"
                    )
                }
            }
        }
    }

    fun openSeriesSeasons(seriesChannel: ChannelEntity, allSeriesItems: List<ChannelEntity> = emptyList()) {
        val profile = activeProfile ?: return
        val seriesId = seriesChannel.remoteId
        _uiState.update {
            it.copy(
                isLoadingSeasons = true,
                selectedSeriesTitle = seriesChannel.title,
                seasonsMap = emptyMap(),
                errorMessage = null
            )
        }
        viewModelScope.launch {
            try {
                if (!seriesId.isNullOrBlank() && !profile.server.isNullOrBlank()) {
                    val seasons = repository.fetchSeriesSeasons(profile, seriesId)
                    if (seasons.isNotEmpty()) {
                        _uiState.update {
                            it.copy(
                                isLoadingSeasons = false,
                                seasonsMap = seasons
                            )
                        }
                        return@launch
                    }
                }

                // Fallback para listas M3U ou séries já listadas por episódio (ex: "Nome S01E02")
                val baseName = extractSeriesBaseTitle(seriesChannel.title)
                val matching = allSeriesItems.filter {
                    extractSeriesBaseTitle(it.title).equals(baseName, ignoreCase = true)
                }.ifEmpty { listOf(seriesChannel) }

                val grouped = linkedMapOf<String, MutableList<SeriesEpisode>>()
                val seRegex = Regex("(?i)(?:S|T)(\\d{1,2})\\s*(?:E|EP)?(\\d{1,3})")
                matching.forEachIndexed { idx, item ->
                    val m = seRegex.find(item.title)
                    val seasonKey = m?.groupValues?.getOrNull(1)?.toIntOrNull()?.toString() ?: "1"
                    val epNum = m?.groupValues?.getOrNull(2)?.toIntOrNull() ?: (idx + 1)
                    val ext = item.url.substringAfterLast('.', "mp4").substringBefore('?').takeIf { it.length in 2..5 } ?: "mp4"
                    grouped.getOrPut(seasonKey) { mutableListOf() }.add(
                        SeriesEpisode(
                            id = "DIRECT_URL::${item.url}",
                            episodeNum = epNum,
                            title = item.title,
                            extension = ext,
                            duration = null
                        )
                    )
                }

                val sortedSeasons = grouped.mapValues { (_, list) -> list.sortedBy { it.episodeNum } }
                _uiState.update {
                    it.copy(
                        isLoadingSeasons = false,
                        seasonsMap = sortedSeasons
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingSeasons = false,
                        errorMessage = "Erro ao carregar episodios: ${e.message ?: "Falha"}"
                    )
                }
            }
        }
    }

    private fun extractSeriesBaseTitle(rawTitle: String): String {
        return rawTitle
            .replace(Regex("(?i)\\s*[-–:]?\\s*(?:S|T)\\d{1,2}\\s*(?:E|EP)?\\d{1,3}.*$"), "")
            .replace(Regex("(?i)\\s*Temporada\\s*\\d+.*$"), "")
            .trim()
            .ifBlank { rawTitle.trim() }
    }


    fun openMediaDetails(channel: ChannelEntity, allSeriesItems: List<ChannelEntity> = emptyList()) {
        val isSeries = channel.contentType.equals("SERIES", ignoreCase = true)
        val dlKey = "vod_${channel.remoteId ?: channel.id}"
        _uiState.update {
            it.copy(
                isLoadingMediaDetails = true,
                selectedMediaDetails = null,
                selectedMovieDownloadKey = dlKey,
                seasonsMap = emptyMap(),
                selectedSeriesTitle = if (isSeries) channel.title else "",
                errorMessage = null
            )
        }
        viewModelScope.launch {
            val profile = activeProfile
            if (isSeries) {
                val (details, seasons) = if (profile != null && !channel.remoteId.isNullOrBlank() && !profile.server.isNullOrBlank()) {
                    runCatching { repository.fetchSeriesDetailsAndSeasons(profile, channel) }
                        .getOrElse {
                            MediaDetails(
                                title = channel.title,
                                plot = "Sinopse não disponível.",
                                releaseDate = "—",
                                year = "—",
                                ratingImdb = "N/A",
                                duration = "—",
                                genres = listOf(channel.groupName.ifBlank { "Geral" }),
                                posterUrl = channel.streamIcon,
                                backdropUrl = channel.streamIcon,
                                streamUrl = null,
                                contentType = "SERIES"
                            ) to emptyMap()
                        }
                } else {
                    MediaDetails(
                        title = channel.title,
                        plot = "Sinopse não disponível para listas M3U simples.",
                        releaseDate = "—",
                        year = "—",
                        ratingImdb = "N/A",
                        duration = "—",
                        genres = listOf(channel.groupName.ifBlank { "Geral" }),
                        posterUrl = channel.streamIcon,
                        backdropUrl = channel.streamIcon,
                        streamUrl = null,
                        contentType = "SERIES"
                    ) to emptyMap()
                }

                val resolvedSeasons = if (seasons.isNotEmpty()) seasons else buildFallbackSeasons(channel, allSeriesItems)
                _uiState.update {
                    it.copy(
                        isLoadingMediaDetails = false,
                        selectedMediaDetails = details,
                        seasonsMap = resolvedSeasons
                    )
                }
            } else {
                val details = if (profile != null) {
                    runCatching { repository.fetchVodDetails(profile, channel) }
                        .getOrElse {
                            MediaDetails(
                                title = channel.title,
                                plot = "Sinopse não disponível.",
                                releaseDate = "—",
                                year = "—",
                                ratingImdb = "N/A",
                                duration = "—",
                                genres = listOf(channel.groupName.ifBlank { "Geral" }),
                                posterUrl = channel.streamIcon,
                                backdropUrl = channel.streamIcon,
                                streamUrl = channel.url,
                                contentType = "VOD"
                            )
                        }
                } else {
                    MediaDetails(
                        title = channel.title,
                        plot = "Sinopse não disponível.",
                        releaseDate = "—",
                        year = "—",
                        ratingImdb = "N/A",
                        duration = "—",
                        genres = listOf(channel.groupName.ifBlank { "Geral" }),
                        posterUrl = channel.streamIcon,
                        backdropUrl = channel.streamIcon,
                        streamUrl = channel.url,
                        contentType = "VOD"
                    )
                }

                val existing = downloadManager.checkExistingFile(dlKey, details.title, details.extension)
                if (existing.isCompleted) {
                    // Refresh state map if file exists on disk
                    downloadManager.checkExistingFile(dlKey, details.title, details.extension)
                }

                _uiState.update {
                    it.copy(
                        isLoadingMediaDetails = false,
                        selectedMediaDetails = details
                    )
                }
            }
        }
    }

    private fun buildFallbackSeasons(
        seriesChannel: ChannelEntity,
        allSeriesItems: List<ChannelEntity>
    ): Map<String, List<SeriesEpisode>> {
        val baseName = extractSeriesBaseTitle(seriesChannel.title)
        val matching = allSeriesItems.filter {
            extractSeriesBaseTitle(it.title).equals(baseName, ignoreCase = true)
        }.ifEmpty { listOf(seriesChannel) }

        val grouped = linkedMapOf<String, MutableList<SeriesEpisode>>()
        val seRegex = Regex("(?i)(?:S|T)(\\d{1,2})\\s*(?:E|EP)?(\\d{1,3})")
        matching.forEachIndexed { idx, item ->
            val m = seRegex.find(item.title)
            val seasonKey = m?.groupValues?.getOrNull(1)?.toIntOrNull()?.toString() ?: "1"
            val epNum = m?.groupValues?.getOrNull(2)?.toIntOrNull() ?: (idx + 1)
            val ext = item.url.substringAfterLast('.', "mp4").substringBefore('?').takeIf { it.length in 2..5 } ?: "mp4"
            grouped.getOrPut(seasonKey) { mutableListOf() }.add(
                SeriesEpisode(
                    id = "DIRECT_URL::${item.url}",
                    episodeNum = epNum,
                    title = item.title,
                    extension = ext,
                    duration = null
                )
            )
        }
        return grouped.mapValues { (_, list) -> list.sortedBy { it.episodeNum } }
    }

    fun downloadSelectedMovie() {
        val details = _uiState.value.selectedMediaDetails ?: return
        val key = _uiState.value.selectedMovieDownloadKey
        val url = details.streamUrl ?: return
        viewModelScope.launch {
            downloadManager.downloadMedia(key = key, title = details.title, url = url, extension = details.extension)
        }
    }

    fun deleteSelectedMovieOffline() {
        val details = _uiState.value.selectedMediaDetails ?: return
        val key = _uiState.value.selectedMovieDownloadKey
        downloadManager.deleteOfflineFile(key = key, title = details.title, extension = details.extension)
    }

    fun getMovieOfflineState(): OfflineDownloadState {
        val details = _uiState.value.selectedMediaDetails ?: return OfflineDownloadState()
        val key = _uiState.value.selectedMovieDownloadKey
        return downloadStates.value[key] ?: downloadManager.checkExistingFile(key, details.title, details.extension)
    }

    fun downloadEpisode(episode: SeriesEpisode) {
        val url = buildEpisodeStreamUrl(episode) ?: return
        val key = "ep_${episode.id}"
        viewModelScope.launch {
            downloadManager.downloadMedia(key = key, title = episode.title, url = url, extension = episode.extension)
        }
    }

    fun deleteEpisodeOffline(episode: SeriesEpisode) {
        downloadManager.deleteOfflineFile(key = "ep_${episode.id}", title = episode.title, extension = episode.extension)
    }

    fun checkEpisodeOffline(episode: SeriesEpisode): OfflineDownloadState {
        val key = "ep_${episode.id}"
        return downloadStates.value[key] ?: downloadManager.checkExistingFile(key, episode.title, episode.extension)
    }

    fun closeSeriesSeasons() {
        _uiState.update { it.copy(seasonsMap = emptyMap(), selectedSeriesTitle = "", isLoadingSeasons = false) }
    }

    fun buildEpisodeStreamUrl(episode: SeriesEpisode): String? {
        if (episode.id.startsWith("DIRECT_URL::")) {
            return episode.id.removePrefix("DIRECT_URL::")
        }
        val profile = activeProfile ?: return null
        val cleanServer = profile.server?.trimEnd('/') ?: return null
        val encUser = URLEncoder.encode(profile.user.orEmpty(), "UTF-8")
        val encPass = URLEncoder.encode(profile.pass.orEmpty(), "UTF-8")
        return "$cleanServer/series/$encUser/$encPass/${episode.id}.${episode.extension}"
    }

    fun exportProfilesText(currentProfiles: List<ProfileEntity>): String {
        return repository.formatProfilesExportTxt(currentProfiles)
    }

    fun setStreamUrl(url: String?, title: String = "") {
        _uiState.update {
            it.copy(
                currentStreamUrl = url,
                currentChannelTitle = title,
                playbackIndex = if (url.isNullOrBlank()) -1 else it.playbackIndex
            )
        }
    }

    fun startPlaylistPlayback(
        titles: List<String>,
        urls: List<String>,
        remoteIds: List<String?> = emptyList(),
        startIndex: Int,
        tab: String,
        category: String,
        search: String
    ) {
        if (urls.isEmpty() || startIndex !in urls.indices) return
        val targetUrl = urls[startIndex]
        val targetTitle = titles.getOrElse(startIndex) { "Canal ${startIndex + 1}" }
        val targetRemoteId = remoteIds.getOrNull(startIndex)
        saveNavigationHistory(
            tab = tab,
            category = category,
            search = search,
            focusedIndex = startIndex,
            openedTitle = targetTitle
        )
        _uiState.update {
            it.copy(
                currentStreamUrl = targetUrl,
                currentChannelTitle = targetTitle,
                playbackPlaylistTitles = titles,
                playbackPlaylistUrls = urls,
                playbackPlaylistRemoteIds = remoteIds,
                playbackIndex = startIndex,
                currentEpgItems = emptyList()
            )
        }
        refreshEpgForCurrentStream(targetUrl, targetRemoteId, targetTitle)
    }

    fun stepChannel(delta: Int): Boolean {
        val urls = _uiState.value.playbackPlaylistUrls
        val titles = _uiState.value.playbackPlaylistTitles
        val remoteIds = _uiState.value.playbackPlaylistRemoteIds
        val curIdx = _uiState.value.playbackIndex
        if (urls.isEmpty() || curIdx !in urls.indices) return false
        val nextIdx = (curIdx + delta + urls.size) % urls.size
        val nextUrl = urls[nextIdx]
        val nextTitle = titles.getOrElse(nextIdx) { "Canal ${nextIdx + 1}" }
        val nextRemoteId = remoteIds.getOrNull(nextIdx)
        saveNavigationHistory(
            tab = _uiState.value.lastSelectedTab,
            category = _uiState.value.lastSelectedCategory,
            search = _uiState.value.lastSearchQuery,
            focusedIndex = nextIdx,
            openedTitle = nextTitle
        )
        _uiState.update {
            it.copy(
                currentStreamUrl = nextUrl,
                currentChannelTitle = nextTitle,
                playbackIndex = nextIdx,
                currentEpgItems = emptyList()
            )
        }
        refreshEpgForCurrentStream(nextUrl, nextRemoteId, nextTitle)
        return true
    }

    fun refreshEpgForCurrentStream(
        streamUrl: String?,
        remoteId: String? = null,
        channelTitle: String = ""
    ) {
        if (streamUrl.isNullOrBlank()) {
            _uiState.update { it.copy(currentEpgItems = emptyList(), isLoadingEpg = false) }
            return
        }
        _uiState.update { it.copy(isLoadingEpg = true) }
        viewModelScope.launch {
            val items = repository.fetchShortEpg(activeProfile, streamUrl, remoteId, channelTitle)
            _uiState.update {
                it.copy(
                    currentEpgItems = items,
                    isLoadingEpg = false
                )
            }
        }
    }
}

class MainViewModelFactory(
    private val repository: IptvRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(repository, context.applicationContext) as T
        }
        throw IllegalArgumentException("Classe ViewModel desconhecida: ${modelClass.name}")
    }
}
