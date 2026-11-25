package com.iptvhero.pro.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.iptvhero.pro.ui.components.heroFocusable
import com.iptvhero.pro.ui.theme.DcCyan
import com.iptvhero.pro.ui.theme.MarvelYellow
import com.iptvhero.pro.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay

@Composable
fun FullPlayerScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var showOverlay by remember { mutableStateOf(true) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context.applicationContext).build()
    }

    LaunchedEffect(uiState.currentStreamUrl) {
        val url = uiState.currentStreamUrl
        if (!url.isNullOrBlank()) {
            exoPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(url)))
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
            showOverlay = true
        }
    }

    LaunchedEffect(showOverlay) {
        if (showOverlay) {
            delay(4000)
            showOverlay = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp) {
                    showOverlay = true
                    when (event.key) {
                        Key.ChannelUp, Key.DirectionUp -> {
                            val next = uiState.playlistCurrentIndex + 1
                            if (next < uiState.playlistUrls.size) {
                                viewModel.switchPlaylistIndex(next)
                            }
                            true
                        }
                        Key.ChannelDown, Key.DirectionDown -> {
                            val prev = uiState.playlistCurrentIndex - 1
                            if (prev >= 0) {
                                viewModel.switchPlaylistIndex(prev)
                            }
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (showOverlay) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xCC0A0E1A))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = uiState.currentChannelTitle.ifBlank { "Reprodução Ao Vivo" },
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Cima / Baixo no controle troca de canal • Voltar retorna à grade",
                            color = DcCyan,
                            fontSize = 11.sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF151C2E))
                                .heroFocusable {
                                    val prev = (uiState.playlistCurrentIndex - 1).coerceAtLeast(0)
                                    viewModel.switchPlaylistIndex(prev)
                                }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text("◀ Anterior", color = MarvelYellow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF151C2E))
                                .heroFocusable {
                                    val next = (uiState.playlistCurrentIndex + 1)
                                        .coerceAtMost((uiState.playlistUrls.size - 1).coerceAtLeast(0))
                                    viewModel.switchPlaylistIndex(next)
                                }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text("Próximo ▶", color = MarvelYellow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF3B1219))
                                .heroFocusable { onBack() }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text("SAIR", color = Color(0xFFFF6B6E), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
