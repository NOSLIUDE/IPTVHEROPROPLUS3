package com.iptvhero.pro.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iptvhero.pro.data.local.CardHeroCatalog
import com.iptvhero.pro.data.local.CardHeroSpec
import com.iptvhero.pro.data.local.CardHeroUniverse
import com.iptvhero.pro.data.local.ProfileEntity
import com.iptvhero.pro.ui.components.heroFocusable
import com.iptvhero.pro.ui.theme.DcBlue
import com.iptvhero.pro.ui.theme.GoldFocus
import com.iptvhero.pro.ui.theme.HeroThemeType
import com.iptvhero.pro.ui.theme.MarvelRed
import com.iptvhero.pro.ui.theme.MarvelYellow
import com.iptvhero.pro.ui.theme.TextDim
import java.io.IOException
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private fun resolveCardHeroUniverse(profile: ProfileEntity, hero: CardHeroSpec): CardHeroUniverse {
    return CardHeroUniverse.fromRaw(profile.universe) ?: hero.universe
}

private fun resolveStrictHeroLabel(profile: ProfileEntity, hero: CardHeroSpec, index: Int): String {
    val trimmed = profile.name.trim().uppercase()
    val match = Regex("^([A-Z0-9]+)_(\\d+)$").matchEntire(trimmed)
    if (match != null) {
        val prefix = match.groupValues[1]
        val numStr = match.groupValues[2]
        val matchedHero = CardHeroCatalog.findMatch(prefix)
        if (matchedHero != null) {
            return "${matchedHero.baseTag}_$numStr"
        }
    }
    return CardHeroCatalog.formatHeroNumberedName(hero, index + 1)
}

private fun cardOverlayBrush(isMarvel: Boolean): Brush {
    return if (isMarvel) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0x66991B1B),
                Color(0xB8450A0A),
                Color(0xD978350F)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0x591D4ED8),
                Color(0xB01E3A8A),
                Color(0xCCDDEAF8)
            )
        )
    }
}

@Composable
private fun rememberHeroCardBitmap(assetFileName: String): ImageBitmap? {
    val context = LocalContext.current
    return remember(assetFileName) {
        try {
            context.assets.open("avatars/$assetFileName").use { stream ->
                BitmapFactory.decodeStream(stream)?.asImageBitmap()
            }
        } catch (_: IOException) {
            null
        }
    }
}

