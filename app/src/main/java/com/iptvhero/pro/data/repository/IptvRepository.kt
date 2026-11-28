package com.iptvhero.pro.data.repository

import com.iptvhero.pro.data.local.ChannelDao
import com.iptvhero.pro.data.local.ChannelEntity
import com.iptvhero.pro.data.local.ProfileDao
import com.iptvhero.pro.data.local.ProfileEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs

data class PlaylistItem(
    val title: String,
    val url: String,
    val groupName: String = "Geral",
    val contentType: String = "LIVE",
    val streamIcon: String? = null,
    val remoteId: String? = null
)

data class XtreamCredentials(
    val server: String,
    val username: String,
    val password: String
)

data class HeroIdentity(
    val name: String,
    val universe: String,
    val avatarKey: String
)

data class BulkProgress(
    val current: Int,
    val total: Int,
    val success: Int,
    val failed: Int,
    val lastLog: String
)

data class EpgProgramItem(
    val title: String,
    val startTime: String = "",
    val endTime: String = "",
    val isCurrent: Boolean = false
)

data class SeriesEpisode(
    val id: String,
    val episodeNum: Int,
    val title: String,
    val extension: String,
    val duration: String?
)

data class MediaDetails(
    val title: String,
    val plot: String,
    val releaseDate: String,
    val year: String,
    val ratingImdb: String,
    val duration: String,
    val genres: List<String>,
    val posterUrl: String?,
    val backdropUrl: String?,
    val streamUrl: String?,
    val extension: String = "mp4",
    val contentType: String = "VOD"
)

enum class StrictCardUniverse(val value: String) {
    MARVEL("marvel"),
    DC("dc");

    companion object {
        fun fromRaw(raw: String?): StrictCardUniverse? = when (raw?.trim()?.lowercase()) {
            "marvel" -> MARVEL
            "dc", "dc comics", "dc_comics" -> DC
            else -> null
        }
    }
}

data class StrictCardHero(
    val key: String,
    val baseTag: String,
    val displayName: String,
    val assetFileName: String,
    val universe: StrictCardUniverse,
    val aliases: List<String> = emptyList()
)

object CardProfileHelper {
    private val sequenceCounter = AtomicInteger(1)

    val all16: List<StrictCardHero> = listOf(
        // DC (8)
        StrictCardHero("batman", "BATMAN", "Batman", "01_batman_0801.png", StrictCardUniverse.DC, listOf("batman_0801", "morcego")),
        StrictCardHero("superman", "SUPERMAN", "Superman", "02_superman_0001.png", StrictCardUniverse.DC, listOf("superman_0001", "super-homem", "super homem")),
        StrictCardHero("wwoman", "WWOMAN", "Wonder Woman", "03_wwoman_0901.png", StrictCardUniverse.DC, listOf("wwoman_0901", "wonder woman", "mulher-maravilha", "mulher maravilha")),
        StrictCardHero("flash", "FLASH", "Flash", "04_flash_0501.png", StrictCardUniverse.DC, listOf("flash_0501", "the flash")),
        StrictCardHero("aquaman", "AQUAMAN", "Aquaman", "05_aquaman_0701.png", StrictCardUniverse.DC, listOf("aquaman_0701")),
        StrictCardHero("glantern", "GLANTERN", "Green Lantern", "06_glantern_2814.png", StrictCardUniverse.DC, listOf("glantern_2814", "green lantern", "lanterna verde", "lanterna-verde")),
        StrictCardHero("garrow", "GARROW", "Green Arrow", "07_garrow_0101.png", StrictCardUniverse.DC, listOf("garrow_0101", "green arrow", "arqueiro verde", "arqueiro-verde")),
        StrictCardHero("cyborg", "CYBORG", "Cyborg", "08_cyborg_0001.png", StrictCardUniverse.DC, listOf("cyborg_0001")),

        // Marvel (8)
        StrictCardHero("ironman", "IRONMAN", "Iron Man", "09_ironman_0001.png", StrictCardUniverse.MARVEL, listOf("ironman_0001", "iron man", "homem de ferro", "homem-de-ferro")),
        StrictCardHero("captain", "CAPTAIN", "Captain America", "10_captain_0001.png", StrictCardUniverse.MARVEL, listOf("captain_0001", "captain america", "capitao america", "capitão américa", "capitao-america")),
        StrictCardHero("thor", "THOR", "Thor", "11_thor_0001.png", StrictCardUniverse.MARVEL, listOf("thor_0001")),
        StrictCardHero("hulk", "HULK", "Hulk", "12_hulk_0001.png", StrictCardUniverse.MARVEL, listOf("hulk_0001")),
        StrictCardHero("spider", "SPIDER", "Spider-Man", "13_spider_0001.png", StrictCardUniverse.MARVEL, listOf("spider_0001", "spiderman", "spider-man", "homem aranha", "homem-aranha")),
        StrictCardHero("bwidow", "BWIDOW", "Black Widow", "14_bwidow_0001.png", StrictCardUniverse.MARVEL, listOf("bwidow_0001", "black widow", "viuva negra", "viúva negra", "viuva-negra")),
        StrictCardHero("dstrange", "DSTRANGE", "Doctor Strange", "15_dstrange_0001.png", StrictCardUniverse.MARVEL, listOf("dstrange_0001", "doctor strange", "doutor estranho", "doutor-estranho")),
        StrictCardHero("bpanther", "BPANTHER", "Black Panther", "16_bpanther_0001.png", StrictCardUniverse.MARVEL, listOf("bpanther_0001", "black panther", "pantera negra", "pantera-negra"))
    )

    private fun normalize(raw: String): String =
        raw.trim()
            .lowercase()
            .removeSuffix(".png")
            .replace(Regex("[áàâãä]"), "a")
            .replace(Regex("[éèêë]"), "e")
            .replace(Regex("[íìîï]"), "i")
            .replace(Regex("[óòôõö]"), "o")
            .replace(Regex("[úùûü]"), "u")
            .replace("ç", "c")
            .replace(Regex("[^a-z0-9_]+"), "-")
            .trim('-')

    fun findMatch(vararg candidates: String?): StrictCardHero? {
        for (candidate in candidates) {
            if (candidate.isNullOrBlank()) continue
            val norm = normalize(candidate)
            if (norm.isBlank()) continue

            all16.firstOrNull { spec ->
                normalize(spec.key) == norm ||
                    normalize(spec.baseTag) == norm ||
                    normalize(spec.assetFileName) == norm ||
                    normalize(spec.displayName) == norm ||
                    spec.aliases.any { normalize(it) == norm }
            }?.let { return it }

            all16.firstOrNull { spec ->
                val terms = (listOf(spec.key, spec.baseTag, spec.displayName) + spec.aliases).map { normalize(it) }
                terms.any { term -> term.length >= 4 && norm.contains(term) }
            }?.let { return it }
        }
        return null
    }

