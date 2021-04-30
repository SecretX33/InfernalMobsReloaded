package com.github.secretx33.infernalmobsreloaded.eventlisteners.entity

import com.github.secretx33.infernalmobsreloaded.events.InfernalDamageDoneEvent
import com.github.secretx33.infernalmobsreloaded.events.InfernalDamageTakenEvent
import com.github.secretx33.infernalmobsreloaded.manager.InfernalMobsManager
import org.bukkit.Bukkit
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.plugin.Plugin
import org.koin.core.component.KoinApiExtension

@KoinApiExtension
class EntityDamageEntityListener(plugin: Plugin, private val mobsManager: InfernalMobsManager): Listener {

    init { Bukkit.getPluginManager().registerEvents(this, plugin) }

    // When an infernal takes damage

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    private fun EntityDamageByEntityEvent.onInfernalDamageTaken() {
        val entity = entity as? LivingEntity ?: return
        val attacker = damager as? LivingEntity ?: return
        if(!entity.isInfernalMob()) return

        val infernalType = mobsManager.getInfernalTypeOrNull(entity) ?: return
        val event = InfernalDamageTakenEvent(entity, attacker, damage, cause, infernalType)
        Bukkit.getPluginManager().callEvent(event)
        if(event.isCancelled) isCancelled = true
        damage *= event.damageMulti
    }

    // When an infernal causes damage

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    private fun EntityDamageByEntityEvent.onInfernalDamageDone() {
        val entity = damager as? LivingEntity ?: return
        val defender = entity as? LivingEntity ?: return
        if(!entity.isInfernalMob()) return

        val infernalType = mobsManager.getInfernalTypeOrNull(entity) ?: return
        val event = InfernalDamageDoneEvent(entity, defender, damage, cause, infernalType)
        Bukkit.getPluginManager().callEvent(event)
        if(event.isCancelled) isCancelled = true
        damage *= event.damageMulti
    }

    private fun LivingEntity.isInfernalMob() = mobsManager.isValidInfernalMob(this)
}