@Composable
fun ProfilesScreen(
    profiles: List<ProfileEntity>,
    activeProfileId: Long?,
    statusMessage: String?,
    pendingHeroMessage: String? = null,
    currentTheme: HeroThemeType,
    onCycleTheme: () -> Unit,
    onOpenImportTxt: () -> Unit,
    onLoadHeroGithub: () -> Unit,
    onConfirmPendingHero: () -> Unit = {},
    onDismissPendingHero: () -> Unit = {},
    onSelectProfile: (Long) -> Unit,
    onDeleteProfile: (Long) -> Unit = {},
    onClearAllProfiles: () -> Unit,
    onEnterApp: () -> Unit
) {
    val sortedProfiles = remember(profiles) {
        profiles.sortedWith(
            compareBy<ProfileEntity> { extractProfileHost(it) }
                .thenByDescending { parseRemainingDays(it.validityIso) ?: Long.MIN_VALUE }
                .thenBy { it.name.lowercase() }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(currentTheme.backgroundBrush)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0x8C07070F))
                .padding(horizontal = 24.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "IPTV HERO PRO+",
                color = MarvelYellow,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.weight(1f))
            HeroActionButton("TEMA", isPrimary = false, onClick = onCycleTheme)
            HeroActionButton("HERO LISTA", isPrimary = false, onClick = onLoadHeroGithub)
            HeroActionButton("XTREAM / TXT", isPrimary = false, onClick = onOpenImportTxt)
            HeroActionButton("ENTRAR", isPrimary = true, onClick = onEnterApp)
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

        if (sortedProfiles.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = statusMessage ?: "Nenhum perfil ainda.\nClique em HERO LISTA ou XTREAM / TXT para adicionar listas.",
                    color = TextDim,
                    fontSize = 15.sp
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 270.dp),
                modifier = Modifier
                    .weight(1f)
                    .padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                itemsIndexed(sortedProfiles, key = { _, profile -> profile.id }) { index, profile ->
                    val heroSpec = remember(profile.id, profile.avatarKey, profile.name, index) {
                        CardHeroCatalog.resolveFrom16(index, profile.avatarKey, profile.name)
                    }
                    val heroTitle = remember(profile.id, profile.name, heroSpec, index) {
                        resolveStrictHeroLabel(profile, heroSpec, index)
                    }
                    val isActive = profile.id == activeProfileId
                    val isMarvel = resolveCardHeroUniverse(profile, heroSpec) == CardHeroUniverse.MARVEL
                    val cardBitmap = rememberHeroCardBitmap(heroSpec.assetFileName)
                    val borderColor = if (isMarvel) {
                        MarvelYellow.copy(alpha = 0.85f)
                    } else {
                        Color.White.copy(alpha = 0.85f)
                    }
                    val primaryTextColor = if (isMarvel) Color.White else Color(0xFF0F172A)
                    val secondaryTextColor = if (isMarvel) Color(0xFFFEF3C7) else Color(0xFF1E293B)
                    val validityInfo = formatValidity(profile.validityIso)

                    Box(
                        modifier = Modifier
                            .height(195.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                width = if (isActive) 2.5.dp else 1.25.dp,
                                color = if (isActive) GoldFocus else borderColor,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .heroFocusable(RoundedCornerShape(16.dp)) {
                                onSelectProfile(profile.id)
                            }
                    ) {
                        if (cardBitmap != null) {
                            Image(
                                bitmap = cardBitmap,
                                contentDescription = heroTitle,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(cardOverlayBrush(isMarvel))
                                .padding(14.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (isActive) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color.Black.copy(alpha = 0.72f))
                                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                            ) {
                                                Text(
                                                    text = "ATIVO",
                                                    color = GoldFocus,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                            }
                                        }

                                        Box(
                                            modifier = Modifier
                                                .size(26.dp)
                                                .clip(CircleShape)
                                                .background(MarvelRed.copy(alpha = 0.92f))
                                                .heroFocusable(CircleShape) { onDeleteProfile(profile.id) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "X",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isMarvel) Color(0xCC2A070B)
                                            else Color(0xD9EFF6FF)
                                        )
                                        .padding(horizontal = 12.dp, vertical = 9.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text(
                                            text = heroTitle,
                                            color = primaryTextColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        val hostLabel = extractProfileHost(profile).ifBlank { "sem-host" }
                                        Text(
                                            text = "$hostLabel • ${profile.sourceType.uppercase()}${if (profile.bulkOrigin) " • LOTE" else ""}",
                                            color = secondaryTextColor,
                                            fontSize = 10.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            letterSpacing = 0.4.sp
                                        )
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "TV ${profile.liveCount}",
                                                color = if (isMarvel) MarvelYellow else DcBlue,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "VOD ${profile.vodCount}",
                                                color = if (isMarvel) Color(0xFFFDE68A) else Color(0xFF0369A1),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "SER ${profile.seriesCount}",
                                                color = primaryTextColor,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "CON ${profile.activeConnections}/${profile.maxConnections ?: "∞"}",
                                                color = secondaryTextColor,
                                                fontSize = 11.sp
                                            )
                                        }
                                        Text(
                                            text = validityInfo.first,
                                            color = validityInfo.second,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xA607070F))
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HeroActionButton("CARREGAR HERO LISTA", isPrimary = true, onClick = onLoadHeroGithub)
            HeroActionButton("IMPORTAR LOTE TXT", isPrimary = false, onClick = onOpenImportTxt)
            HeroActionButton("LIMPAR TUDO", isPrimary = false, onClick = onClearAllProfiles)
            if (!statusMessage.isNullOrBlank()) {
                Text(
                    text = statusMessage,
                    color = GoldFocus,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    if (!pendingHeroMessage.isNullOrBlank()) {
        AlertDialog(
            onDismissRequest = onDismissPendingHero,
            title = { Text("HERO LISTA") },
            text = { Text(pendingHeroMessage) },
            confirmButton = {
                Button(onClick = onConfirmPendingHero) {
                    Text("Reprocessar")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissPendingHero) {
                    Text("Cancelar")
                }
            }
        )
    }
}

private fun parseRemainingDays(validityIso: String?): Long? {
    if (validityIso.isNullOrBlank()) return null
    return try {
        val exp = Instant.parse(validityIso)
        ChronoUnit.DAYS.between(Instant.now(), exp)
    } catch (_: Exception) {
        null
    }
}

private fun extractProfileHost(profile: ProfileEntity): String {
    val rawCandidate = listOfNotNull(
        profile.server?.takeIf { it.isNotBlank() },
        profile.sourceLabel.takeIf { it.isNotBlank() }
    ).firstOrNull().orEmpty().trim()

    if (rawCandidate.isBlank()) return ""

    val normalized = if (rawCandidate.contains("://")) rawCandidate else "http://$rawCandidate"
    val parsedHost = runCatching { Uri.parse(normalized).host }.getOrNull().orEmpty()
    val cleanHost = parsedHost.ifBlank {
        rawCandidate
            .substringAfter("://", rawCandidate)
            .substringBefore("/")
            .substringBefore("?")
            .substringBefore(":")
    }
    return cleanHost.removePrefix("www.").lowercase().trim()
}

private fun formatValidity(validityIso: String?): Pair<String, Color> {
    if (validityIso.isNullOrBlank()) return Pair("Sem validade", TextDim)
    return try {
        val exp = Instant.parse(validityIso)
        val now = Instant.now()
        val days = ChronoUnit.DAYS.between(now, exp)
        val formatted = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            .withZone(ZoneId.systemDefault())
            .format(exp)
        when {
            days < 0 -> Pair("Expirado ($formatted)", Color(0xFFFF6B6E))
            days <= 7 -> Pair("${days}d restantes ($formatted)", Color(0xFFFFB347))
            else -> Pair("${days}d • Válido até $formatted", Color(0xFF15803D))
        }
    } catch (_: Exception) {
        Pair("Sem validade", TextDim)
    }
}

@Composable
private fun HeroActionButton(text: String, isPrimary: Boolean, onClick: () -> Unit) {
    val bgBrush = if (isPrimary) {
        Brush.horizontalGradient(listOf(MarvelRed, DcBlue))
    } else {
        Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.08f)))
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bgBrush)
            .heroFocusable(RoundedCornerShape(10.dp), onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}