    fun resolveFrom16(seedIndex: Int, vararg candidates: String?): StrictCardHero {
        findMatch(*candidates)?.let { return it }
        val idx = abs(seedIndex) % all16.size
        return all16[idx]
    }

    fun formatHeroNumberedName(spec: StrictCardHero, sequenceNumber: Int): String {
        val safeNum = sequenceNumber.coerceAtLeast(1)
        return "${spec.baseTag}_${safeNum.toString().padStart(4, '0')}"
    }

    fun nextSequenceNumber(): Int = sequenceCounter.getAndIncrement()

    fun createHeroIdentity(seedIndex: Int = 0, vararg hints: String?): HeroIdentity {
        val seq = nextSequenceNumber()
        val spec = resolveFrom16(seedIndex = seedIndex, candidates = hints)
        return HeroIdentity(
            name = formatHeroNumberedName(spec, seq),
            universe = spec.universe.value,
            avatarKey = spec.key
        )
    }

    fun buildStrictProfile(
        seedIndex: Int = 0,
        hint: String? = null,
        sourceType: String = "m3u",
        sourceLabel: String = "",
        server: String? = null,
        user: String? = null,
        pass: String? = null,
        validityIso: String? = null,
        maxConnections: Int? = null,
        activeConnections: Int = 0,
        liveCount: Int = 0,
        vodCount: Int = 0,
        seriesCount: Int = 0,
        bulkOrigin: Boolean = false,
        isActive: Boolean = false
    ): ProfileEntity {
        val identity = createHeroIdentity(seedIndex = seedIndex, hint, server, sourceLabel)
        return ProfileEntity(
            name = identity.name,
            avatarKey = identity.avatarKey,
            universe = identity.universe,
            sourceType = sourceType,
            sourceLabel = sourceLabel,
            server = server,
            user = user,
            pass = pass,
            validityIso = validityIso,
            maxConnections = maxConnections,
            activeConnections = activeConnections,
            liveCount = liveCount,
            vodCount = vodCount,
            seriesCount = seriesCount,
            bulkOrigin = bulkOrigin,
            isActive = isActive
        )
    }
}

object HeroGenerator {
    fun generateHeroName(seedIndex: Int = 0, vararg hints: String?): HeroIdentity =
        CardProfileHelper.createHeroIdentity(seedIndex = seedIndex, hints = hints)
}

object PlaylistParser {

    fun parseM3U(text: String): List<PlaylistItem> {
        val lines = text.split(Regex("\r?\n"))
        val channels = mutableListOf<PlaylistItem>()
        var pendingName: String? = null
        var pendingGroup = "Geral"
        var pendingLogo: String? = null
        var hasPendingInfo = false

        val nameRegex = Regex(",(.*)$")
        val groupRegex = Regex("""group-title="([^"]*)"""")
        val logoRegex = Regex("""tvg-logo="([^"]*)"""")

        for (raw in lines) {
            val l = raw.trim().removePrefix("\uFEFF")
            if (l.isEmpty()) continue

            if (l.startsWith("#EXTINF:")) {
                val nMatch = nameRegex.find(l)
                val gMatch = groupRegex.find(l)
                val logoMatch = logoRegex.find(l)

                pendingName = nMatch?.groupValues?.getOrNull(1)?.trim()?.ifEmpty { "Canal" } ?: "Canal"
                pendingGroup = gMatch?.groupValues?.getOrNull(1)?.trim()?.ifEmpty { "Geral" } ?: "Geral"
                pendingLogo = logoMatch?.groupValues?.getOrNull(1)?.trim()?.ifEmpty { null }
                hasPendingInfo = true
            } else if (!l.startsWith("#") && hasPendingInfo) {
                channels.add(
                    PlaylistItem(
                        title = pendingName ?: "Canal",
                        url = l,
                        groupName = pendingGroup,
                        contentType = "LIVE",
                        streamIcon = pendingLogo
                    )
                )
                hasPendingInfo = false
                pendingName = null
                pendingGroup = "Geral"
                pendingLogo = null
            }
        }
        return channels
    }

    fun parseTXT(text: String): List<PlaylistItem> {
        val lines = text.split(Regex("\r?\n"))
        val channels = mutableListOf<PlaylistItem>()
        for (raw in lines) {
            val l = raw.trim().removePrefix("\uFEFF")
            if (l.isEmpty() || l.startsWith("#")) continue
            val inline = parseInlineChannel(l)
            if (inline != null) {
                channels.add(
                    PlaylistItem(
                        title = inline.first,
                        url = inline.second,
                        groupName = "TXT",
                        contentType = "LIVE"
                    )
                )
            }
        }
        return channels
    }

    fun parseInlineChannel(line: String): Pair<String, String>? {
        val trimmed = line.trim()
        if (trimmed.contains(",")) {
            val parts = trimmed.split(",")
            val p0 = parts.getOrNull(0)?.trim().orEmpty()
            val p1 = parts.getOrNull(1)?.trim().orEmpty()
            if (p0.matches(Regex("^https?://.*", RegexOption.IGNORE_CASE))) {
                return Pair(p1.ifEmpty { "Canal" }, p0)
            }
            if (p1.matches(Regex("^https?://.*", RegexOption.IGNORE_CASE))) {
                return Pair(p0.ifEmpty { "Canal" }, p1)
            }
        } else if (trimmed.matches(Regex("^https?://.*", RegexOption.IGNORE_CASE))) {
            return Pair("Canal", trimmed)
        }
        return null
    }

    fun detectXtreamPure(text: String): XtreamCredentials? {
        val patterns = listOf(
            Regex("""https?://[^\s]+/get\.php\?[^\s]*username=([^&\s]+)[^\s]*password=([^&\s]+)""", RegexOption.IGNORE_CASE),
            Regex("""https?://[^\s]+/player_api\.php\?[^\s]*username=([^&\s]+)[^\s]*password=([^&\s]+)""", RegexOption.IGNORE_CASE)
        )
        val hostRegex = Regex("""(https?://[^/\s]+)""", RegexOption.IGNORE_CASE)

        for (p in patterns) {
            val m = p.find(text)
            if (m != null) {
                val h = hostRegex.find(text)
                if (h != null) {
                    val server = h.groupValues[1].trimEnd('/')
                    val user = m.groupValues[1].trim()
                    val pass = m.groupValues[2].trim()
                    if (user.isNotEmpty() && pass.isNotEmpty()) {
                        return XtreamCredentials(server = server, username = user, password = pass)
                    }
                }
            }
        }
        return null
    }

    fun parseHeroUrls(text: String): List<String> {
        return text.split(Regex("\r?\n"))
            .map { it.trim().removePrefix("\uFEFF") }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .map { l ->
                val idx = l.indexOf(" | ")
                if (idx > -1) l.substring(0, idx).trim() else l
            }
            .filter { it.startsWith("http", ignoreCase = true) }
    }
}

class IptvRepository(
    private val profileDao: ProfileDao,
    private val channelDao: ChannelDao,
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        const val HERO_LIST_URL =
            "https://raw.githubusercontent.com/NOSLIUDE/Satsil/refs/heads/main/all_in_one.txt"
    }

    val allProfiles: Flow<List<ProfileEntity>> = profileDao.getAllProfiles()

    fun getChannelsByProfile(profileId: Long): Flow<List<ChannelEntity>> =
        channelDao.getChannelsByProfile(profileId)

    suspend fun getActiveProfile(): ProfileEntity? = profileDao.getActiveProfile()

    suspend fun setActiveProfile(profileId: Long) {
        profileDao.setActiveProfile(profileId)
    }

    suspend fun deleteProfile(profileId: Long) {
        profileDao.deleteProfileById(profileId)
    }

    suspend fun clearAllProfiles() {
        profileDao.deleteAllProfiles()
    }

    fun sha256Hex(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    suspend fun fetchHeroListRaw(url: String = HERO_LIST_URL): Pair<String, List<String>> =
        withContext(Dispatchers.IO) {
            val clean = url.trim()
            val sep = if (clean.contains("?")) "&" else "?"
            val bustedUrl = "$clean${sep}t=${System.currentTimeMillis()}"
            val req = Request.Builder()
                .url(bustedUrl)
                .header("Cache-Control", "no-cache, no-store")
                .build()

            val text = httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) throw IllegalStateException("HTTP ${resp.code}")
                resp.body?.string().orEmpty()
            }
            if (text.isBlank()) throw IllegalStateException("Lista vazia no servidor.")
            val urls = PlaylistParser.parseHeroUrls(text)
            if (urls.isEmpty()) throw IllegalStateException("Nenhuma lista valida encontrada.")
            Pair(text, urls)
        }

