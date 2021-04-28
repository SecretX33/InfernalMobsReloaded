package com.github.secretx33.infernalmobsreloaded.model

import java.util.*
import kotlin.math.min

enum class Abilities {
    ARCHER,
    ARMOURED,
    BERSERK,
    BLINDING,
    CALL_THE_GANG,
    CONFUSING,
    FIREWORK,
    FLYING,
    GHASTLY,
    GHOST,
    HEAVY,
    INVISIBLE,
    LEVITATE,
    LIFESTEAL,
    LIGHTNING,
    KAMIKAZE,
    MOLTEN,
    MORPH,
    MOUNTED,
    NECROMANCER,
    POISONOUS,
    POTIONS,
    SLOWNESS,
    RUST,
    SAPPER,
    SECOND_WING,
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
