package com.iptvhero.pro.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val DcCyan = Color(0xFF00E5FF)
val MarvelYellow = Color(0xFFFFD600)
val GoldFocus = Color(0xFFFFC107)
val TextPrimary = Color(0xFFF4F7FF)
val TextDim = Color(0xFF9AA7C7)

enum class HeroThemeType(
    val displayName: String,
    val backgroundBrush: Brush,
    val headerBrush: Brush,
    val accentColor: Color
) {
    DUEL(
        displayName = "Marvel vs DC",
        backgroundBrush = Brush.linearGradient(
            listOf(Color(0xFF1A060A), Color(0xFF070A14), Color(0xFF04182B))
        ),
        headerBrush = Brush.horizontalGradient(
            listOf(Color(0xFFB71C1C), Color(0xFF111626), Color(0xFF0277BD))
        ),
        accentColor = GoldFocus
    ),
    MARVEL(
        displayName = "Marvel Hero",
        backgroundBrush = Brush.verticalGradient(
            listOf(Color(0xFF23070B), Color(0xFF0B0D17))
        ),
        headerBrush = Brush.horizontalGradient(
            listOf(Color(0xFFD32F2F), Color(0xFF5D101D))
        ),
        accentColor = MarvelYellow
    ),
    DC(
        displayName = "DC Justice",
        backgroundBrush = Brush.verticalGradient(
            listOf(Color(0xFF04192C), Color(0xFF070B16))
        ),
        headerBrush = Brush.horizontalGradient(
            listOf(Color(0xFF01579B), Color(0xFF0D253F))
        ),
        accentColor = DcCyan
    )
}
