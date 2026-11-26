package com.iptvhero.pro.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iptvhero.pro.R
import com.iptvhero.pro.ui.theme.DcBlue
import com.iptvhero.pro.ui.theme.DcCyan
import com.iptvhero.pro.ui.theme.MarvelRed
import com.iptvhero.pro.ui.theme.MarvelYellow
import com.iptvhero.pro.ui.theme.TextDim
import com.iptvhero.pro.ui.theme.TextPrimary

@Composable
fun SplashScreen(
    statusText: String = "CARREGANDO UNIVERSO...",
    progress: Float = 0.15f,
    errorMessage: String? = null,
    onRetry: () -> Unit = {},
    onContinueOffline: (() -> Unit)? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "comic_splash")
    val badgeScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "badge_scale"
    )

    val comicInk = Color(0xFF06070C)

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Imagem splash_background cobrindo 100% da tela
        Image(
            painter = painterResource(id = R.drawable.splash_background),
            contentDescription = "IPTV Hero Pro Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Camada sutil de contraste para garantir leitura dos textos sobre a imagem
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0x5506070C),
                            Color(0x3306070C),
                            Color(0x8806070C)
                        )
                    )
                )
        )

        // Conteúdo textual e barra de progresso sobre o background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // Selo de edição no canto superior esquerdo
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .rotate(-3f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MarvelYellow)
                    .border(3.dp, comicInk, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "ISSUE #01 • MULTIVERSE",
                    color = comicInk,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    letterSpacing = 1.2.sp
                )
            }

            // Balão de impacto no canto superior direito
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .rotate(4f)
                    .scale(badgeScale)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MarvelRed)
                    .border(3.dp, comicInk, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "ACTION TV!",
                    color = MarvelYellow,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp
                )
            }

            // Textos centrais (sem splash_logo e sem raios/quadros ao redor)
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .rotate(-1.5f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(MarvelRed, Color(0xE6141B2E), DcBlue)
                            )
                        )
                        .border(3.5.dp, comicInk, RoundedCornerShape(10.dp))
                        .padding(horizontal = 24.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "IPTV HERO PRO+",
                        color = MarvelYellow,
                        fontWeight = FontWeight.Black,
                        fontSize = 28.sp,
                        letterSpacing = 3.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xE60A0E1A))
                        .border(2.5.dp, comicInk, RoundedCornerShape(8.dp))
                        .border(1.dp, DcCyan.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LIVE TV",
                        color = MarvelYellow,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        letterSpacing = 1.5.sp
                    )
                    Text("•", color = TextDim, fontSize = 11.sp)
                    Text(
                        text = "MOVIES",
                        color = DcCyan,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        letterSpacing = 1.5.sp
                    )
                    Text("•", color = TextDim, fontSize = 11.sp)
                    Text(
                        text = "SERIES",
                        color = TextPrimary,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        letterSpacing = 1.5.sp
                    )
                }
            }

            // Rodapé com legenda de status e barra de progresso
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .offset(y = (-2).dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (errorMessage != null) MarvelRed else MarvelYellow)
                        .border(2.5.dp, comicInk, RoundedCornerShape(6.dp))
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = errorMessage ?: statusText.uppercase(),
                        color = if (errorMessage != null) Color.White else comicInk,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        letterSpacing = 1.1.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (errorMessage == null) {
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0.02f, 1f) },
                        modifier = Modifier
                            .width(300.dp)
                            .height(7.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .border(1.5.dp, comicInk, RoundedCornerShape(6.dp)),
                        color = MarvelYellow,
                        trackColor = Color.White.copy(alpha = 0.24f)
                    )
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onRetry,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MarvelYellow,
                                contentColor = comicInk
                            )
                        ) {
                            Text("Tentar novamente", fontWeight = FontWeight.Black)
                        }
                        if (onContinueOffline != null) {
                            Button(
                                onClick = onContinueOffline,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DcBlue,
                                    contentColor = Color.White
                                )
                            ) {
                                Text("Abrir perfis", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
