package com.iptvhero.pro.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.math.abs

enum class CardHeroUniverse(val value: String) {
    MARVEL("marvel"),
    DC("dc");

    companion object {
        fun fromRaw(raw: String?): CardHeroUniverse? = when (raw?.trim()?.lowercase()) {
            "marvel" -> MARVEL
            "dc", "dc comics", "dc_comics" -> DC
            else -> null
        }
    }
}

data class CardHeroSpec(
    val key: String,
    val baseTag: String,
    val displayName: String,
    val assetFileName: String,
    val universe: CardHeroUniverse,
    val aliases: List<String> = emptyList()
)

object CardHeroCatalog {
    val all: List<CardHeroSpec> = listOf(
        // DC (8)
        CardHeroSpec("batman", "BATMAN", "Batman", "01_batman_0801.png", CardHeroUniverse.DC, listOf("batman_0801", "morcego")),
        CardHeroSpec("superman", "SUPERMAN", "Superman", "02_superman_0001.png", CardHeroUniverse.DC, listOf("superman_0001", "super-homem", "super homem")),
        CardHeroSpec("wwoman", "WWOMAN", "Wonder Woman", "03_wwoman_0901.png", CardHeroUniverse.DC, listOf("wwoman_0901", "wonder woman", "mulher-maravilha", "mulher maravilha")),
        CardHeroSpec("flash", "FLASH", "Flash", "04_flash_0501.png", CardHeroUniverse.DC, listOf("flash_0501", "the flash")),
        CardHeroSpec("aquaman", "AQUAMAN", "Aquaman", "05_aquaman_0701.png", CardHeroUniverse.DC, listOf("aquaman_0701")),
        CardHeroSpec("glantern", "GLANTERN", "Green Lantern", "06_glantern_2814.png", CardHeroUniverse.DC, listOf("glantern_2814", "green lantern", "lanterna verde", "lanterna-verde")),
        CardHeroSpec("garrow", "GARROW", "Green Arrow", "07_garrow_0101.png", CardHeroUniverse.DC, listOf("garrow_0101", "green arrow", "arqueiro verde", "arqueiro-verde")),
        CardHeroSpec("cyborg", "CYBORG", "Cyborg", "08_cyborg_0001.png", CardHeroUniverse.DC, listOf("cyborg_0001")),

        // Marvel (8)
        CardHeroSpec("ironman", "IRONMAN", "Iron Man", "09_ironman_0001.png", CardHeroUniverse.MARVEL, listOf("ironman_0001", "iron man", "homem de ferro", "homem-de-ferro")),
        CardHeroSpec("captain", "CAPTAIN", "Captain America", "10_captain_0001.png", CardHeroUniverse.MARVEL, listOf("captain_0001", "captain america", "capitao america", "capitão américa", "capitao-america")),
        CardHeroSpec("thor", "THOR", "Thor", "11_thor_0001.png", CardHeroUniverse.MARVEL, listOf("thor_0001")),
        CardHeroSpec("hulk", "HULK", "Hulk", "12_hulk_0001.png", CardHeroUniverse.MARVEL, listOf("hulk_0001")),
        CardHeroSpec("spider", "SPIDER", "Spider-Man", "13_spider_0001.png", CardHeroUniverse.MARVEL, listOf("spider_0001", "spiderman", "spider-man", "homem aranha", "homem-aranha")),
        CardHeroSpec("bwidow", "BWIDOW", "Black Widow", "14_bwidow_0001.png", CardHeroUniverse.MARVEL, listOf("bwidow_0001", "black widow", "viuva negra", "viúva negra", "viuva-negra")),
        CardHeroSpec("dstrange", "DSTRANGE", "Doctor Strange", "15_dstrange_0001.png", CardHeroUniverse.MARVEL, listOf("dstrange_0001", "doctor strange", "doutor estranho", "doutor-estranho")),
        CardHeroSpec("bpanther", "BPANTHER", "Black Panther", "16_bpanther_0001.png", CardHeroUniverse.MARVEL, listOf("bpanther_0001", "black panther", "pantera negra", "pantera-negra"))
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

    fun findMatch(vararg candidates: String?): CardHeroSpec? {
        for (candidate in candidates) {
            if (candidate.isNullOrBlank()) continue
            val norm = normalize(candidate)
            if (norm.isBlank()) continue

            all.firstOrNull { spec ->
                normalize(spec.key) == norm ||
                    normalize(spec.baseTag) == norm ||
                    normalize(spec.assetFileName) == norm ||
                    normalize(spec.displayName) == norm ||
                    spec.aliases.any { normalize(it) == norm }
            }?.let { return it }

            all.firstOrNull { spec ->
                val terms = (listOf(spec.key, spec.baseTag, spec.displayName) + spec.aliases).map { normalize(it) }
                terms.any { term -> term.length >= 4 && norm.contains(term) }
            }?.let { return it }
        }
        return null
    }

    /**
     * Sempre retorna um dos 16 heróis do catálogo (repetindo de forma cíclica se necessário).
     */
    fun resolveRepeating(seedIndex: Int, vararg candidates: String?): CardHeroSpec {
        findMatch(*candidates)?.let { return it }
        val idx = abs(seedIndex) % all.size
        return all[idx]
    }

    /**
     * Formata o nome do herói + numeral (ex.: BATMAN_0001, IRONMAN_0002).
     */
    fun formatHeroNumberedName(spec: CardHeroSpec, sequenceNumber: Int): String {
        val safeNum = sequenceNumber.coerceAtLeast(1)
        val digits = safeNum.toString().padStart(4, '0')
        return "${spec.baseTag}_$digits"
    }
}

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val avatarKey: String = "batman",
    val universe: String = "dc", // "marvel" ou "dc"
    val sourceType: String = "m3u", // "xtream", "m3u", "txt"
    val sourceLabel: String = "",
    val server: String? = null,
    val user: String? = null,
    val pass: String? = null,
    val validityIso: String? = null,
    val maxConnections: Int? = null,
    val activeConnections: Int = 0,
    val liveCount: Int = 0,
    val vodCount: Int = 0,
    val seriesCount: Int = 0,
    val bulkOrigin: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsed: Long = System.currentTimeMillis(),
    val isActive: Boolean = false
) {
    fun resolveHero(fallbackIndex: Int = id.toInt()): CardHeroSpec =
        CardHeroCatalog.resolveRepeating(fallbackIndex, avatarKey, name)
}
