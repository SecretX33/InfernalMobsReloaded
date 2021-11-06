package com.github.secretx33.infernalmobsreloaded.eventlisteners.player

import com.github.secretx33.infernalmobsreloaded.config.Config
import com.github.secretx33.infernalmobsreloaded.config.ConfigKeys
import com.github.secretx33.infernalmobsreloaded.manager.BossBarManager
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.plugin.Plugin

// to update the boss bars
class PlayerMoveListener(
    plugin: Plugin,
    private val config: Config,
    private val bossBarManager: BossBarManager,
) : Listener {

    init { Bukkit.getPluginManager().registerEvents(this, plugin) }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun PlayerMoveEvent.whenPlayerMoves() {
        if (!bossBarEnabled || !player.world.isWhitelisted()) return
        bossBarManager.showBarOfNearbyInfernals(player)
    }

    private fun World.isWhitelisted(): Boolean =
        validWorlds.let { worlds -> "<ALL>" in worlds || worlds.any { it.equals(name, ignoreCase = true) } }

    private val validWorlds: List<String> get() = config.get(ConfigKeys.INFERNAL_ALLOWED_WORLDS)

    private val bossBarEnabled: Boolean get() = config.get(ConfigKeys.ENABLE_BOSS_BARS)
}
