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
import androidx.compose.foundation.lazy.grid.items
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
import com.iptvhero.pro.ui.theme.DcCyan
import com.iptvhero.pro.ui.theme.GoldFocus
import com.iptvhero.pro.ui.theme.HeroThemeType
import com.iptvhero.pro.ui.theme.MarvelRed
import com.iptvhero.pro.ui.theme.MarvelYellow
import com.iptvhero.pro.ui.theme.TextDim
import com.iptvhero.pro.ui.theme.TextPrimary
import java.io.IOException
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.abs

private data class HeroCharacterEntry(
    val key: String,
    val fileName: String,
    val isMarvel: Boolean
)

private val HeroProfileCatalog: List<HeroCharacterEntry> = listOf(
    // Lote 1
    HeroCharacterEntry("batman", "batman.png", isMarvel = false),
    HeroCharacterEntry("capitao-america", "capitao-america.png", isMarvel = true),
    HeroCharacterEntry("homem-aranha", "homem-aranha.png", isMarvel = true),
    HeroCharacterEntry("homem-de-ferro", "homem-de-ferro.png", isMarvel = true),
    HeroCharacterEntry("lanterna-verde", "lanterna-verde.png", isMarvel = false),
    HeroCharacterEntry("mulher-maravilha", "mulher-maravilha.png", isMarvel = false),
    HeroCharacterEntry("shazam", "shazam.png", isMarvel = false),
    HeroCharacterEntry("super-homem", "super-homem.png", isMarvel = false),
    HeroCharacterEntry("thor", "thor.png", isMarvel = true),
    HeroCharacterEntry("wolverine", "wolverine.png", isMarvel = true),

    // Lote 2
    HeroCharacterEntry("aranha-de-ferro", "aranha-de-ferro.png", isMarvel = true),
    HeroCharacterEntry("dentinho", "dentinho.png", isMarvel = true),
    HeroCharacterEntry("garota-aranha", "garota-aranha.png", isMarvel = true),
    HeroCharacterEntry("hulk", "hulk.png", isMarvel = true),
    HeroCharacterEntry("jessica-jones", "jessica-jones.png", isMarvel = true),
    HeroCharacterEntry("mulher-hulk", "mulher-hulk.png", isMarvel = true),
    HeroCharacterEntry("nova", "nova.png", isMarvel = true),
    HeroCharacterEntry("pantera-negra", "pantera-negra.png", isMarvel = true),
    HeroCharacterEntry("soldado-invernal", "soldado-invernal.png", isMarvel = true),
    HeroCharacterEntry("thor_2", "thor_2.png", isMarvel = true),

    // Lote 3
    HeroCharacterEntry("doutor-estranho", "doutor-estranho.png", isMarvel = true),
    HeroCharacterEntry("elektra", "elektra.png", isMarvel = true),
    HeroCharacterEntry("gaviao-arqueiro", "gaviao-arqueiro.png", isMarvel = true),
    HeroCharacterEntry("maquina-de-combate", "maquina-de-combate.png", isMarvel = true),
    HeroCharacterEntry("motoqueiro-fantasma", "motoqueiro-fantasma.png", isMarvel = true),
    HeroCharacterEntry("mulher-aranha", "mulher-aranha.png", isMarvel = true),
    HeroCharacterEntry("mulher-escarlate", "mulher-escarlate.png", isMarvel = true),
    HeroCharacterEntry("nick-fury", "nick-fury.png", isMarvel = true),
    HeroCharacterEntry("punho-de-ferro", "punho-de-ferro.png", isMarvel = true),
    HeroCharacterEntry("vespa", "vespa.png", isMarvel = true),

    // Lote 4
    HeroCharacterEntry("arqueiro-verde", "arqueiro-verde.png", isMarvel = false),
    HeroCharacterEntry("arsenal", "arsenal.png", isMarvel = false),
    HeroCharacterEntry("batman_2", "batman_2.png", isMarvel = false),
    HeroCharacterEntry("canario-negro", "canario-negro.png", isMarvel = false),
    HeroCharacterEntry("flash", "flash.png", isMarvel = false),
    HeroCharacterEntry("iris-west", "iris-west.png", isMarvel = false),
    HeroCharacterEntry("lois-lane", "lois-lane.png", isMarvel = false),
    HeroCharacterEntry("mulher-gato", "mulher-gato.png", isMarvel = false),
    HeroCharacterEntry("mulher-maravilha_2", "mulher-maravilha_2.png", isMarvel = false)
)

private fun normalizeHeroKey(raw: String): String {
    return raw.trim()
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
}

private fun resolveHeroEntry(profile: ProfileEntity): HeroCharacterEntry {
    val normalizedName = normalizeHeroKey(profile.name)
    val matched = HeroProfileCatalog.firstOrNull { entry ->
        normalizedName == entry.key || normalizedName.contains(entry.key)
    }
    if (matched != null) return matched

    val index = abs(profile.id.hashCode()) % HeroProfileCatalog.size
    return HeroProfileCatalog[index]
}

