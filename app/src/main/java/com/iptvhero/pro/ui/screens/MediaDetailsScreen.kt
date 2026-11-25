package com.iptvhero.pro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.iptvhero.pro.ui.components.heroFocusable
import com.iptvhero.pro.ui.theme.DcCyan
import com.iptvhero.pro.ui.theme.GoldFocus
import com.iptvhero.pro.ui.theme.HeroThemeType
import com.iptvhero.pro.ui.theme.MarvelYellow
import com.iptvhero.pro.ui.theme.TextDim
import com.iptvhero.pro.ui.theme.TextPrimary
import com.iptvhero.pro.ui.viewmodel.MainViewModel

@Composable
fun MediaDetailsScreen(
    viewModel: MainViewModel,
    currentTheme: HeroThemeType = HeroThemeType.DUEL,
    onBack: () -> Unit,
    onPlayStream: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val media = uiState.selectedMediaChannel

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(currentTheme.backgroundBrush)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = media?.title ?: "Detalhes do Conteúdo",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF3B1219))
                    .heroFocusable { onBack() }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("VOLTAR", color = Color(0xFFFF6B6E), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        if (media != null) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xE60D111E))
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(200.dp)
                        .aspectRatio(0.72f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0A0F1D)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!media.streamIcon.isNullOrBlank()) {
                        AsyncImage(
                            model = media.streamIcon,
                            contentDescription = media.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(media.title.take(2).uppercase(), color = DcCyan, fontSize = 28.sp, fontWeight = FontWeight.Black)
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(text = "Categoria: ${media.groupName}", color = DcCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)

                    if (!media.contentType.equals("SERIES", ignoreCase = true)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF0476F2))
                                .heroFocusable {
                                    viewModel.startPlaylistPlayback(
                                        titles = listOf(media.title),
                                        urls = listOf(media.url),
                                        remoteIds = listOf(media.remoteId)
                                    )
                                    onPlayStream()
                                }
                                .padding(horizontal = 22.dp, vertical = 10.dp)
                        ) {
                            Text("▶ ASSISTIR AGORA", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                        }
                    } else {
                        Text("EPISÓDIOS DISPONÍVEIS", color = GoldFocus, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                        if (uiState.isLoadingSeasons) {
                            CircularProgressIndicator(color = DcCyan)
                        } else if (uiState.seasonsMap.isEmpty()) {
                            Text(
                                text = uiState.errorMessage ?: "Nenhum episódio carregado.",
                                color = MarvelYellow,
                                fontSize = 12.sp
                            )
                        } else {
                            val firstSeason = uiState.seasonsMap.keys.first()
                            val episodes = uiState.seasonsMap[firstSeason].orEmpty()
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                itemsIndexed(episodes) { idx, ep ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF141B2E))
                                            .heroFocusable {
                                                val urls = episodes.mapNotNull { viewModel.buildEpisodeStreamUrl(it) }
                                                val titles = episodes.map { "${media.title} • T${firstSeason}E${it.episodeNum}" }
                                                if (urls.isNotEmpty()) {
                                                    viewModel.startPlaylistPlayback(titles, urls, startIndex = idx)
                                                    onPlayStream()
                                                }
                                            }
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "T${firstSeason}E${ep.episodeNum} - ${ep.title}",
                                            color = TextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text("ASSISTIR ▶", color = DcCyan, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Use D-Pad + OK para iniciar a reprodução em tela cheia", color = TextDim, fontSize = 11.sp)
                }
            }
        }
    }
}
