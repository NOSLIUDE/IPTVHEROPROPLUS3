package com.nosliude.iptvheropro.ui.profile

data class CharacterAvatar(
    val id: String,
    val displayName: String,
    val assetFileName: String
)

object CharacterAvatarCatalog {

    val all: List<CharacterAvatar> = listOf(
        // Lote 1 (10)
        CharacterAvatar("batman", "Batman", "batman.png"),
        CharacterAvatar("capitao-america", "Capitão América", "capitao-america.png"),
        CharacterAvatar("homem-aranha", "Homem-Aranha", "homem-aranha.png"),
        CharacterAvatar("homem-de-ferro", "Homem de Ferro", "homem-de-ferro.png"),
        CharacterAvatar("lanterna-verde", "Lanterna Verde", "lanterna-verde.png"),
        CharacterAvatar("mulher-maravilha", "Mulher-Maravilha", "mulher-maravilha.png"),
        CharacterAvatar("shazam", "Shazam", "shazam.png"),
        CharacterAvatar("super-homem", "Super-Homem", "super-homem.png"),
        CharacterAvatar("thor", "Thor", "thor.png"),
        CharacterAvatar("wolverine", "Wolverine", "wolverine.png"),

        // Lote 2 (10)
        CharacterAvatar("aranha-de-ferro", "Aranha de Ferro", "aranha-de-ferro.png"),
        CharacterAvatar("dentinho", "Dentinho", "dentinho.png"),
        CharacterAvatar("garota-aranha", "Garota-Aranha", "garota-aranha.png"),
        CharacterAvatar("hulk", "Hulk", "hulk.png"),
        CharacterAvatar("jessica-jones", "Jessica Jones", "jessica-jones.png"),
        CharacterAvatar("mulher-hulk", "Mulher-Hulk", "mulher-hulk.png"),
        CharacterAvatar("nova", "Nova", "nova.png"),
        CharacterAvatar("pantera-negra", "Pantera Negra", "pantera-negra.png"),
        CharacterAvatar("soldado-invernal", "Soldado Invernal", "soldado-invernal.png"),
        CharacterAvatar("thor_2", "Thor (Clássico)", "thor_2.png"),

        // Lote 3 (10)
        CharacterAvatar("doutor-estranho", "Doutor Estranho", "doutor-estranho.png"),
        CharacterAvatar("elektra", "Elektra", "elektra.png"),
        CharacterAvatar("gaviao-arqueiro", "Gavião Arqueiro", "gaviao-arqueiro.png"),
        CharacterAvatar("maquina-de-combate", "Máquina de Combate", "maquina-de-combate.png"),
        CharacterAvatar("motoqueiro-fantasma", "Motoqueiro Fantasma", "motoqueiro-fantasma.png"),
        CharacterAvatar("mulher-aranha", "Mulher-Aranha", "mulher-aranha.png"),
        CharacterAvatar("mulher-escarlate", "Mulher Escarlate", "mulher-escarlate.png"),
        CharacterAvatar("nick-fury", "Nick Fury", "nick-fury.png"),
        CharacterAvatar("punho-de-ferro", "Punho de Ferro", "punho-de-ferro.png"),
        CharacterAvatar("vespa", "Vespa", "vespa.png"),

        // Lote 4 (9)
        CharacterAvatar("arqueiro-verde", "Arqueiro Verde", "arqueiro-verde.png"),
        CharacterAvatar("arsenal", "Arsenal", "arsenal.png"),
        CharacterAvatar("batman_2", "Batman (Moderna)", "batman_2.png"),
        CharacterAvatar("canario-negro", "Canário Negro", "canario-negro.png"),
        CharacterAvatar("flash", "Flash", "flash.png"),
        CharacterAvatar("iris-west", "Iris West", "iris-west.png"),
        CharacterAvatar("lois-lane", "Lois Lane", "lois-lane.png"),
        CharacterAvatar("mulher-gato", "Mulher-Gato", "mulher-gato.png"),
        CharacterAvatar("mulher-maravilha_2", "Mulher-Maravilha (Moderna)", "mulher-maravilha_2.png")
    )

    fun resolveId(rawNameOrId: String?): String {
        if (rawNameOrId.isNullOrBlank()) return "batman"
        val cleaned = rawNameOrId
            .trim()
            .lowercase()
            .removeSuffix(".png")

        // Verifica correspondência direta (ex.: "batman_2", "thor_2", "mulher-maravilha_2")
        all.firstOrNull { it.id == cleaned }?.let { return it.id }

        // Normaliza acentos e espaços (ex.: "Capitão América" -> "capitao-america")
        val normalized = cleaned
            .replace(Regex("[áàâãä]"), "a")
            .replace(Regex("[éèêë]"), "e")
            .replace(Regex("[íìîï]"), "i")
            .replace(Regex("[óòôõö]"), "o")
            .replace(Regex("[úùûü]"), "u")
            .replace("ç", "c")
            .replace(Regex("[^a-z0-9_]+"), "-")
            .trim('-')

        return all.firstOrNull { it.id == normalized }?.id ?: "batman"
    }

    fun findById(id: String?): CharacterAvatar {
        val resolved = resolveId(id)
        return all.first { it.id == resolved }
    }
}
