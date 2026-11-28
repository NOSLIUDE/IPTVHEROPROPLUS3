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
import kotlin.math.abs

private enum class ScreenHeroUniverse(val value: String) {
    MARVEL("marvel"),
    DC("dc");

    companion object {
        fun fromRaw(raw: String?): ScreenHeroUniverse? = when (raw?.trim()?.lowercase()) {
            "marvel" -> MARVEL
            "dc", "dc comics", "dc_comics" -> DC
            else -> null
        }
    }
}

private data class ScreenHeroCard(
    val key: String,
    val baseTag: String,
    val displayName: String,
    val assetFileName: String,
    val universe: ScreenHeroUniverse,
    val aliases: List<String> = emptyList()
)

private object ScreenHeroCatalog {
    val all16: List<ScreenHeroCard> = listOf(
        // DC (8)
        ScreenHeroCard("batman", "BATMAN", "Batman", "01_batman_0801.png", ScreenHeroUniverse.DC, listOf("batman_0801", "morcego")),
        ScreenHeroCard("superman", "SUPERMAN", "Superman", "02_superman_0001.png", ScreenHeroUniverse.DC, listOf("superman_0001", "super-homem", "super homem")),
        ScreenHeroCard("wwoman", "WWOMAN", "Wonder Woman", "03_wwoman_0901.png", ScreenHeroUniverse.DC, listOf("wwoman_0901", "wonder woman", "mulher-maravilha", "mulher maravilha")),
        ScreenHeroCard("flash", "FLASH", "Flash", "04_flash_0501.png", ScreenHeroUniverse.DC, listOf("flash_0501", "the flash")),
        ScreenHeroCard("aquaman", "AQUAMAN", "Aquaman", "05_aquaman_0701.png", ScreenHeroUniverse.DC, listOf("aquaman_0701")),
        ScreenHeroCard("glantern", "GLANTERN", "Green Lantern", "06_glantern_2814.png", ScreenHeroUniverse.DC, listOf("glantern_2814", "green lantern", "lanterna verde", "lanterna-verde")),
        ScreenHeroCard("garrow", "GARROW", "Green Arrow", "07_garrow_0101.png", ScreenHeroUniverse.DC, listOf("garrow_0101", "green arrow", "arqueiro verde", "arqueiro-verde")),
        ScreenHeroCard("cyborg", "CYBORG", "Cyborg", "08_cyborg_0001.png", ScreenHeroUniverse.DC, listOf("cyborg_0001")),

        // Marvel (8)
        ScreenHeroCard("ironman", "IRONMAN", "Iron Man", "09_ironman_0001.png", ScreenHeroUniverse.MARVEL, listOf("ironman_0001", "iron man", "homem de ferro", "homem-de-ferro")),
        ScreenHeroCard("captain", "CAPTAIN", "Captain America", "10_captain_0001.png", ScreenHeroUniverse.MARVEL, listOf("captain_0001", "captain america", "capitao america", "capitão américa", "capitao-america")),
        ScreenHeroCard("thor", "THOR", "Thor", "11_thor_0001.png", ScreenHeroUniverse.MARVEL, listOf("thor_0001")),
        ScreenHeroCard("hulk", "HULK", "Hulk", "12_hulk_0001.png", ScreenHeroUniverse.MARVEL, listOf("hulk_0001")),
        ScreenHeroCard("spider", "SPIDER", "Spider-Man", "13_spider_0001.png", ScreenHeroUniverse.MARVEL, listOf("spider_0001", "spiderman", "spider-man", "homem aranha", "homem-aranha")),
        ScreenHeroCard("bwidow", "BWIDOW", "Black Widow", "14_bwidow_0001.png", ScreenHeroUniverse.MARVEL, listOf("bwidow_0001", "black widow", "viuva negra", "viúva negra", "viuva-negra")),
        ScreenHeroCard("dstrange", "DSTRANGE", "Doctor Strange", "15_dstrange_0001.png", ScreenHeroUniverse.MARVEL, listOf("dstrange_0001", "doctor strange", "doutor estranho", "doutor-estranho")),
        ScreenHeroCard("bpanther", "BPANTHER", "Black Panther", "16_bpanther_0001.png", ScreenHeroUniverse.MARVEL, listOf("bpanther_0001", "black panther", "pantera negra", "pantera-negra"))
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

    fun findMatch(vararg candidates: String?): ScreenHeroCard? {
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

    fun resolveFrom16(seedIndex: Int, vararg candidates: String?): ScreenHeroCard {
        findMatch(*candidates)?.let { return it }
        val idx = abs(seedIndex) % all16.size
        return all16[idx]
    }

    fun formatHeroNumberedName(spec: ScreenHeroCard, sequenceNumber: Int): String {
        val safeNum = sequenceNumber.coerceAtLeast(1)
        return "${spec.baseTag}_${safeNum.toString().padStart(4, '0')}"
    }
}

private fun resolveCardHeroUniverse(profile: ProfileEntity, hero: ScreenHeroCard): ScreenHeroUniverse {
    return ScreenHeroUniverse.fromRaw(profile.universe) ?: hero.universe
}

private fun resolveStrictHeroLabel(profile: ProfileEntity, hero: ScreenHeroCard, index: Int): String {
    val trimmed = profile.name.trim().uppercase()
    val match = Regex("^([A-Z0-9]+)_(\\d+)$").matchEntire(trimmed)
    if (match != null) {
        val prefix = match.groupValues[1]
        val numStr = match.groupValues[2]
        val matchedHero = ScreenHeroCatalog.findMatch(prefix)
        if (matchedHero != null) {
            return "${matchedHero.baseTag}_$numStr"
        }
    }
    return ScreenHeroCatalog.formatHeroNumberedName(hero, index + 1)
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
                itemsIndexed(
                    items = sortedProfiles,
                    key = { _: Int, profile: ProfileEntity -> profile.id }
                ) { index: Int, profile: ProfileEntity ->
                    val heroSpec = remember(profile.id, profile.avatarKey, profile.name, index) {
                        ScreenHeroCatalog.resolveFrom16(index, profile.avatarKey, profile.name)
                    }
                    val heroTitle = remember(profile.id, profile.name, heroSpec, index) {
                        resolveStrictHeroLabel(profile, heroSpec, index)
                    }
                    val isActive = profile.id == activeProfileId
                    val isMarvel = resolveCardHeroUniverse(profile, heroSpec) == ScreenHeroUniverse.MARVEL
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
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.linearGradient(
                                            if (isMarvel) listOf(Color(0xE0260A12), Color(0xF00E0A12))
                                            else listOf(Color(0xE00A1628), Color(0xF00A0E18))
                                        )
                                    )
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