    suspend fun processBulkLines(
        rawLines: List<String>,
        isHeroOrigin: Boolean = true,
        onProgress: ((BulkProgress) -> Unit)? = null
    ): Pair<Int, Int> = withContext(Dispatchers.IO) {
        val lines = rawLines
            .map { it.trim().removePrefix("\uFEFF") }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .map { l ->
                val idx = l.indexOf(" | ")
                if (idx > -1 && l.substring(0, idx).trim().startsWith("http", ignoreCase = true)) {
                    l.substring(0, idx).trim()
                } else {
                    l
                }
            }
            .take(100_000)

        val total = lines.size
        if (total == 0) return@withContext Pair(0, 0)

        val successCount = AtomicInteger(0)
        val failedCount = AtomicInteger(0)
        val processedCount = AtomicInteger(0)
        val semaphore = Semaphore(15)

        coroutineScope {
            val jobs = lines.mapIndexed { idx, line ->
                async {
                    semaphore.withPermit {
                        val resultLog = try {
                            val info = processSingleBulkLine(line, isHeroOrigin, idx)
                            successCount.incrementAndGet()
                            "OK [${idx + 1}] $info"
                        } catch (e: Exception) {
                            failedCount.incrementAndGet()
                            "ERRO [${idx + 1}] ${e.message ?: "Falha"}"
                        }
                        val done = processedCount.incrementAndGet()
                        onProgress?.invoke(
                            BulkProgress(
                                current = done,
                                total = total,
                                success = successCount.get(),
                                failed = failedCount.get(),
                                lastLog = resultLog
                            )
                        )
                    }
                }
            }
            jobs.awaitAll()
        }

        Pair(successCount.get(), failedCount.get())
    }

    private suspend fun processSingleBulkLine(line: String, bulkOrigin: Boolean, seedIndex: Int): String {
        val creds = PlaylistParser.detectXtreamPure(line)
        if (creds != null) {
            return bulkLoadXtreamProfileOnly(creds.server, creds.username, creds.password, bulkOrigin, seedIndex)
        }

        if (line.matches(Regex("^https?://.*", RegexOption.IGNORE_CASE))) {
            val isM3uHint = line.contains("m3u", ignoreCase = true) ||
                line.contains("get.php", ignoreCase = true) ||
                line.contains("type=m3u", ignoreCase = true)
            val profile = CardProfileHelper.buildStrictProfile(
                seedIndex = seedIndex,
                hint = line,
                sourceType = if (isM3uHint) "m3u" else "txt",
                sourceLabel = line.take(300),
                liveCount = 0,
                vodCount = 0,
                seriesCount = 0,
                bulkOrigin = bulkOrigin,
                isActive = false
            )
            val profileId = profileDao.insertProfile(profile)
            profileDao.setActiveProfile(profileId)
            return "${profile.name} (${if (isM3uHint) "M3U" else "TXT"}) - pendente"
        }

        val inline = PlaylistParser.parseInlineChannel(line)
        if (inline != null) {
            val profile = CardProfileHelper.buildStrictProfile(
                seedIndex = seedIndex,
                hint = inline.first,
                sourceType = "txt",
                sourceLabel = inline.first,
                liveCount = 1,
                bulkOrigin = bulkOrigin,
                isActive = false
            )
            val profileId = profileDao.insertProfile(profile)
            channelDao.insertChannels(
                listOf(
                    ChannelEntity(
                        profileId = profileId,
                        title = inline.first,
                        url = inline.second,
                        groupName = "TXT",
                        contentType = "LIVE"
                    )
                )
            )
            profileDao.setActiveProfile(profileId)
            return "${profile.name} (inline) - 1"
        }

        throw IllegalStateException("Nao reconhecido")
    }

    private suspend fun bulkLoadXtreamProfileOnly(
        server: String,
        user: String,
        pass: String,
        bulkOrigin: Boolean,
        seedIndex: Int = 0
    ): String {
        val cleanServer = server.trim().trimEnd('/')
        val encUser = URLEncoder.encode(user.trim(), "UTF-8")
        val encPass = URLEncoder.encode(pass.trim(), "UTF-8")
        val authUrl = "$cleanServer/player_api.php?username=$encUser&password=$encPass"

        val authJson = fetchJsonObject(authUrl)
        val userInfo = authJson?.optJSONObject("user_info")
        if (userInfo == null || userInfo.optInt("auth", 0) == 0) {
            throw IllegalStateException("Credenciais invalidas")
        }

        val expSeconds = userInfo.optString("exp_date").toLongOrNull() ?: 0L
        val validityIso = if (expSeconds > 0L) Instant.ofEpochSecond(expSeconds).toString() else null
        val maxConnections = userInfo.optString("max_connections").toIntOrNull()
        val activeConnections = userInfo.optString("active_cons").toIntOrNull() ?: 0

        val profile = CardProfileHelper.buildStrictProfile(
            seedIndex = seedIndex,
            hint = cleanServer,
            sourceType = "xtream",
            sourceLabel = cleanServer,
            server = cleanServer,
            user = user.trim(),
            pass = pass.trim(),
            validityIso = validityIso,
            maxConnections = maxConnections,
            activeConnections = activeConnections,
            liveCount = 0,
            vodCount = 0,
            seriesCount = 0,
            bulkOrigin = bulkOrigin,
            isActive = false
        )
        val profileId = profileDao.insertProfile(profile)
        profileDao.setActiveProfile(profileId)
        return "${profile.name} (XTREAM) - perfil salvo"
    }

