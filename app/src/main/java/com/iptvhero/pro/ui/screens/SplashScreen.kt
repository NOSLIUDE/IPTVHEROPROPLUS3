package com.iptvhero.pro.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iptvhero.pro.ui.theme.DcCyan
import com.iptvhero.pro.ui.theme.GoldFocus
import com.iptvhero.pro.ui.theme.HeroThemeType
import com.iptvhero.pro.ui.theme.MarvelYellow
import com.iptvhero.pro.ui.theme.TextPrimary
import com.iptvhero.pro.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    viewModel: MainViewModel,
    currentTheme: HeroThemeType = HeroThemeType.DUEL,
    onFinishedToProfiles: () -> Unit
) {
    val profiles by viewModel.profiles.collectAsState(initial = emptyList())
    val uiState by viewModel.uiState.collectAsState()

    var showChooseHeroMessage by remember { mutableStateOf(false) }
    var hasNavigated by remember { mutableStateOf(false) }

    LaunchedEffect(profiles, uiState.isProfilesLoading) {
        val profilesLoaded = !uiState.isProfilesLoading
        if (profilesLoaded && !hasNavigated) {
            delay(300)
            showChooseHeroMessage = true
            delay(1200)
            hasNavigated = true
            onFinishedToProfiles()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(currentTheme.backgroundBrush)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "IPTV HERO PRO+",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "MARVEL vs DC EDITION",
                color = DcCyan,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(26.dp))

            if (!showChooseHeroMessage) {
                CircularProgressIndicator(color = GoldFocus)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Carregando perfis de heróis...",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            AnimatedVisibility(
                visible = showChooseHeroMessage,
                enter = fadeIn(animationSpec = tween(400)) + scaleIn(initialScale = 0.9f)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xD90A0F1D))
                        .border(2.dp, GoldFocus, RoundedCornerShape(14.dp))
                        .padding(horizontal = 28.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = "Escolha seu Herói!",
                        color = MarvelYellow,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