private fun resolveIsMarvel(profile: ProfileEntity, matchedEntry: HeroCharacterEntry): Boolean {
    val universe = profile.universe.trim().lowercase()
    return when {
        universe.contains("marvel") -> true
        universe.contains("dc") -> false
        normalizeHeroKey(profile.name) == matchedEntry.key ||
            normalizeHeroKey(profile.name).contains(matchedEntry.key) -> matchedEntry.isMarvel
        else -> abs(profile.id.hashCode()) % 2 == 0
    }
}

private fun heroCardBackgroundBrush(isMarvel: Boolean): Brush {
    return if (isMarvel) {
        // Vermelho com dourado representando os heróis Marvel
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF991B1B),
                Color(0xFF450A0A),
                Color(0xFFB45309)
            )
        )
    } else {
        // Azul com branco representando os heróis DC Comics
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF1D4ED8),
                Color(0xFF1E3A8A),
                Color(0xFFE2E8F0)
            )
        )
    }
}

@Composable
private fun rememberProfileAvatarBitmap(fileName: String): ImageBitmap? {
    val context = LocalContext.current
    return remember(fileName) {
        try {
            context.assets.open("avatars/$fileName").use { stream ->
                BitmapFactory.decodeStream(stream)?.asImageBitmap()
            }
        } catch (_: IOException) {
            null
        }
    }
}

@Composable
private fun ProfileHeroAvatar(
    fileName: String,
    profileName: String,
    isMarvel: Boolean,
    modifier: Modifier = Modifier
) {
    val bitmap = rememberProfileAvatarBitmap(fileName)

    Box(
        modifier = modifier
            .size(50.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isMarvel) {
                    Brush.linearGradient(listOf(MarvelYellow, MarvelRed))
                } else {
                    Brush.linearGradient(listOf(Color.White, DcBlue))
                }
            )
            .border(
                width = 1.5.dp,
                color = if (isMarvel) MarvelYellow else Color.White,
                shape = RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = profileName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = if (isMarvel) "M" else "DC",
                color = if (isMarvel) Color.Black else Color(0xFF0F172A),
                fontWeight = FontWeight.Black
            )
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

        if (profiles.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = statusMessage ?: "Nenhum perfil ainda.\nClique em HERO LISTA ou XTREAM / TXT para adicionar listas.",
                    color = TextDim,
                    fontSize = 15.sp
                )
            }
        } else {
            val sortedProfiles = remember(profiles) {
                profiles.sortedWith(
                    compareBy<ProfileEntity> { extractProfileHost(it) }
                        .thenByDescending { parseRemainingDays(it.validityIso) ?: Long.MIN_VALUE }
                        .thenBy { it.name.lowercase() }
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 260.dp),
                modifier = Modifier.weight(1f).padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(sortedProfiles, key = { it.id }) { profile ->
                    val isActive = profile.id == activeProfileId
                    val heroEntry = remember(profile.id, profile.name) { resolveHeroEntry(profile) }
                    val isMarvel = remember(profile.id, profile.name, profile.universe, heroEntry) {
                        resolveIsMarvel(profile, heroEntry)
                    }
                    val cardBrush = remember(isMarvel) { heroCardBackgroundBrush(isMarvel) }
                    val borderColor = if (isMarvel) {
                        MarvelYellow.copy(alpha = 0.75f)
                    } else {
                        Color.White.copy(alpha = 0.75f)
                    }
                    val secondaryTextColor = if (isMarvel) {
                        Color(0xFFFEF3C7)
                    } else {
                        Color(0xFFF8FAFC)
                    }
                    val validityInfo = formatValidity(profile.validityIso)

                    Box(
                        modifier = Modifier
                            .height(190.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(cardBrush)
                            .border(
                                width = if (isActive) 2.5.dp else 1.25.dp,
                                color = if (isActive) GoldFocus else borderColor,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .heroFocusable(RoundedCornerShape(16.dp)) {
                                onSelectProfile(profile.id)
                            }
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ProfileHeroAvatar(
                                    fileName = heroEntry.fileName,
                                    profileName = profile.name,
                                    isMarvel = isMarvel
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (isActive) {
                                        Text(
                                            text = "ATIVO",
                                            color = GoldFocus,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(MarvelRed.copy(alpha = 0.9f))
                                            .heroFocusable(CircleShape) { onDeleteProfile(profile.id) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("X", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = profile.name,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val hostLabel = extractProfileHost(profile).ifBlank { "sem-host" }
                                Text(
                                    text = "$hostLabel • ${profile.sourceType.uppercase()}${if (profile.bulkOrigin) " • LOTE" else ""}",
                                    color = secondaryTextColor,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "TV ${profile.liveCount}",
                                        color = if (isMarvel) MarvelYellow else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "VOD ${profile.vodCount}",
                                        color = if (isMarvel) Color(0xFFFDE68A) else DcCyan,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "SER ${profile.seriesCount}",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "CON ${profile.activeConnections}/${profile.maxConnections ?: "∞"}",
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
            else -> Pair("${days}d • Válido até $formatted", Color(0xFF4ADE80))
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
