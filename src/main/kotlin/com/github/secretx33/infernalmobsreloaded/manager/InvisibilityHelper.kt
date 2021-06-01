package com.github.secretx33.infernalmobsreloaded.manager

import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffectType

object InvisibilityHelper {

    fun Player.isInvisibleOrVanished(): Boolean = isVanished() || isInvisible(this)

    private fun Player.isVanished(): Boolean = getMetadata("vanished").any { it.asBoolean() }

    private fun isInvisible(player: Player): Boolean = player.isInvisible || player.activePotionEffects.any { it.type == PotionEffectType.INVISIBILITY && it.duration > 0 }
}
