package com.github.secretx33.infernalmobsreloaded.eventlisteners.entity

import com.github.secretx33.infernalmobsreloaded.config.Config
import com.github.secretx33.infernalmobsreloaded.config.ConfigKeys
import com.github.secretx33.infernalmobsreloaded.manager.BossBarManager
import com.github.secretx33.infernalmobsreloaded.manager.InfernalMobsManager
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.plugin.Plugin
import org.koin.core.component.KoinApiExtension

@KoinApiExtension
class PlayerMoveListener  (
    plugin: Plugin,
    private val config: Config,
    private val barManager: BossBarManager,
    private val mobsManager: InfernalMobsManager,
): Listener {

    init { Bukkit.getPluginManager().registerEvents(this, plugin) }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun PlayerMoveEvent.whenPlayerMoves() {
        val world = player.world
        if(!world.isWhitelisted()) return

        val nearbyInfernals = player.getNearbyInfernals().takeIf { it.isNotEmpty() } ?: return
        barManager.manageInfernalBossBarsVisibility(player, nearbyInfernals)
    }

    private fun LivingEntity.getNearbyInfernals() = location.getNearbyLivingEntities(bossBarShowRange) { !it.isDead && it.isValid && it.isInfernalMob() }

    private fun LivingEntity.isInfernalMob() = mobsManager.isValidInfernalMob(this)

    private fun World.isWhitelisted() = validWorlds.let { worlds -> worlds.contains("<ALL>") || worlds.any { it.equals(name, ignoreCase = true) } }

    private val validWorlds get() = config.get<List<String>>(ConfigKeys.INFERNAL_ALLOWED_WORLDS)
    private val bossBarShowRange get() = config.getDouble(ConfigKeys.BOSS_BAR_SHOW_RANGE, maxValue = 256.0)
}
