package com.iptvhero.pro.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iptvhero.pro.data.local.AppDatabase
import com.iptvhero.pro.data.local.ChannelEntity
import com.iptvhero.pro.data.local.ProfileEntity
import com.iptvhero.pro.data.repository.PlaylistParser
import com.iptvhero.pro.data.repository.SeriesEpisode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MainUiState(
    val isProfilesLoading: Boolean = true,
    val navigateToMainScreen: Boolean = false,
    val isLoading: Boolean = false,
    val loadingProgress: Float = 0f,
    val loadingStage: String = "",
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val pendingHeroMessage: String? = null,
    val currentStreamUrl: String? = null,
    val currentChannelTitle: String = "",
    val playlistTitles: List<String> = emptyList(),
    val playlistUrls: List<String> = emptyList(),
    val playlistRemoteIds: List<String?> = emptyList(),
    val playlistCurrentIndex: Int = 0,
    val lastSelectedTab: String = "CANAIS",
    val lastSelectedCategory: String = "",
    val lastSearchQuery: String = "",
    val lastFocusedIndex: Int = 0,
    val lastOpenedChannelTitle: String = "",
    val selectedMediaChannel: ChannelEntity? = null,
    val relatedChannels: List<ChannelEntity> = emptyList(),
    val selectedSeriesTitle: String = "",
    val isLoadingSeasons: Boolean = false,
    val seasonsMap: Map<String, List<SeriesEpisode>> = emptyMap()
)

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val profileDao = db.profileDao()
    private val channelDao = db.channelDao()

    val profiles: Flow<List<ProfileEntity>> = profileDao.observeAll()

    private val _activeProfileId = MutableStateFlow<Long?>(null)
    var activeProfile by mutableStateOf<ProfileEntity?>(null)
        private set

    val channels: Flow<List<ChannelEntity>> = _activeProfileId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else channelDao.observeByProfile(id)
    }

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var pendingHeroUrl: String = DEFAULT_HERO_URL

    init {
        initializeProfiles()
    }

    private fun initializeProfiles() {
        viewModelScope.launch {
            _uiState.update { it.copy(isProfilesLoading = true) }
            withContext(Dispatchers.IO) {
                val existing = profileDao.getAll()
                if (existing.isEmpty()) {
                    profileDao.insert(
                        ProfileEntity(
                            name = "Herói Marvel & DC",
                            heroAvatar = "IRONMAN",
                            sourceType = "GITHUB_HERO",
                            urlOrServer = DEFAULT_HERO_URL
                        )
                    )
                    profileDao.insert(
                        ProfileEntity(
                            name = "Cavaleiro das Trevas",
                            heroAvatar = "BATMAN",
                            sourceType = "GITHUB_HERO",
                            urlOrServer = DEFAULT_HERO_URL
                        )
                    )
                }
            }
            _uiState.update { it.copy(isProfilesLoading = false) }
        }
    }

    fun consumeNavigateToMainScreen() {
        _uiState.update { it.copy(navigateToMainScreen = false) }
    }

    fun selectProfile(profileId: Long) {
        selectProfileInternal(profileId, enterMainAfter = false)
    }

    fun selectProfileAndEnterApp(profileId: Long) {
        selectProfileInternal(profileId, enterMainAfter = true)
    }

    private fun selectProfileInternal(profileId: Long, enterMainAfter: Boolean) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    loadingProgress = 0.25f,
                    loadingStage = "Carregando perfil selecionado...",
                    errorMessage = null,
                    navigateToMainScreen = false
                )
            }
            val profile = withContext(Dispatchers.IO) { profileDao.getById(profileId) }
            if (profile != null) {
                activeProfile = profile
                _activeProfileId.value = profile.id

                if (profile.urlOrServer.isNotBlank()) {
                    _uiState.update {
                        it.copy(
                            loadingProgress = 0.65f,
                            loadingStage = "Sincronizando canais de ${profile.name}..."
                        )
                    }
                    runCatching {
                        withContext(Dispatchers.IO) {
                            val parsed = if (profile.sourceType == "XTREAM") {
                                PlaylistParser.fetchXtreamAll(
                                    profile.urlOrServer,
                                    profile.username,
                                    profile.password,
                                    profile.id
                                )
                            } else {
                                val text = PlaylistParser.fetchText(profile.urlOrServer)
                                PlaylistParser.parseM3uOrTxt(text, profile.id)
                            }
                            if (parsed.isNotEmpty()) {
                                channelDao.deleteByProfile(profile.id)
                                channelDao.insertAll(parsed)
                            }
                        }
                    }
                }
            }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    loadingProgress = 1f,
                    loadingStage = "",
                    navigateToMainScreen = enterMainAfter && profile != null
                )
            }
        }
    }

    fun deleteProfile(profileId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            channelDao.deleteByProfile(profileId)
            profileDao.deleteById(profileId)
            if (activeProfile?.id == profileId) {
                activeProfile = null
                _activeProfileId.value = null
            }
        }
    }

    fun loadHeroListFromGithub(url: String = DEFAULT_HERO_URL, force: Boolean = false) {
        loadHeroInternal(url = url, force = force, enterMainAfter = false)
    }

    fun loadHeroListAndEnterApp(url: String = DEFAULT_HERO_URL, force: Boolean = true) {
        loadHeroInternal(url = url, force = force, enterMainAfter = true)
    }

    private fun loadHeroInternal(url: String, force: Boolean, enterMainAfter: Boolean) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    loadingProgress = 0.2f,
                    loadingStage = "Baixando lista Hero...",
                    errorMessage = null,
                    navigateToMainScreen = false
                )
            }
            try {
                val targetProfile = withContext(Dispatchers.IO) {
                    val current = activeProfile ?: profileDao.getAll().firstOrNull()
                    if (current != null) {
                        val updated = current.copy(urlOrServer = url, sourceType = "GITHUB_HERO")
                        profileDao.insert(updated)
                        updated
                    } else {
                        val id = profileDao.insert(
                            ProfileEntity(
                                name = "Hero Lista",
                                heroAvatar = "MARVEL",
                                sourceType = "GITHUB_HERO",
                                urlOrServer = url
                            )
                        )
                        profileDao.getById(id)!!
                    }
                }
                activeProfile = targetProfile
                _activeProfileId.value = targetProfile.id

                _uiState.update { it.copy(loadingProgress = 0.6f, loadingStage = "Processando canais, filmes e séries...") }

                val items = withContext(Dispatchers.IO) {
                    val raw = PlaylistParser.fetchText(url)
                    val parsed = PlaylistParser.parseM3uOrTxt(raw, targetProfile.id)
                    channelDao.deleteByProfile(targetProfile.id)
                    channelDao.insertAll(parsed)
                    parsed
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadingProgress = 1f,
                        loadingStage = "",
                        successMessage = "${items.size} itens carregados!",
                        navigateToMainScreen = enterMainAfter
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadingProgress = 0f,
                        loadingStage = "",
                        errorMessage = "Falha ao carregar lista: ${e.message ?: "Erro de rede"}",
                        navigateToMainScreen = enterMainAfter
                    )
                }
            }
        }
    }

    fun importXtream(server: String, user: String, pass: String) {
        importXtreamInternal(server, user, pass, enterMainAfter = false)
    }

    fun importXtreamAndEnterApp(server: String, user: String, pass: String) {
        importXtreamInternal(server, user, pass, enterMainAfter = true)
    }

    private fun importXtreamInternal(server: String, user: String, pass: String, enterMainAfter: Boolean) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    loadingProgress = 0.25f,
                    loadingStage = "Autenticando servidor Xtream...",
                    errorMessage = null,
                    navigateToMainScreen = false
                )
            }
            try {
                val targetProfile = withContext(Dispatchers.IO) {
                    val id = profileDao.insert(
                        ProfileEntity(
                            name = "Xtream • ${user.trim()}",
                            heroAvatar = "DC",
                            sourceType = "XTREAM",
                            urlOrServer = server.trim(),
                            username = user.trim(),
                            password = pass.trim()
                        )
                    )
                    profileDao.getById(id)!!
                }
                activeProfile = targetProfile
                _activeProfileId.value = targetProfile.id

                _uiState.update { it.copy(loadingProgress = 0.65f, loadingStage = "Importando Canais, VOD e Séries...") }

                val items = withContext(Dispatchers.IO) {
                    val parsed = PlaylistParser.fetchXtreamAll(server, user, pass, targetProfile.id)
                    channelDao.deleteByProfile(targetProfile.id)
                    channelDao.insertAll(parsed)
                    parsed
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadingProgress = 1f,
                        loadingStage = "",
                        successMessage = "${items.size} itens Xtream importados!",
                        navigateToMainScreen = enterMainAfter
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Erro Xtream: ${e.message}",
                        navigateToMainScreen = enterMainAfter
                    )
                }
            }
        }
    }

    fun importBatchTxt(txtContent: String) {
        importBatchTxtInternal(txtContent, enterMainAfter = false)
    }

    fun importBatchTxtAndEnterApp(txtContent: String) {
        importBatchTxtInternal(txtContent, enterMainAfter = true)
    }

    private fun importBatchTxtInternal(txtContent: String, enterMainAfter: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadingProgress = 0.4f, loadingStage = "Importando lista TXT...") }
            val targetProfile = withContext(Dispatchers.IO) {
                val current = activeProfile ?: run {
                    val id = profileDao.insert(
                        ProfileEntity(name = "Lista Personalizada", heroAvatar = "FLASH", sourceType = "TXT")
                    )
                    profileDao.getById(id)!!
                }
                val parsed = PlaylistParser.parseM3uOrTxt(txtContent, current.id)
                channelDao.deleteByProfile(current.id)
                channelDao.insertAll(parsed)
                current
            }
            activeProfile = targetProfile
            _activeProfileId.value = targetProfile.id
            _uiState.update {
                it.copy(
                    isLoading = false,
                    loadingProgress = 1f,
                    loadingStage = "",
                    navigateToMainScreen = enterMainAfter
                )
            }
        }
    }

    fun confirmPendingHeroList() {
        val url = pendingHeroUrl
        _uiState.update { it.copy(pendingHeroMessage = null) }
        loadHeroListFromGithub(url, force = true)
    }

    fun dismissPendingHeroList() {
        _uiState.update { it.copy(pendingHeroMessage = null) }
    }

    fun setStreamUrl(url: String?, title: String) {
        _uiState.update {
            it.copy(
                currentStreamUrl = url,
                currentChannelTitle = title
            )
        }
    }

    fun startPlaylistPlayback(
        titles: List<String>,
        urls: List<String>,
        remoteIds: List<String?> = emptyList(),
        startIndex: Int = 0,
        tab: String = "CANAIS",
        category: String = "",
        search: String = ""
    ) {
        val safeIndex = startIndex.coerceIn(0, (urls.size - 1).coerceAtLeast(0))
        _uiState.update {
            it.copy(
                playlistTitles = titles,
                playlistUrls = urls,
                playlistRemoteIds = remoteIds,
                playlistCurrentIndex = safeIndex,
                currentStreamUrl = urls.getOrNull(safeIndex),
                currentChannelTitle = titles.getOrNull(safeIndex).orEmpty(),
                lastSelectedTab = tab,
                lastSelectedCategory = category,
                lastSearchQuery = search
            )
        }
    }

    fun switchPlaylistIndex(newIndex: Int) {
        val urls = _uiState.value.playlistUrls
        val titles = _uiState.value.playlistTitles
        if (newIndex in urls.indices) {
            _uiState.update {
                it.copy(
                    playlistCurrentIndex = newIndex,
                    currentStreamUrl = urls[newIndex],
                    currentChannelTitle = titles.getOrNull(newIndex).orEmpty()
                )
            }
        }
    }

    fun saveNavigationHistory(
        tab: String,
        category: String,
        search: String,
        focusedIndex: Int,
        openedTitle: String = _uiState.value.lastOpenedChannelTitle
    ) {
        _uiState.update {
            it.copy(
                lastSelectedTab = tab,
                lastSelectedCategory = category,
                lastSearchQuery = search,
                lastFocusedIndex = focusedIndex,
                lastOpenedChannelTitle = openedTitle
            )
        }
    }

    fun openMediaDetails(channel: ChannelEntity, allTabChannels: List<ChannelEntity>) {
        val related = allTabChannels
            .filter { it.id != channel.id && it.groupName.equals(channel.groupName, ignoreCase = true) }
            .take(12)
        _uiState.update {
            it.copy(
                selectedMediaChannel = channel,
                relatedChannels = related
            )
        }
        if (channel.contentType.equals("SERIES", ignoreCase = true)) {
            loadSeriesSeasons(channel)
        } else {
            _uiState.update { it.copy(selectedSeriesTitle = "", seasonsMap = emptyMap()) }
        }
    }

    fun loadSeriesSeasons(channel: ChannelEntity) {
        val profile = activeProfile
        val seriesId = channel.remoteId
        if (profile == null || profile.sourceType != "XTREAM" || seriesId.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    selectedSeriesTitle = channel.title,
                    isLoadingSeasons = false,
                    seasonsMap = emptyMap(),
                    errorMessage = "Temporadas disponíveis apenas para contas Xtream."
                )
            }
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    selectedSeriesTitle = channel.title,
                    isLoadingSeasons = true,
                    seasonsMap = emptyMap(),
                    errorMessage = null
                )
            }
            val map = withContext(Dispatchers.IO) {
                runCatching {
                    PlaylistParser.fetchSeriesSeasons(
                        profile.urlOrServer,
                        profile.username,
                        profile.password,
                        seriesId
                    )
                }.getOrDefault(emptyMap())
            }
            _uiState.update {
                it.copy(
                    isLoadingSeasons = false,
                    seasonsMap = map
                )
            }
        }
    }

    fun closeSeriesSeasons() {
        _uiState.update {
            it.copy(
                selectedSeriesTitle = "",
                isLoadingSeasons = false,
                seasonsMap = emptyMap()
            )
        }
    }

    fun buildEpisodeStreamUrl(episode: SeriesEpisode): String? {
        val profile = activeProfile ?: return null
        if (profile.urlOrServer.isBlank() || episode.id.isBlank()) return null
        val clean = profile.urlOrServer.trim().trimEnd('/')
        return "$clean/series/${profile.username}/${profile.password}/${episode.id}.${episode.containerExtension}"
    }

    companion object {
        const val DEFAULT_HERO_URL = "https://raw.githubusercontent.com/NOSLIUDE/IPTVHeroProTV/main/lista.m3u"
    }
}
