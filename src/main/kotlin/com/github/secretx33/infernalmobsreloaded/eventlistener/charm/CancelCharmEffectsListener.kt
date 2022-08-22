package com.github.secretx33.infernalmobsreloaded.eventlistener.charm

import com.github.secretx33.infernalmobsreloaded.manager.CharmsManager
import com.github.secretx33.infernalmobsreloaded.util.extension.runSync
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
import toothpick.InjectConstructor

@InjectConstructor
class CancelCharmEffectsListener(
    private val plugin: Plugin,
    private val charmsManager: CharmsManager,
) : Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun PlayerJoinEvent.onPlayerJoin() {
        if (!player.isOnCharmEnabledWorld()) return
        player.updateCharmEffects()
    }

    @EventHandler(priority = EventPriority.NORMAL)
    private fun PlayerQuitEvent.onPlayerQuit() = player.cancelCharmEffects()

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun PlayerDeathEvent.onPlayerDeath() = entity.cancelCharmEffects()

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun PlayerItemBreakEvent.onItemBreak() =
        runSync(plugin, 50L) { player.updateCharmEffects() }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun PlayerChangedWorldEvent.onWorldChange() = player.scheduleCharmCheck()

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun PlayerTeleportEvent.onPlayerTeleport() = player.scheduleCharmCheck()

    private fun Player.scheduleCharmCheck() = runSync(plugin, 50L) {
        if (isOnCharmEnabledWorld()) {
            updateCharmEffects()
        } else {
            cancelCharmEffects()
        }
    }

    private fun Player.updateCharmEffects() = charmsManager.updateCharmEffects(this)

    private fun Player.cancelCharmEffects() = charmsManager.cancelAllCharmTasks(this)

    private fun Player.isOnCharmEnabledWorld(): Boolean = charmsManager.areCharmsAllowedOnWorld(world)
}
