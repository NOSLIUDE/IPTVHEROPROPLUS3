package com.iptvhero.pro.data.repository

import com.iptvhero.pro.data.local.ChannelEntity
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class SeriesEpisode(
    val id: String,
    val episodeNum: Int,
    val title: String,
    val containerExtension: String = "mp4",
    val duration: String? = null
)

object PlaylistParser {
    val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    fun fetchText(url: String): String {
        val req = Request.Builder()
            .url(url.trim())
            .header("User-Agent", "IPTVHeroProPlus/2.0")
            .build()
        httpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IllegalStateException("HTTP ${resp.code}")
            return resp.body?.string().orEmpty()
        }
    }

    fun parseM3uOrTxt(raw: String, profileId: Long): List<ChannelEntity> {
        val lines = raw.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val result = mutableListOf<ChannelEntity>()
        var pendingTitle = ""
        var pendingGroup = "Geral"
        var pendingLogo: String? = null

        val groupRegex = Regex("""group-title="([^"]*)"""", RegexOption.IGNORE_CASE)
        val logoRegex = Regex("""tvg-logo="([^"]*)"""", RegexOption.IGNORE_CASE)

        for (line in lines) {
            when {
                line.startsWith("#EXTM3U", ignoreCase = true) -> Unit
                line.startsWith("#EXTINF", ignoreCase = true) -> {
                    pendingGroup = groupRegex.find(line)?.groupValues?.getOrNull(1)?.ifBlank { "Geral" } ?: "Geral"
                    pendingLogo = logoRegex.find(line)?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }
                    pendingTitle = line.substringAfterLast(",", "").trim().ifBlank { "Canal ${result.size + 1}" }
                }
                line.startsWith("#") -> Unit
                line.startsWith("http://", ignoreCase = true) || line.startsWith("https://", ignoreCase = true) -> {
                    val lower = line.lowercase()
                    val contentType = when {
                        lower.contains("/series/") -> "SERIES"
                        lower.contains("/movie/") || lower.endsWith(".mp4") || lower.endsWith(".mkv") || lower.endsWith(".avi") -> "VOD"
                        else -> "LIVE"
                    }
                    result.add(
                        ChannelEntity(
                            profileId = profileId,
                            title = pendingTitle.ifBlank { "Canal ${result.size + 1}" },
                            groupName = pendingGroup,
                            streamIcon = pendingLogo,
                            url = line,
                            contentType = contentType
                        )
                    )
                    pendingTitle = ""
                    pendingLogo = null
                }
                line.contains(",") && (line.contains("http://") || line.contains("https://")) -> {
                    val parts = line.split(",", limit = 2)
                    val title = parts[0].trim()
                    val url = parts[1].trim()
                    result.add(
                        ChannelEntity(
                            profileId = profileId,
                            title = title.ifBlank { "Canal ${result.size + 1}" },
                            groupName = "Lista TXT",
                            url = url,
                            contentType = "LIVE"
                        )
                    )
                }
            }
        }
        return result
    }

    fun fetchXtreamAll(server: String, user: String, pass: String, profileId: Long): List<ChannelEntity> {
        val cleanServer = server.trim().trimEnd('/')
        val baseApi = "$cleanServer/player_api.php?username=${user.trim()}&password=${pass.trim()}"
        val all = mutableListOf<ChannelEntity>()

        val liveCats = parseCategoriesMap(runCatching { fetchText("$baseApi&action=get_live_categories") }.getOrDefault("[]"))
        val liveJson = runCatching { JSONArray(fetchText("$baseApi&action=get_live_streams")) }.getOrDefault(JSONArray())
        for (i in 0 until liveJson.length()) {
            val obj = liveJson.optJSONObject(i) ?: continue
            val streamId = obj.optString("stream_id")
            if (streamId.isBlank()) continue
            val catId = obj.optString("category_id")
            all.add(
                ChannelEntity(
                    profileId = profileId,
                    remoteId = streamId,
                    title = obj.optString("name", "Canal $streamId"),
                    groupName = liveCats[catId] ?: "Ao Vivo",
                    streamIcon = obj.optString("stream_icon").takeIf { it.isNotBlank() },
                    url = "$cleanServer/live/${user.trim()}/${pass.trim()}/$streamId.ts",
                    contentType = "LIVE"
                )
            )
        }

        val vodCats = parseCategoriesMap(runCatching { fetchText("$baseApi&action=get_vod_categories") }.getOrDefault("[]"))
        val vodJson = runCatching { JSONArray(fetchText("$baseApi&action=get_vod_streams")) }.getOrDefault(JSONArray())
        for (i in 0 until vodJson.length()) {
            val obj = vodJson.optJSONObject(i) ?: continue
            val streamId = obj.optString("stream_id")
            if (streamId.isBlank()) continue
            val ext = obj.optString("container_extension", "mp4").ifBlank { "mp4" }
            val catId = obj.optString("category_id")
            all.add(
                ChannelEntity(
                    profileId = profileId,
                    remoteId = streamId,
                    title = obj.optString("name", "Filme $streamId"),
                    groupName = vodCats[catId] ?: "Filmes",
                    streamIcon = obj.optString("stream_icon").takeIf { it.isNotBlank() },
                    url = "$cleanServer/movie/${user.trim()}/${pass.trim()}/$streamId.$ext",
                    contentType = "VOD"
                )
            )
        }

        val seriesCats = parseCategoriesMap(runCatching { fetchText("$baseApi&action=get_series_categories") }.getOrDefault("[]"))
        val seriesJson = runCatching { JSONArray(fetchText("$baseApi&action=get_series")) }.getOrDefault(JSONArray())
        for (i in 0 until seriesJson.length()) {
            val obj = seriesJson.optJSONObject(i) ?: continue
            val seriesId = obj.optString("series_id")
            if (seriesId.isBlank()) continue
            val catId = obj.optString("category_id")
            all.add(
                ChannelEntity(
                    profileId = profileId,
                    remoteId = seriesId,
                    title = obj.optString("name", "Série $seriesId"),
                    groupName = seriesCats[catId] ?: "Séries",
                    streamIcon = obj.optString("cover").takeIf { it.isNotBlank() },
                    url = "xtream://series/$seriesId",
                    contentType = "SERIES"
                )
            )
        }

        return all
    }

    fun fetchSeriesSeasons(server: String, user: String, pass: String, seriesId: String): Map<String, List<SeriesEpisode>> {
        val cleanServer = server.trim().trimEnd('/')
        val url = "$cleanServer/player_api.php?username=${user.trim()}&password=${pass.trim()}&action=get_series_info&series_id=$seriesId"
        val json = JSONObject(fetchText(url))
        val episodesObj = json.optJSONObject("episodes") ?: return emptyMap()
        val map = linkedMapOf<String, List<SeriesEpisode>>()
        val keys = episodesObj.keys()
        while (keys.hasNext()) {
            val seasonKey = keys.next()
            val arr = episodesObj.optJSONArray(seasonKey) ?: continue
            val list = mutableListOf<SeriesEpisode>()
            for (i in 0 until arr.length()) {
                val ep = arr.optJSONObject(i) ?: continue
                list.add(
                    SeriesEpisode(
                        id = ep.optString("id"),
                        episodeNum = ep.optInt("episode_num", i + 1),
                        title = ep.optString("title", "Episódio ${i + 1}"),
                        containerExtension = ep.optString("container_extension", "mp4").ifBlank { "mp4" },
                        duration = ep.optJSONObject("info")?.optString("duration")?.takeIf { it.isNotBlank() }
                    )
                )
            }
            if (list.isNotEmpty()) {
                map[seasonKey] = list
            }
        }
        return map
    }

    private fun parseCategoriesMap(rawJson: String): Map<String, String> {
        val arr = runCatching { JSONArray(rawJson) }.getOrDefault(JSONArray())
        val map = mutableMapOf<String, String>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val id = obj.optString("category_id")
            val name = obj.optString("category_name")
            if (id.isNotBlank() && name.isNotBlank()) {
                map[id] = name
            }
        }
        return map
    }
}
