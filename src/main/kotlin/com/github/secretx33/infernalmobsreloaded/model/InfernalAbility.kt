package com.github.secretx33.infernalmobsreloaded.model

import kotlin.math.min

enum class InfernalAbility {
    SECOND_WING,
    ARCHER,
    ARMOURED,
    BERSERK,
    BLINDING,
    HEAVY,
    RESILIENT,
    INVISIBLE,
    CONFUSING,
    TELEPORT,
    EXPLODE,
    FIREWORK,
    FLYING,
    GHASTLY,
    GHOST,
    LEVITATE,
    LIFESTEAL,
    CALL_THE_GANG,
    MOLTEN,
    MORPH,
    MOUNTED,
    NECROMANCER,
    POISONOUS,
    POTIONS,
    QUICKSAND,
    RUST,
    SAPPER,
    SPEEDY,
    STORM,
    THIEF,
    TOSSER,
    VENGEANCE,
    WEAKNESS,
    WEBBER,
    WITHERING;

    companion object {
        val values = values().toList()

        fun random(number: Int): List<InfernalAbility> {
            require(number >= 0) { "number cannot be lower than 0, number = $number" }
            return values.subList(0, min(values.size, number))
        }
    }
}
