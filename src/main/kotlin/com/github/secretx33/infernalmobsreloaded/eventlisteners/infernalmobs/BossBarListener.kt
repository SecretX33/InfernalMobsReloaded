package com.github.secretx33.infernalmobsreloaded.eventlisteners.infernalmobs

import com.github.secretx33.infernalmobsreloaded.events.InfernalHealedEvent
import com.github.secretx33.infernalmobsreloaded.manager.BossBarManager
import com.github.secretx33.infernalmobsreloaded.manager.InfernalMobsManager
import com.github.secretx33.infernalmobsreloaded.utils.getHealthPercent
import com.github.secretx33.infernalmobsreloaded.utils.runSync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.bukkit.Bukkit
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.plugin.Plugin
import org.koin.core.component.KoinApiExtension

@KoinApiExtension
class BossBarListener (
    private val plugin: Plugin,
    private val mobsManager: InfernalMobsManager,
    private val bossBarManager: BossBarManager,
): Listener {

    init { Bukkit.getPluginManager().registerEvents(this, plugin) }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun EntityDamageEvent.updateBossBarHealth() {
        val entity = entity
        if(entity !is LivingEntity || !entity.isInfernalMob()) return
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
        CoroutineScope(Dispatchers.Default).launch {
            delay(150L)
            runSync(plugin) { bossBarManager.showBarOfNearbyInfernals(player) }
        }
    }

    private fun LivingEntity.isInfernalMob() = mobsManager.isValidInfernalMob(this)
}
