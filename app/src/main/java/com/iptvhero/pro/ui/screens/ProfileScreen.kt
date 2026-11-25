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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iptvhero.pro.ui.components.ProfileManagerDialog
import com.iptvhero.pro.ui.components.heroFocusable
import com.iptvhero.pro.ui.theme.DcCyan
import com.iptvhero.pro.ui.theme.GoldFocus
import com.iptvhero.pro.ui.theme.HeroThemeType
import com.iptvhero.pro.ui.theme.MarvelYellow
import com.iptvhero.pro.ui.theme.TextDim
import com.iptvhero.pro.ui.theme.TextPrimary
import com.iptvhero.pro.ui.viewmodel.MainViewModel

@Composable
fun ProfileScreen(
    viewModel: MainViewModel,
    currentTheme: HeroThemeType = HeroThemeType.DUEL,
    onCycleTheme: () -> Unit = {},
    onSelectProfile: (Long) -> Unit,
    onLoadHeroList: (String) -> Unit,
    onImportXtream: (String, String, String) -> Unit,
    onImportBatchTxt: (String) -> Unit
) {
    val profiles by viewModel.profiles.collectAsState(initial = emptyList())
    val uiState by viewModel.uiState.collectAsState()
    var showManagerDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(currentTheme.backgroundBrush)
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(currentTheme.headerBrush)
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "IPTV HERO PRO+",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Escolha seu Herói ou selecione uma lista para entrar automaticamente",
                    color = Color.White.copy(alpha = 0.88f),
                    fontSize = 11.5.sp
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x99090B14))
                        .heroFocusable { showManagerDialog = true }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text("+ Lista / Xtream", color = DcCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x99090B14))
                        .heroFocusable { onCycleTheme() }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text("Tema", color = MarvelYellow, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "ESCOLHA SEU HERÓI!",
                color = MarvelYellow,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black
            )

            if (uiState.isLoading) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xCC0C1120))
                        .padding(18.dp)
                ) {
                    CircularProgressIndicator(color = DcCyan)
                    Text(
                        text = uiState.loadingStage.ifBlank { "Entrando no aplicativo..." },
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    LinearProgressIndicator(
                        progress = { uiState.loadingProgress.coerceIn(0.1f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                        color = GoldFocus
                    )
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(profiles, key = { it.id }) { profile ->
                        val badgeColor = when (profile.heroAvatar.uppercase()) {
                            "IRONMAN", "MARVEL" -> Color(0xFFE62429)
                            "BATMAN", "DC" -> Color(0xFF0476F2)
                            else -> Color(0xFF6A1B9A)
                        }
                        Column(
                            modifier = Modifier
                                .width(185.dp)
                                .height(205.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xE6101628))
                                .heroFocusable(RoundedCornerShape(16.dp)) {
                                    onSelectProfile(profile.id)
                                }
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(badgeColor)
                                    .border(2.dp, GoldFocus, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = profile.name.take(2).uppercase(),
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = profile.name,
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = profile.sourceType,
                                color = DcCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "OK: ENTRAR",
                                color = MarvelYellow,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    item {
                        Column(
                            modifier = Modifier
                                .width(185.dp)
                                .height(205.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xE6181018))
                                .heroFocusable(RoundedCornerShape(16.dp)) {
                                    onLoadHeroList(MainViewModel.DEFAULT_HERO_URL)
                                }
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE62429))
                                    .border(2.dp, MarvelYellow, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("★", color = MarvelYellow, fontSize = 28.sp, fontWeight = FontWeight.Black)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "HERO LISTA",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Entrar Direto",
                                color = MarvelYellow,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Text(
            text = "Navegue com o D-Pad e pressione OK no perfil ou lista para entrar diretamente no app",
            color = TextDim,
            fontSize = 11.sp
        )
    }

    if (showManagerDialog) {
        ProfileManagerDialog(
            profiles = profiles,
            activeProfileId = viewModel.activeProfile?.id,
            onDismiss = { showManagerDialog = false },
            onActivateProfile = { id ->
                showManagerDialog = false
                onSelectProfile(id)
            },
            onDeleteProfile = { viewModel.deleteProfile(it) },
            onImportContent = { txt ->
                showManagerDialog = false
                onImportBatchTxt(txt)
            },
            onImportGithubHero = { url ->
                showManagerDialog = false
                onLoadHeroList(url)
            },
            onImportXtream = { server, user, pass ->
                showManagerDialog = false
                onImportXtream(server, user, pass)
            }
        )
    }
}
