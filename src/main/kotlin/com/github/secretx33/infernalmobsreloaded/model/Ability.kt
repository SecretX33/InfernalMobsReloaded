package com.github.secretx33.infernalmobsreloaded.model

import com.github.secretx33.infernalmobsreloaded.util.extension.capitalizeFully
import java.util.Locale
import kotlin.math.min

enum class Ability {
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
    MULTI_GHASTLY,
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

    val configEntry = name.lowercase(Locale.US).replace('_', '-')
    val displayName = name.replace('_', ' ').capitalizeFully()

    companion object {
        val VALUES = values().toList()
        val LOWERCASE_VALUES = values().map { it.name.lowercase(Locale.US) }
        val MAX_AMOUNT_OF_SIMULTANEOUS_ABILITIES = VALUES.size - 2

        fun random(number: Int, disabled: Set<Ability>): Set<Ability> {
            require(number >= 0) { "number cannot be lower than 0, number = $number" }

            if (number == 0) return emptySet()
            return VALUES.filter { it !in disabled }.shuffled().let {
                it.subList(0, min(it.size, min(VALUES.size, number))).toSet()
            }
        }

        fun getOrNull(name: String) = VALUES.firstOrNull { it.name.equals(name, ignoreCase = true) }
    }
}
