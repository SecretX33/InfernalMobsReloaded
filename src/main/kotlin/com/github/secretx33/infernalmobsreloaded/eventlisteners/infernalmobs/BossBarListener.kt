package com.github.secretx33.infernalmobsreloaded.eventlisteners.infernalmobs

import com.github.secretx33.infernalmobsreloaded.events.InfernalHealedEvent
import com.github.secretx33.infernalmobsreloaded.manager.BossBarManager
import com.github.secretx33.infernalmobsreloaded.manager.InfernalMobsManager
import com.github.secretx33.infernalmobsreloaded.utils.extension.getHealthPercent
import com.github.secretx33.infernalmobsreloaded.utils.extension.runSync
import org.bukkit.Bukkit
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.plugin.Plugin

class BossBarListener(
    private val plugin: Plugin,
    private val mobsManager: InfernalMobsManager,
    private val bossBarManager: BossBarManager,
) : Listener {

    init { Bukkit.getPluginManager().registerEvents(this, plugin) }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun EntityDamageEvent.updateBossBarHealth() {
        val entity = entity
        if (entity !is LivingEntity || !entity.isInfernalMob()) return
        bossBarManager.updateBossBar(entity, entity.getHealthPercent(finalDamage))
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private fun InfernalHealedEvent.updateBossBarHealth() {
        bossBarManager.updateBossBar(entity, entity.getHealthPercent())
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun PlayerJoinEvent.onPlayerJoin() {
        bossBarManager.showBarOfNearbyInfernals(player)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun PlayerTeleportEvent.onPlayerTeleport() {
        runSync(plugin, 50L) { bossBarManager.showBarOfNearbyInfernals(player) }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun PlayerChangedWorldEvent.onPlayerSwitchWorld() {
        runSync(plugin, 150L) { bossBarManager.showBarOfNearbyInfernals(player) }
    }

    private fun LivingEntity.isInfernalMob() = mobsManager.isValidInfernalMob(this)
}
