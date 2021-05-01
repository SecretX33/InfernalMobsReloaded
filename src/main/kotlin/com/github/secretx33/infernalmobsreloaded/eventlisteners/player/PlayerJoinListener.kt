package com.github.secretx33.infernalmobsreloaded.eventlisteners.player

import com.github.secretx33.infernalmobsreloaded.manager.BossBarManager
import com.github.secretx33.infernalmobsreloaded.utils.runSync
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.Plugin
import org.koin.core.component.KoinApiExtension

@KoinApiExtension
class PlayerJoinListener (
    private val plugin: Plugin,
    private val barManager: BossBarManager,
): Listener {

    init { Bukkit.getPluginManager().registerEvents(this, plugin) }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun PlayerJoinEvent.onPlayerJoin() {
        barManager.showBarOfNearbyInfernals(player)
    }
}
