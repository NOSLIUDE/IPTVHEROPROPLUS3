package com.iptvhero.pro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.iptvhero.pro.data.download.OfflineDownloadState
import com.iptvhero.pro.data.repository.MediaDetails
import com.iptvhero.pro.data.repository.SeriesEpisode
import com.iptvhero.pro.ui.components.heroFocusable
import com.iptvhero.pro.ui.theme.DcCyan
import com.iptvhero.pro.ui.theme.GoldFocus
import com.iptvhero.pro.ui.theme.HeroThemeType
import com.iptvhero.pro.ui.theme.MarvelRed
import com.iptvhero.pro.ui.theme.MarvelYellow
import com.iptvhero.pro.ui.theme.TextDim
import com.iptvhero.pro.ui.theme.TextPrimary

@Composable
fun MediaDetailsScreen(
    details: MediaDetails?,
    isLoading: Boolean,
    seasonsMap: Map<String, List<SeriesEpisode>>,
    downloadStates: Map<String, OfflineDownloadState>,
    movieDownloadKey: String,
    currentTheme: HeroThemeType,
    onWatchMovie: (streamUrl: String, title: String) -> Unit,
    onDownloadMovie: () -> Unit,
    onDeleteMovieOffline: () -> Unit,
    onWatchEpisode: (episode: SeriesEpisode, seasonKey: String, seasonEpisodes: List<SeriesEpisode>, index: Int) -> Unit,
    onDownloadEpisode: (episode: SeriesEpisode, seasonKey: String) -> Unit,
    onDeleteEpisodeOffline: (episode: SeriesEpisode) -> Unit,
    onCheckEpisodeOffline: (episode: SeriesEpisode) -> OfflineDownloadState,
    onBack: () -> Unit
) {
    var selectedSeason by remember(seasonsMap) {
        mutableStateOf(seasonsMap.keys.firstOrNull().orEmpty())
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(currentTheme.backgroundBrush)
    ) {
        details?.backdropUrl?.let { bg ->
            AsyncImage(
                model = bg,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = 0.22f,
                modifier = Modifier.fillMaxSize()
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xCC060913), Color(0xF2080C18))
                    )
                )
        )

        if (isLoading || details == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = GoldFocus)
                    Spacer(Modifier.height(12.dp))
                    Text("Carregando sinopse e informações...", color = TextDim, fontSize = 14.sp)
                }
            }
            return@Box
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF162038))
                        .heroFocusable(RoundedCornerShape(10.dp)) { onBack() }
                        .padding(horizontal = 16.dp, vertical = 9.dp)
                ) {
                    Text("◀ VOLTAR", color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                }

                Text(
                    text = if (details.contentType == "SERIES") "DETALHES DA SÉRIE" else "DETALHES DO FILME",
                    color = DcCyan,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    letterSpacing = 1.5.sp
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(170.dp)
                        .height(250.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF12192C))
                        .border(1.5.dp, GoldFocus.copy(alpha = 0.6f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!details.posterUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = details.posterUrl,
                            contentDescription = details.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = details.title.take(2).uppercase(),
                            color = GoldFocus,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = details.title,
                        color = TextPrimary,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MetaBadge(label = "IMDb ★ ${details.ratingImdb}", bgColor = Color(0xFF3A2D08), textColor = MarvelYellow)
                        MetaBadge(label = "Ano: ${details.year}", bgColor = Color(0xFF16223B), textColor = TextPrimary)
                        MetaBadge(label = "Lançamento: ${details.releaseDate}", bgColor = Color(0xFF16223B), textColor = TextDim)
                        MetaBadge(label = "Duração: ${details.duration}", bgColor = Color(0xFF16223B), textColor = DcCyan)
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(details.genres) { genre ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(MarvelRed.copy(alpha = 0.25f))
                                    .border(1.dp, MarvelRed, RoundedCornerShape(20.dp))
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(genre, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Text(
                        text = "SINOPSE",
                        color = GoldFocus,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = details.plot,
                        color = Color(0xFFD8E1F0),
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        maxLines = if (details.contentType == "SERIES") 3 else 5,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (details.contentType == "VOD") {
                        val dlState = downloadStates[movieDownloadKey] ?: OfflineDownloadState()
                        Spacer(Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MarvelRed)
                                    .heroFocusable(RoundedCornerShape(10.dp)) {
                                        val playUrl = dlState.localUri ?: details.streamUrl.orEmpty()
                                        if (playUrl.isNotBlank()) onWatchMovie(playUrl, details.title)
                                    }
                                    .padding(horizontal = 22.dp, vertical = 12.dp)
                            ) {
                                Text(
                                    text = if (dlState.isCompleted) "▶ ASSISTIR OFFLINE" else "▶ ASSISTIR AGORA",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (dlState.isCompleted) Color(0xFF14452F) else Color(0xFF182744))
                                    .heroFocusable(RoundedCornerShape(10.dp)) {
                                        if (dlState.isCompleted) onDeleteMovieOffline()
                                        else if (!dlState.isDownloading) onDownloadMovie()
                                    }
                                    .padding(horizontal = 18.dp, vertical = 12.dp)
                            ) {
                                val btnLabel = when {
                                    dlState.isCompleted -> "✓ BAIXADO (REMOVER)"
                                    dlState.isDownloading -> "BAIXANDO ${(dlState.progress * 100).toInt()}%"
                                    else -> "⬇ BAIXAR OFFLINE"
                                }
                                Text(btnLabel, color = DcCyan, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                            }
                        }

                        if (dlState.isDownloading) {
                            LinearProgressIndicator(
                                progress = { dlState.progress },
                                color = GoldFocus,
                                modifier = Modifier.fillMaxWidth(0.6f).height(5.dp).clip(RoundedCornerShape(4.dp))
                            )
                        }
                        dlState.error?.let {
                            Text(it, color = Color(0xFFFF6B6E), fontSize = 11.sp)
                        }
                    }
                }
            }

            if (details.contentType == "SERIES") {
                val seasonKeys = seasonsMap.keys.toList()
                val activeSeason = if (selectedSeason in seasonKeys) selectedSeason else seasonKeys.firstOrNull().orEmpty()
                val episodes = seasonsMap[activeSeason].orEmpty()

                if (seasonKeys.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(seasonKeys) { sKey ->
                            val active = sKey == activeSeason
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(if (active) GoldFocus.copy(alpha = 0.25f) else Color(0xFF141C30))
                                    .heroFocusable(RoundedCornerShape(9.dp)) { selectedSeason = sKey }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "Temporada $sKey",
                                    color = if (active) GoldFocus else TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(episodes, key = { "${activeSeason}_${it.id}_${it.episodeNum}" }) { ep ->
                        val epIdx = episodes.indexOf(ep).coerceAtLeast(0)
                        val epKey = "ep_${ep.id}"
                        val epDl = downloadStates[epKey] ?: onCheckEpisodeOffline(ep)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF131B2E))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "EP ${ep.episodeNum.toString().padStart(2, '0')} • ${ep.title}",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val sub = buildString {
                                    if (!ep.duration.isNullOrBlank()) append("Duração: ${ep.duration} ")
                                    if (epDl.isDownloading) append("• Baixando ${(epDl.progress * 100).toInt()}%")
                                    else if (epDl.isCompleted) append("• Disponível offline")
                                    else epDl.error?.let { append("• $it") }
                                }
                                if (sub.isNotBlank()) {
                                    Text(sub, color = if (epDl.isDownloading) GoldFocus else TextDim, fontSize = 11.sp)
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (epDl.isCompleted) Color(0xFF14452F) else Color(0xFF1D2B4A))
                                        .heroFocusable(RoundedCornerShape(8.dp)) {
                                            if (epDl.isCompleted) onDeleteEpisodeOffline(ep)
                                            else if (!epDl.isDownloading) onDownloadEpisode(ep, activeSeason)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 7.dp)
                                ) {
                                    val dlLabel = when {
                                        epDl.isCompleted -> "✓ OFFLINE"
                                        epDl.isDownloading -> "${(epDl.progress * 100).toInt()}%"
                                        else -> "⬇ BAIXAR"
                                    }
                                    Text(
                                        text = dlLabel,
                                        color = DcCyan,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MarvelRed)
                                        .heroFocusable(RoundedCornerShape(8.dp)) {
                                            onWatchEpisode(ep, activeSeason, episodes, epIdx)
                                        }
                                        .padding(horizontal = 14.dp, vertical = 7.dp)
                                ) {
                                    Text(
                                        text = if (epDl.isCompleted) "▶ OFFLINE" else "▶ ASSISTIR",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetaBadge(label: String, bgColor: Color, textColor: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(label, color = textColor, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
    }
}