    suspend fun activateAndSyncProfile(
        profileId: Long,
        onStage: ((Float, String) -> Unit)? = null
    ): Int = withContext(Dispatchers.IO) {
        val profile = profileDao.getProfileById(profileId) ?: return@withContext 0
        profileDao.setActiveProfile(profileId)
        onStage?.invoke(0.15f, "Perfil ativado: ${profile.name}")

        if (profile.sourceType.equals("xtream", ignoreCase = true) &&
            !profile.server.isNullOrBlank() &&
            !profile.user.isNullOrBlank() &&
            !profile.pass.isNullOrBlank()
        ) {
            return@withContext loadXtreamData(
                server = profile.server,
                username = profile.user,
                password = profile.pass,
                existingProfileId = profile.id,
                onStage = onStage
            )
        }

        if (profile.sourceType.equals("m3u", ignoreCase = true) &&
            profile.sourceLabel.startsWith("http", ignoreCase = true)
        ) {
            return@withContext loadM3UUrlForProfile(profile.id, profile.sourceLabel, onStage)
        }

        if (profile.sourceType.equals("txt", ignoreCase = true) &&
            profile.sourceLabel.startsWith("http", ignoreCase = true)
        ) {
            return@withContext loadM3UUrlForProfile(profile.id, profile.sourceLabel, onStage)
        }

        onStage?.invoke(0.95f, "Contando itens salvos...")
        channelDao.countChannelsByProfile(profileId)
    }

