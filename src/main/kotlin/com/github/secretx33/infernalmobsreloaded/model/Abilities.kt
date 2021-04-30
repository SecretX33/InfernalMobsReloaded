package com.github.secretx33.infernalmobsreloaded.model

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
    SECOND_WING,
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

    companion object {
        val values = values().toList()

        fun random(number: Int): MutableSet<Abilities> {
            require(number >= 0) { "number cannot be lower than 0, number = $number" }
            return values.subList(0, min(values.size, number)).toMutableSet()
        }
    }
}
