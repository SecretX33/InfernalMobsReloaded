package com.github.secretx33.infernalmobsreloaded.model

import com.github.secretx33.infernalmobsreloaded.utils.capitalizeFully
import java.util.*
import kotlin.math.min

enum class Abilities {
    ARCHER,
    ARMOURED,
    BERSERK,
    BLINDING,
    CALL_THE_GANG,
    CONFUSION,
    FIREWORK,
    FLYING,
    GHASTLY,
    GHOST,
    HEAVY,
    HUNGER,
    INVISIBLE,
    KAMIKAZE,
    LEVITATE,
    LIFESTEAL,
    LIGHTNING,
    MOLTEN,
    MORPH,
    MOUNTED,
    NECROMANCER,
    POISONOUS,
    POTIONS,
    RUST,
    SECOND_WIND,
    SLOWNESS,
    SPEEDY,
    TELEPORT,
    THIEF,
    THORNMAIL,
    TOSSER,
    WEAKNESS,
    WEBBER,
    WITHERING;

    val configEntry = name.toLowerCase(Locale.US).replace('_', '-')
    val displayName = name.replace('_', ' ').capitalizeFully()

    companion object {
        val values = values().toList()
        val lowercasedValues = values().map { it.name.toLowerCase(Locale.US) }

        fun random(number: Int): Set<Abilities> {
            require(number >= 0) { "number cannot be lower than 0, number = $number" }
            return values.shuffled().subList(0, min(values.size, number)).toSet()
        }

        fun getOrNull(name: String) = values.firstOrNull { it.name.equals(name, ignoreCase = true) }
    }
}
