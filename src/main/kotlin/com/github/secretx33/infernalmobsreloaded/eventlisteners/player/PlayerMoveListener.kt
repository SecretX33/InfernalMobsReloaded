package com.github.secretx33.infernalmobsreloaded.eventlisteners.player

import com.github.secretx33.infernalmobsreloaded.config.Config
import com.github.secretx33.infernalmobsreloaded.config.ConfigKeys
import com.github.secretx33.infernalmobsreloaded.manager.BossBarManager
import com.github.secretx33.infernalmobsreloaded.manager.InfernalMobsManager
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.plugin.Plugin
import org.koin.core.component.KoinApiExtension

@KoinApiExtension
class PlayerMoveListener (
    plugin: Plugin,
    private val config: Config,
    private val bossBarManager: BossBarManager,
    private val mobsManager: InfernalMobsManager,
): Listener {

    init { Bukkit.getPluginManager().registerEvents(this, plugin) }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun PlayerMoveEvent.whenPlayerMoves() {
        if(!bossBarEnabled) return
        val world = player.world
        if(!world.isWhitelisted()) return
        bossBarManager.showBarOfNearbyInfernals(player)
    }

    private fun World.isWhitelisted() = validWorlds.let { worlds -> worlds.contains("<ALL>") || worlds.any { it.equals(name, ignoreCase = true) } }

    private val validWorlds get() = config.get<List<String>>(ConfigKeys.INFERNAL_ALLOWED_WORLDS)

    private val bossBarEnabled get() = config.get<Boolean>(ConfigKeys.ENABLE_BOSS_BARS)
}