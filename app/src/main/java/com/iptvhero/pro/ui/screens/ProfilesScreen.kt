package com.iptvhero.pro.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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

private val HeroProfileAssets: List<Pair<String, String>> = listOf(
    "batman" to "batman.png",
    "capitao-america" to "capitao-america.png",
    "homem-aranha" to "homem-aranha.png",
    "homem-de-ferro" to "homem-de-ferro.png",
    "lanterna-verde" to "lanterna-verde.png",
    "mulher-maravilha" to "mulher-maravilha.png",
    "shazam" to "shazam.png",
    "super-homem" to "super-homem.png",
    "thor" to "thor.png",
    "wolverine" to "wolverine.png",
    "aranha-de-ferro" to "aranha-de-ferro.png",
    "dentinho" to "dentinho.png",
    "garota-aranha" to "garota-aranha.png",
    "hulk" to "hulk.png",
    "jessica-jones" to "jessica-jones.png",
    "mulher-hulk" to "mulher-hulk.png",
    "nova" to "nova.png",
    "pantera-negra" to "pantera-negra.png",
    "soldado-invernal" to "soldado-invernal.png",
    "thor_2" to "thor_2.png",
    "doutor-estranho" to "doutor-estranho.png",
    "elektra" to "elektra.png",
    "gaviao-arqueiro" to "gaviao-arqueiro.png",
    "maquina-de-combate" to "maquina-de-combate.png",
    "motoqueiro-fantasma" to "motoqueiro-fantasma.png",
    "mulher-aranha" to "mulher-aranha.png",
    "mulher-escarlate" to "mulher-escarlate.png",
    "nick-fury" to "nick-fury.png",
    "punho-de-ferro" to "punho-de-ferro.png",
    "vespa" to "vespa.png",
    "arqueiro-verde" to "arqueiro-verde.png",
    "arsenal" to "arsenal.png",
    "batman_2" to "batman_2.png",
    "canario-negro" to "canario-negro.png",
    "flash" to "flash.png",
    "iris-west" to "iris-west.png",
    "lois-lane" to "lois-lane.png",
    "mulher-gato" to "mulher-gato.png",
    "mulher-maravilha_2" to "mulher-maravilha_2.png"
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

private fun resolveProfileAssetFile(profile: ProfileEntity): String {
    val normalizedName = normalizeHeroKey(profile.name)
    val directMatch = HeroProfileAssets.firstOrNull { (key, _) ->
        normalizedName == key || normalizedName.contains(key)
    }
    if (directMatch != null) return directMatch.second

    val index = abs(profile.id.hashCode()) % HeroProfileAssets.size
    return HeroProfileAssets[index].second
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
    profile: ProfileEntity,
    isMarvel: Boolean,
    modifier: Modifier = Modifier
) {
    val fileName = remember(profile.id, profile.name) { resolveProfileAssetFile(profile) }
    val bitmap = rememberProfileAvatarBitmap(fileName)

    Box(
        modifier = modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.linearGradient(
                    if (isMarvel) listOf(MarvelRed, Color(0xFF450A0A))
                    else listOf(DcBlue, DcCyan)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = profile.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = if (isMarvel) "M" else "DC",
                color = Color.White,
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
                    val isMarvel = profile.universe.equals("marvel", ignoreCase = true)
                    val validityInfo = formatValidity(profile.validityIso)

                    Box(
                        modifier = Modifier
                            .height(190.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.linearGradient(
                                    if (isMarvel) listOf(Color(0xE0260A12), Color(0xF00E0A12))
                                    else listOf(Color(0xE00A1628), Color(0xF00A0E18))
                                )
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
                                    profile = profile,
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
                                            .background(MarvelRed.copy(alpha = 0.85f))
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
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val hostLabel = extractProfileHost(profile).ifBlank { "sem-host" }
                                Text(
                                    text = "$hostLabel • ${profile.sourceType.uppercase()}${if (profile.bulkOrigin) " • LOTE" else ""}",
                                    color = TextDim,
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
                                    Text("TV ${profile.liveCount}", color = MarvelYellow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("VOD ${profile.vodCount}", color = DcCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("SER ${profile.seriesCount}", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        "CON ${profile.activeConnections}/${profile.maxConnections ?: "∞"}",
                                        color = TextDim,
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
