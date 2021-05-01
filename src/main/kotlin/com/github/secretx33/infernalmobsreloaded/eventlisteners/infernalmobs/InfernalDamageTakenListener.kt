package com.github.secretx33.infernalmobsreloaded.eventlisteners.infernalmobs

import com.github.secretx33.infernalmobsreloaded.events.InfernalDamageTakenEvent
import com.github.secretx33.infernalmobsreloaded.manager.BossBarManager
import com.github.secretx33.infernalmobsreloaded.manager.InfernalMobsManager
import com.github.secretx33.infernalmobsreloaded.utils.getHealthPercent
import org.bukkit.Bukkit
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.plugin.Plugin
import org.koin.core.component.KoinApiExtension

@KoinApiExtension
class InfernalDamageTakenListener (
    plugin: Plugin,
    private val mobsManager: InfernalMobsManager,
    private val bossBarManager: BossBarManager,
): Listener {

    init { Bukkit.getPluginManager().registerEvents(this, plugin) }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    private fun InfernalDamageTakenEvent.onInfernalDamageTaken() {
        mobsManager.triggerOnDamageTakenAbilities(this)
        println("${attacker.name} attacked infernal ${entity.name} (${entity.getHealthPercent()}) (${infernalType.name}), was event cancelled = $isCancelled")
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun EntityDamageEvent.updateBossBarHealth() {
        val entity = entity
        if(entity !is LivingEntity || !entity.isInfernalMob()) return
        bossBarManager.updateBossBar(entity, entity.getHealthPercent(finalDamage))
    }

    private fun LivingEntity.isInfernalMob() = mobsManager.isValidInfernalMob(this)
}
