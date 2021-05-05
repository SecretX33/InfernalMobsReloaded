package com.github.secretx33.infernalmobsreloaded.model

import com.cryptomorin.xseries.XPotion
import org.bukkit.potion.PotionEffectType

enum class DisplayCustomNameMode (
    val addCustomName: Boolean,
    val customNameVisible: Boolean,
) {
    NONE(false, false),
    LOOKING_AT(true, false),
    ALWAYS(true, true),
}

enum class KilledByPoison {
    ALL, PLAYERS, MONSTERS, NONE
}