    suspend fun loadM3UUrlForProfile(
        profileId: Long,
        url: String,
        onStage: ((Float, String) -> Unit)? = null
    ): Int =
        withContext(Dispatchers.IO) {
            onStage?.invoke(0.25f, "Baixando lista...")
            val req = Request.Builder().url(url.trim()).build()
            val text = httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) throw IllegalStateException("HTTP ${resp.code}")
                resp.body?.string().orEmpty()
            }
            onStage?.invoke(0.55f, "Processando canais, filmes e séries...")
            val items = if (text.contains("#EXTM3U") || text.contains("#EXTINF")) {
                PlaylistParser.parseM3U(text)
            } else {
                PlaylistParser.parseTXT(text)
            }

            onStage?.invoke(0.75f, "Salvando ${items.size} itens no banco...")
            channelDao.deleteChannelsByProfile(profileId)
            if (items.isNotEmpty()) {
                val entities = items.map {
                    ChannelEntity(
                        profileId = profileId,
                        title = it.title,
                        url = it.url,
                        groupName = it.groupName,
                        contentType = it.contentType,
                        streamIcon = it.streamIcon,
                        remoteId = it.remoteId
                    )
                }
                entities.chunked(500).forEach { batch ->
                    channelDao.insertChannels(batch)
                }
            }

            onStage?.invoke(0.92f, "Atualizando perfil...")
            profileDao.getProfileById(profileId)?.let { p ->
                val spec = CardProfileHelper.resolveFrom16(p.id.toInt(), p.avatarKey, p.name)
                profileDao.updateProfile(
                    p.copy(
                        avatarKey = spec.key,
                        universe = spec.universe.value,
                        liveCount = items.size,
                        vodCount = 0,
                        seriesCount = 0,
                        lastUsed = System.currentTimeMillis()
                    )
                )
            }
            items.size
        }

    suspend fun loadXtreamData(
        server: String,
        username: String,
        password: String,
        existingProfileId: Long? = null,
        onStage: ((Float, String) -> Unit)? = null
    ): Int = withContext(Dispatchers.IO) {
        val cleanServer = server.trim().trimEnd('/')
        val encUser = URLEncoder.encode(username.trim(), "UTF-8")
        val encPass = URLEncoder.encode(password.trim(), "UTF-8")
        val base = "$cleanServer/player_api.php?username=$encUser&password=$encPass"

        onStage?.invoke(0.20f, "Autenticando conta Xtream...")
        val authJson = fetchJsonObject(base)
        val userInfo = authJson?.optJSONObject("user_info")
        if (userInfo == null || userInfo.optInt("auth", 0) == 0) {
            throw IllegalArgumentException("Credenciais invalidas")
        }

        val expSeconds = userInfo.optString("exp_date").toLongOrNull() ?: 0L
        val validityIso = if (expSeconds > 0L) Instant.ofEpochSecond(expSeconds).toString() else null
        val maxConnections = userInfo.optString("max_connections").toIntOrNull()
        val activeConnections = userInfo.optString("active_cons").toIntOrNull() ?: 0

        val liveItems = mutableListOf<PlaylistItem>()
        val vodItems = mutableListOf<PlaylistItem>()
        val seriesItems = mutableListOf<PlaylistItem>()

        onStage?.invoke(0.36f, "Carregando canais ao vivo (TV)...")
        runCatching {
            val cats = fetchJsonArray("$base&action=get_live_categories").toCategoryMap()
            val streams = fetchJsonArray("$base&action=get_live_streams")
            if (streams != null) {
                for (i in 0 until streams.length()) {
                    val x = streams.optJSONObject(i) ?: continue
                    val streamId = x.optString("stream_id").takeIf { it.isNotBlank() } ?: continue
                    val title = x.optString("name").ifBlank { "Canal" }
                    val catId = x.optString("category_id")
                    val group = cats[catId] ?: "Geral"
                    val icon = x.optString("stream_icon").ifBlank { x.optString("stream_icon_url") }.ifBlank { null }
                    liveItems.add(
                        PlaylistItem(
                            title = title,
                            url = "$cleanServer/live/$encUser/$encPass/$streamId.m3u8",
                            groupName = group,
                            contentType = "LIVE",
                            streamIcon = icon,
                            remoteId = streamId
                        )
                    )
                }
            }
        }

        onStage?.invoke(0.56f, "Carregando filmes (VOD)...")
        runCatching {
            val cats = fetchJsonArray("$base&action=get_vod_categories").toCategoryMap()
            val streams = fetchJsonArray("$base&action=get_vod_streams")
            if (streams != null) {
                for (i in 0 until streams.length()) {
                    val x = streams.optJSONObject(i) ?: continue
                    val streamId = x.optString("stream_id").takeIf { it.isNotBlank() } ?: continue
                    val title = x.optString("name").ifBlank { "Filme" }
                    val ext = x.optString("container_extension").ifBlank { "mp4" }
                    val catId = x.optString("category_id")
                    val group = cats[catId] ?: "Geral"
                    val icon = x.optString("stream_icon").ifBlank { x.optString("movie_image") }.ifBlank { null }
                    vodItems.add(
                        PlaylistItem(
                            title = title,
                            url = "$cleanServer/movie/$encUser/$encPass/$streamId.$ext",
                            groupName = group,
                            contentType = "VOD",
                            streamIcon = icon,
                            remoteId = streamId
                        )
                    )
                }
            }
        }

        onStage?.invoke(0.76f, "Carregando séries...")
        runCatching {
            val cats = fetchJsonArray("$base&action=get_series_categories").toCategoryMap()
            val streams = fetchJsonArray("$base&action=get_series")
            if (streams != null) {
                for (i in 0 until streams.length()) {
                    val x = streams.optJSONObject(i) ?: continue
                    val seriesId = x.optString("series_id").takeIf { it.isNotBlank() } ?: continue
                    val title = x.optString("name").ifBlank { "Série" }
                    val catId = x.optString("category_id")
                    val group = cats[catId] ?: "Geral"
                    val cover = x.optString("cover").ifBlank { x.optString("series_icon") }.ifBlank { null }
                    seriesItems.add(
                        PlaylistItem(
                            title = title,
                            url = "$cleanServer/series/$encUser/$encPass/$seriesId",
                            groupName = group,
                            contentType = "SERIES",
                            streamIcon = cover,
                            remoteId = seriesId
                        )
                    )
                }
            }
        }

        val targetProfileId = if (existingProfileId != null) {
            val existing = profileDao.getProfileById(existingProfileId)
            if (existing != null) {
                val spec = CardProfileHelper.resolveFrom16(existing.id.toInt(), existing.avatarKey, existing.name)
                profileDao.updateProfile(
                    existing.copy(
                        avatarKey = spec.key,
                        universe = spec.universe.value,
                        validityIso = validityIso,
                        maxConnections = maxConnections,
                        activeConnections = activeConnections,
                        liveCount = liveItems.size,
                        vodCount = vodItems.size,
                        seriesCount = seriesItems.size,
                        lastUsed = System.currentTimeMillis()
                    )
                )
                existing.id
            } else {
                createNewXtreamProfile(cleanServer, username, password, validityIso, maxConnections, activeConnections, liveItems.size, vodItems.size, seriesItems.size)
            }
        } else {
            createNewXtreamProfile(cleanServer, username, password, validityIso, maxConnections, activeConnections, liveItems.size, vodItems.size, seriesItems.size)
        }

        val combined = liveItems + vodItems + seriesItems
        onStage?.invoke(0.90f, "Salvando ${combined.size} itens (TV, Filmes e Séries)...")
        channelDao.deleteChannelsByProfile(targetProfileId)
        if (combined.isNotEmpty()) {
            val entities = combined.map {
                ChannelEntity(
                    profileId = targetProfileId,
                    title = it.title,
                    url = it.url,
                    groupName = it.groupName,
                    contentType = it.contentType,
                    streamIcon = it.streamIcon,
                    remoteId = it.remoteId
                )
            }
            entities.chunked(500).forEach { batch ->
                channelDao.insertChannels(batch)
            }
        }

        profileDao.setActiveProfile(targetProfileId)
        combined.size
    }

    private suspend fun createNewXtreamProfile(
        cleanServer: String,
        username: String,
        password: String,
        validityIso: String?,
        maxConnections: Int?,
        activeConnections: Int,
        liveCount: Int,
        vodCount: Int,
        seriesCount: Int
    ): Long {
        val profile = CardProfileHelper.buildStrictProfile(
            seedIndex = 0,
            hint = cleanServer,
            sourceType = "xtream",
            sourceLabel = cleanServer,
            server = cleanServer,
            user = username.trim(),
            pass = password.trim(),
            validityIso = validityIso,
            maxConnections = maxConnections,
            activeConnections = activeConnections,
            liveCount = liveCount,
            vodCount = vodCount,
            seriesCount = seriesCount,
            bulkOrigin = false,
            isActive = true
        )
        return profileDao.insertProfile(profile)
    }

    suspend fun fetchSeriesSeasons(
        profile: ProfileEntity,
        seriesId: String
    ): Map<String, List<SeriesEpisode>> = withContext(Dispatchers.IO) {
        val cleanServer = profile.server?.trimEnd('/') ?: throw IllegalStateException("Servidor Xtream ausente")
        val encUser = URLEncoder.encode(profile.user.orEmpty(), "UTF-8")
        val encPass = URLEncoder.encode(profile.pass.orEmpty(), "UTF-8")
        val url = "$cleanServer/player_api.php?username=$encUser&password=$encPass&action=get_series_info&series_id=$seriesId"

        val root = fetchJsonObject(url) ?: throw IllegalStateException("Falha ao buscar episodios da serie.")
        val episodesObj = root.optJSONObject("episodes") ?: return@withContext emptyMap()

        val result = linkedMapOf<String, List<SeriesEpisode>>()
        val sortedKeys = episodesObj.keys().asSequence().toList()
            .sortedBy { it.toDoubleOrNull() ?: Double.MAX_VALUE }

        for (seasonNum in sortedKeys) {
            val arr = episodesObj.optJSONArray(seasonNum) ?: continue
            val list = mutableListOf<SeriesEpisode>()
            for (i in 0 until arr.length()) {
                val ep = arr.optJSONObject(i) ?: continue
                val id = ep.optString("id")
                    .ifBlank { ep.optString("episode_id") }
                    .ifBlank { ep.optString("stream_id") }
                    .ifBlank { ep.optString("vod_id") }
                if (id.isBlank()) continue

                val epNum = ep.optInt("episode_num", ep.optInt("episode", i + 1))
                val title = ep.optString("title")
                    .ifBlank { ep.optString("name") }
                    .ifBlank { "Episódio $epNum" }
                val ext = ep.optString("container_extension")
                    .ifBlank { ep.optString("ext") }
                    .ifBlank { "mp4" }
                val dur = ep.optString("duration").ifBlank {
                    ep.optJSONObject("info")?.optString("duration").orEmpty()
                }.ifBlank { null }

                list.add(
                    SeriesEpisode(
                        id = id,
                        episodeNum = epNum,
                        title = title,
                        extension = ext,
                        duration = dur
                    )
                )
            }
            result[seasonNum] = list.sortedBy { it.episodeNum }
        }
        result
    }

    suspend fun fetchVodDetails(
        profile: ProfileEntity,
        channel: ChannelEntity
    ): MediaDetails = withContext(Dispatchers.IO) {
        val cleanServer = profile.server?.trimEnd('/')
        val vodId = channel.remoteId
        if (cleanServer.isNullOrBlank() || vodId.isNullOrBlank()) {
            return@withContext fallbackMediaDetails(channel, "VOD")
        }
        val encUser = URLEncoder.encode(profile.user.orEmpty(), "UTF-8")
        val encPass = URLEncoder.encode(profile.pass.orEmpty(), "UTF-8")
        val url = "$cleanServer/player_api.php?username=$encUser&password=$encPass&action=get_vod_info&vod_id=$vodId"

        val root = fetchJsonObject(url) ?: return@withContext fallbackMediaDetails(channel, "VOD")
        val info = root.optJSONObject("info")
        val movieData = root.optJSONObject("movie_data")

        val releaseDate = info?.optString("releasedate")
            .orEmpty()
            .ifBlank { info?.optString("releaseDate").orEmpty() }
        val year = releaseDate.take(4).ifBlank {
            Regex("\\((\\d{4})\\)").find(channel.title)?.groupValues?.getOrNull(1).orEmpty()
        }
        val rating = info?.optString("rating").orEmpty()
            .ifBlank { info?.optString("rating_5based").orEmpty() }
        val duration = info?.optString("duration").orEmpty()
        val genreRaw = info?.optString("genre").orEmpty().ifBlank { channel.groupName }
        val genres = genreRaw.split(",", "/", "|")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val backdropArr = info?.optJSONArray("backdrop_path")
        val backdrop = backdropArr?.optString(0)?.takeIf { it.isNotBlank() }
            ?: info?.optString("cover_big")?.takeIf { it.isNotBlank() }

        val ext = movieData?.optString("container_extension")?.ifBlank { "mp4" }
            ?: channel.url.substringAfterLast('.', "mp4").substringBefore('?').ifBlank { "mp4" }
        val resolvedStream = if (channel.url.isNotBlank()) channel.url
        else "$cleanServer/movie/$encUser/$encPass/$vodId.$ext"

        MediaDetails(
            title = info?.optString("name").orEmpty().ifBlank { channel.title },
            plot = info?.optString("plot").orEmpty()
                .ifBlank { info?.optString("description").orEmpty() }
                .ifBlank { "Sinopse não disponível na lista atual." },
            releaseDate = releaseDate.ifBlank { "Não informada" },
            year = year.ifBlank { "—" },
            ratingImdb = rating.ifBlank { "N/A" },
            duration = duration.ifBlank { "—" },
            genres = genres.ifEmpty { listOf(channel.groupName.ifBlank { "Geral" }) },
            posterUrl = info?.optString("movie_image")?.takeIf { it.isNotBlank() } ?: channel.streamIcon,
            backdropUrl = backdrop ?: channel.streamIcon,
            streamUrl = resolvedStream,
            extension = ext,
            contentType = "VOD"
        )
    }

    suspend fun fetchSeriesDetailsAndSeasons(
        profile: ProfileEntity,
        channel: ChannelEntity
    ): Pair<MediaDetails, Map<String, List<SeriesEpisode>>> = withContext(Dispatchers.IO) {
        val cleanServer = profile.server?.trimEnd('/')
        val seriesId = channel.remoteId
        if (cleanServer.isNullOrBlank() || seriesId.isNullOrBlank()) {
            return@withContext fallbackMediaDetails(channel, "SERIES") to emptyMap()
        }
        val encUser = URLEncoder.encode(profile.user.orEmpty(), "UTF-8")
        val encPass = URLEncoder.encode(profile.pass.orEmpty(), "UTF-8")
        val url = "$cleanServer/player_api.php?username=$encUser&password=$encPass&action=get_series_info&series_id=$seriesId"

        val root = fetchJsonObject(url) ?: return@withContext fallbackMediaDetails(channel, "SERIES") to emptyMap()
        val info = root.optJSONObject("info")

        val releaseDate = info?.optString("releaseDate").orEmpty()
            .ifBlank { info?.optString("releasedate").orEmpty() }
        val year = releaseDate.take(4).ifBlank {
            Regex("\\((\\d{4})\\)").find(channel.title)?.groupValues?.getOrNull(1).orEmpty()
        }
        val rating = info?.optString("rating").orEmpty()
            .ifBlank { info?.optString("rating_5based").orEmpty() }
        val episodeRunTime = info?.optString("episode_run_time").orEmpty()
        val genreRaw = info?.optString("genre").orEmpty().ifBlank { channel.groupName }
        val genres = genreRaw.split(",", "/", "|").map { it.trim() }.filter { it.isNotBlank() }
        val backdrop = info?.optJSONArray("backdrop_path")?.optString(0)?.takeIf { it.isNotBlank() }
            ?: info?.optString("cover")?.takeIf { it.isNotBlank() }

        val details = MediaDetails(
            title = info?.optString("name").orEmpty().ifBlank { channel.title },
            plot = info?.optString("plot").orEmpty().ifBlank { "Sinopse não disponível na lista atual." },
            releaseDate = releaseDate.ifBlank { "Não informada" },
            year = year.ifBlank { "—" },
            ratingImdb = rating.ifBlank { "N/A" },
            duration = if (episodeRunTime.isNotBlank()) "$episodeRunTime min/ep" else "—",
            genres = genres.ifEmpty { listOf(channel.groupName.ifBlank { "Geral" }) },
            posterUrl = info?.optString("cover")?.takeIf { it.isNotBlank() } ?: channel.streamIcon,
            backdropUrl = backdrop ?: channel.streamIcon,
            streamUrl = null,
            extension = "mp4",
            contentType = "SERIES"
        )

        val seasons = fetchSeriesSeasons(profile, seriesId)
        details to seasons
    }

    private fun fallbackMediaDetails(channel: ChannelEntity, type: String): MediaDetails {
        val year = Regex("\\((\\d{4})\\)").find(channel.title)?.groupValues?.getOrNull(1) ?: "—"
        val ext = channel.url.substringAfterLast('.', "mp4").substringBefore('?').takeIf { it.length in 2..5 } ?: "mp4"
        return MediaDetails(
            title = channel.title,
            plot = "Sinopse não disponível para listas M3U simples.",
            releaseDate = year,
            year = year,
            ratingImdb = "N/A",
            duration = "—",
            genres = listOf(channel.groupName.ifBlank { "Geral" }),
            posterUrl = channel.streamIcon,
            backdropUrl = channel.streamIcon,
            streamUrl = channel.url,
            extension = ext,
            contentType = type
        )
    }

    @Volatile
    private var cachedXmltvKey: String? = null
    @Volatile
    private var cachedXmltvFetchedAt: Long = 0L
    @Volatile
    private var cachedXmltvPrograms: Map<String, List<XmltvProgramme>> = emptyMap()

    private data class XmltvProgramme(
        val channelKey: String,
        val title: String,
        val startEpochMs: Long,
        val stopEpochMs: Long,
        val startFormatted: String,
        val endFormatted: String
    )

    suspend fun fetchShortEpg(
        profile: ProfileEntity?,
        streamUrl: String?,
        remoteId: String?,
        channelTitle: String = ""
    ): List<EpgProgramItem> =
        withContext(Dispatchers.IO) {
            val baseServer = profile?.server?.trimEnd('/')
            val user = profile?.user?.trim()
            val pass = profile?.pass?.trim()
            val resolvedId = remoteId?.takeIf { it.isNotBlank() } ?: extractStreamIdFromUrl(streamUrl)

            if (!baseServer.isNullOrBlank() && !user.isNullOrBlank() && !pass.isNullOrBlank() && !resolvedId.isNullOrBlank()) {
                val jsonItems = try {
                    val encUser = URLEncoder.encode(user, "UTF-8")
                    val encPass = URLEncoder.encode(pass, "UTF-8")
                    val encId = URLEncoder.encode(resolvedId, "UTF-8")
                    val apiUrl = "$baseServer/player_api.php?username=$encUser&password=$encPass&action=get_short_epg&stream_id=$encId"
                    val req = Request.Builder().url(apiUrl).build()
                    httpClient.newCall(req).execute().use { resp ->
                        if (!resp.isSuccessful) emptyList()
                        else {
                            val body = resp.body?.string().orEmpty()
                            if (body.isBlank()) emptyList() else parseShortEpgJson(body)
                        }
                    }
                } catch (_: Exception) {
                    emptyList()
                }
                if (jsonItems.isNotEmpty()) return@withContext jsonItems
            }

            fetchXmltvFallback(profile, resolvedId, channelTitle)
        }

    private fun fetchXmltvFallback(
        profile: ProfileEntity?,
        resolvedId: String?,
        channelTitle: String
    ): List<EpgProgramItem> {
        val xmltvUrl = resolveXmltvUrl(profile) ?: return emptyList()
        val nowMs = System.currentTimeMillis()
        val cacheValid = cachedXmltvKey == xmltvUrl && (nowMs - cachedXmltvFetchedAt) < 15 * 60 * 1000L

        val grouped = if (cacheValid) {
            cachedXmltvPrograms
        } else {
            val parsed = try {
                val req = Request.Builder().url(xmltvUrl).build()
                httpClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) emptyMap()
                    else parseXmltvStream(resp.body?.charStream() ?: return@use emptyMap())
                }
            } catch (_: Exception) {
                emptyMap()
            }
            if (parsed.isNotEmpty()) {
                cachedXmltvKey = xmltvUrl
                cachedXmltvFetchedAt = nowMs
                cachedXmltvPrograms = parsed
            }
            parsed
        }

        if (grouped.isEmpty()) return emptyList()

        val candidates = buildList {
            if (!resolvedId.isNullOrBlank()) add(normalizeKey(resolvedId))
            if (channelTitle.isNotBlank()) add(normalizeKey(channelTitle))
        }.filter { it.isNotBlank() }

        val matchedList = candidates.firstNotNullOfOrNull { key ->
            grouped[key] ?: grouped.entries.firstOrNull { (k, _) ->
                k.contains(key) || key.contains(k)
            }?.value
        }.orEmpty()

        if (matchedList.isEmpty()) return emptyList()

        val currentIdx = matchedList.indexOfFirst { it.startEpochMs <= nowMs && nowMs < it.stopEpochMs }
            .let { idx -> if (idx >= 0) idx else matchedList.indexOfFirst { it.startEpochMs >= nowMs }.coerceAtLeast(0) }

        return matchedList.drop(currentIdx).take(3).mapIndexed { index, prog ->
            EpgProgramItem(
                title = prog.title,
                startTime = prog.startFormatted,
                endTime = prog.endFormatted,
                isCurrent = index == 0
            )
        }
    }

    private fun resolveXmltvUrl(profile: ProfileEntity?): String? {
        val label = profile?.sourceLabel.orEmpty().trim()
        if (label.startsWith("http://", true) || label.startsWith("https://", true)) {
            if (label.contains("xmltv", ignoreCase = true) || label.endsWith(".xml", ignoreCase = true)) {
                return label
            }
        }
        val baseServer = profile?.server?.trimEnd('/') ?: return null
        val user = profile?.user?.trim() ?: return null
        val pass = profile?.pass?.trim() ?: return null
        val encUser = URLEncoder.encode(user, "UTF-8")
        val encPass = URLEncoder.encode(pass, "UTF-8")
        return "$baseServer/xmltv.php?username=$encUser&password=$encPass"
    }

    private fun parseXmltvStream(reader: java.io.Reader): Map<String, List<XmltvProgramme>> {
        val byChannelId = linkedMapOf<String, MutableList<XmltvProgramme>>()
        val aliasToChannelId = linkedMapOf<String, String>()
        try {
            val parser = org.xmlpull.v1.XmlPullParserFactory.newInstance().newPullParser()
            parser.setInput(reader)

            var eventType = parser.eventType
            var inChannel = false
            var currentChannelId = ""
            var inDisplayName = false
            var displayNameText = ""

            var inProgramme = false
            var channelAttr = ""
            var startMs = 0L
            var stopMs = 0L
            var startFmt = ""
            var endFmt = ""
            var titleText = ""
            var inTitle = false

            while (eventType != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    org.xmlpull.v1.XmlPullParser.START_TAG -> {
                        when (parser.name.lowercase()) {
                            "channel" -> {
                                inChannel = true
                                currentChannelId = normalizeKey(parser.getAttributeValue(null, "id").orEmpty())
                                displayNameText = ""
                            }
                            "display-name" -> if (inChannel) inDisplayName = true
                            "programme" -> {
                                inProgramme = true
                                channelAttr = normalizeKey(parser.getAttributeValue(null, "channel").orEmpty())
                                val startRaw = parser.getAttributeValue(null, "start").orEmpty()
                                val stopRaw = parser.getAttributeValue(null, "stop").orEmpty()
                                startMs = parseXmltvDateToEpochMs(startRaw)
                                stopMs = parseXmltvDateToEpochMs(stopRaw)
                                startFmt = formatXmltvClock(startRaw)
                                endFmt = formatXmltvClock(stopRaw)
                                titleText = ""
                            }
                            "title" -> if (inProgramme) inTitle = true
                        }
                    }
                    org.xmlpull.v1.XmlPullParser.TEXT -> {
                        val txt = parser.text.orEmpty()
                        if (inChannel && inDisplayName) displayNameText += txt
                        if (inProgramme && inTitle) titleText += txt
                    }
                    org.xmlpull.v1.XmlPullParser.END_TAG -> {
                        when (parser.name.lowercase()) {
                            "display-name" -> {
                                if (inChannel && currentChannelId.isNotBlank()) {
                                    val normAlias = normalizeKey(displayNameText)
                                    if (normAlias.isNotBlank()) {
                                        aliasToChannelId[normAlias] = currentChannelId
                                    }
                                }
                                inDisplayName = false
                                displayNameText = ""
                            }
                            "channel" -> {
                                inChannel = false
                                inDisplayName = false
                                currentChannelId = ""
                            }
                            "title" -> inTitle = false
                            "programme" -> {
                                if (inProgramme && channelAttr.isNotBlank() && titleText.isNotBlank()) {
                                    byChannelId.getOrPut(channelAttr) { mutableListOf() }.add(
                                        XmltvProgramme(
                                            channelKey = channelAttr,
                                            title = titleText.trim(),
                                            startEpochMs = startMs,
                                            stopEpochMs = stopMs,
                                            startFormatted = startFmt,
                                            endFormatted = endFmt
                                        )
                                    )
                                }
                                inProgramme = false
                                inTitle = false
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (_: Exception) {
            // Ignora XML malformado ou interrompido
        }

        val merged = linkedMapOf<String, MutableList<XmltvProgramme>>()
        byChannelId.forEach { (chKey, progs) ->
            merged.getOrPut(chKey) { mutableListOf() }.addAll(progs)
        }
        aliasToChannelId.forEach { (aliasKey, chKey) ->
            byChannelId[chKey]?.let { progs ->
                merged.getOrPut(aliasKey) { mutableListOf() }.addAll(progs)
            }
        }
        return merged.mapValues { (_, list) -> list.sortedBy { it.startEpochMs } }
    }

    private fun parseXmltvDateToEpochMs(raw: String): Long {
        val trimmed = raw.trim()
        val digits = trimmed.takeWhile { it.isDigit() }
        if (digits.length < 12) return 0L
        return try {
            val tzMatch = Regex("""([+-]\d{4})""").find(trimmed)
            val tzId = tzMatch?.groupValues?.getOrNull(1)?.let { "GMT$it" } ?: "UTC"
            val sdf = SimpleDateFormat("yyyyMMddHHmmss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone(tzId)
            }
            val padded = digits.padEnd(14, '0').take(14)
            sdf.parse(padded)?.time ?: 0L
        } catch (_: Exception) {
            0L
        }
    }

    private fun formatXmltvClock(raw: String): String {
        val digits = raw.takeWhile { it.isDigit() }
        if (digits.length < 12) return ""
        val hh = digits.substring(8, 10)
        val mm = digits.substring(10, 12)
        return "$hh:$mm"
    }

    private fun normalizeKey(input: String): String {
        return input.lowercase()
            .replace(Regex("[^a-z0-9]"), "")
            .trim()
    }

    private fun extractStreamIdFromUrl(streamUrl: String?): String? {
        if (streamUrl.isNullOrBlank()) return null
        val clean = streamUrl.substringBefore("?")
        val seg = clean.substringAfterLast("/").substringBefore(".")
        return seg.takeIf { it.isNotBlank() && it.all { ch -> ch.isDigit() } }
    }

    private fun parseShortEpgJson(rawJson: String): List<EpgProgramItem> {
        return try {
            val root = JSONObject(rawJson)
            val listings = root.optJSONArray("epg_listings") ?: return emptyList()
            val items = mutableListOf<EpgProgramItem>()
            for (i in 0 until listings.length()) {
                val obj = listings.optJSONObject(i) ?: continue
                val rawTitle = decodeBase64IfNeeded(obj.optString("title")).trim()
                if (rawTitle.isBlank()) continue
                val startRaw = obj.optString("start").ifBlank { obj.optString("start_timestamp") }
                val endRaw = obj.optString("end").ifBlank { obj.optString("stop_timestamp") }
                val startFmt = formatEpgTime(startRaw)
                val endFmt = formatEpgTime(endRaw)
                val hasNow = obj.optInt("has_now", 0) == 1 || obj.optString("now_playing") == "1"
                items.add(
                    EpgProgramItem(
                        title = rawTitle,
                        startTime = startFmt,
                        endTime = endFmt,
                        isCurrent = hasNow || (i == 0 && listings.length() > 0)
                    )
                )
                if (items.size >= 4) break
            }
            if (items.isNotEmpty() && items.none { it.isCurrent }) {
                items[0] = items[0].copy(isCurrent = true)
            }
            items
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun decodeBase64IfNeeded(value: String): String {
        if (value.isBlank()) return ""
        return try {
            val decoded = android.util.Base64.decode(value, android.util.Base64.DEFAULT)
            val str = String(decoded, Charsets.UTF_8)
            if (str.isNotBlank() && str.all { it.code in 32..126 || it.code >= 160 }) str else value
        } catch (_: Exception) {
            value
        }
    }

    private fun formatEpgTime(raw: String): String {
        if (raw.isBlank()) return ""
        if (raw.all { it.isDigit() } && raw.length >= 10) {
            val sec = raw.take(10).toLongOrNull() ?: return ""
            val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
            return fmt.format(Date(sec * 1000L))
        }
        val timeMatch = Regex("(\\d{2}:\\d{2})").find(raw)
        return timeMatch?.groupValues?.getOrNull(1) ?: ""
    }

    fun formatProfilesExportTxt(profiles: List<ProfileEntity>): String {
        val lines = mutableListOf("# IPTV Hero Pro Profiles")
        for (p in profiles) {
            lines.add(
                listOf(
                    p.sourceType,
                    p.sourceLabel,
                    p.server.orEmpty(),
                    p.user.orEmpty(),
                    p.pass.orEmpty(),
                    p.validityIso.orEmpty(),
                    p.maxConnections?.toString().orEmpty(),
                    p.name,
                    p.liveCount.toString(),
                    p.vodCount.toString(),
                    p.seriesCount.toString()
                ).joinToString("|")
            )
        }
        return lines.joinToString("\n")
    }

    private fun fetchString(url: String): String? {
        val request = Request.Builder().url(url).build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            return response.body?.string()
        }
    }

    private fun fetchJsonObject(url: String): JSONObject? {
        val raw = fetchString(url) ?: return null
        return runCatching { JSONObject(raw) }.getOrNull()
    }

    private fun fetchJsonArray(url: String): JSONArray? {
        val raw = fetchString(url) ?: return null
        return runCatching { JSONArray(raw) }.getOrNull()
    }

    private fun JSONArray?.toCategoryMap(): Map<String, String> {
        if (this == null) return emptyMap()
        val map = mutableMapOf<String, String>()
        for (i in 0 until length()) {
            val obj = optJSONObject(i) ?: continue
            val id = obj.optString("category_id")
            val name = obj.optString("category_name")
            if (id.isNotBlank() && name.isNotBlank()) {
                map[id] = name
            }
        }
        return map
    }
}
```[cite: 13, 41]
