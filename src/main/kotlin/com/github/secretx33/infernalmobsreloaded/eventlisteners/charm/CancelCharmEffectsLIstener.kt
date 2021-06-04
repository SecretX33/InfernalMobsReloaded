package com.github.secretx33.infernalmobsreloaded.eventlisteners.charm

import com.github.secretx33.infernalmobsreloaded.manager.CharmsManager
import com.github.secretx33.infernalmobsreloaded.utils.runSync
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerItemBreakEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.plugin.Plugin

class CancelCharmEffectsListener (
    private val plugin: Plugin,
    private val charmsManager: CharmsManager,
) : Listener {

    init { Bukkit.getPluginManager().registerEvents(this, plugin) }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun PlayerJoinEvent.onPlayerJoin() {
        if(!player.isOnCharmEnabledWorld()) return
        player.updateCharmEffects()
    }

    @EventHandler(priority = EventPriority.NORMAL)
    private fun PlayerQuitEvent.onPlayerQuit() {
        player.cancelCharmEffects()
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun PlayerDeathEvent.onPlayerDeath() {
        entity.cancelCharmEffects()
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun PlayerItemBreakEvent.onItemBreak() {
        runSync(plugin, 50L) { player.updateCharmEffects() }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun PlayerChangedWorldEvent.onWorldChange() {
        runSync(plugin, 50L) {
            if(player.isOnCharmEnabledWorld()) player.updateCharmEffects()
            else player.cancelCharmEffects()
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun PlayerTeleportEvent.onPlayerTeleport() {
        runSync(plugin, 50L) {
            if(player.isOnCharmEnabledWorld()) player.updateCharmEffects()
            else player.cancelCharmEffects()
        }
    }

    private fun Player.updateCharmEffects() = charmsManager.updateCharmEffects(this)

    private fun Player.cancelCharmEffects() = charmsManager.cancelAllCharmTasks(this)

    private fun Player.isOnCharmEnabledWorld() = charmsManager.areCharmsAllowedOnWorld(world)
}
