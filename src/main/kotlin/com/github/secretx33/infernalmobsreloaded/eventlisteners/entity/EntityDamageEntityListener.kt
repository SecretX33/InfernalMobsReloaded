package com.github.secretx33.infernalmobsreloaded.eventlisteners.entity

import com.github.secretx33.infernalmobsreloaded.config.Config
import com.github.secretx33.infernalmobsreloaded.config.ConfigKeys
import com.github.secretx33.infernalmobsreloaded.events.InfernalDamageDoneEvent
import com.github.secretx33.infernalmobsreloaded.events.InfernalDamageTakenEvent
import com.github.secretx33.infernalmobsreloaded.manager.InfernalMobsManager
import org.bukkit.Bukkit
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Projectile
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.plugin.Plugin
import org.koin.core.component.KoinApiExtension

@KoinApiExtension
class EntityDamageEntityListener (
    plugin: Plugin,
    private val config: Config,
    private val mobsManager: InfernalMobsManager,
): Listener {

    init { Bukkit.getPluginManager().registerEvents(this, plugin) }

    // When an infernal takes damage

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    private fun EntityDamageByEntityEvent.onInfernalDamageTaken() {
        val infernal = entity as? LivingEntity ?: return
        val attacker = damager as? LivingEntity ?: (damager as? Projectile)?.shooter as? LivingEntity ?: return
        if(!infernal.isInfernalMob()) return

        val infernalType = mobsManager.getInfernalTypeOrNull(infernal) ?: return
        val event = InfernalDamageTakenEvent(infernal, attacker, damage, cause, infernalType)
        event.callEvent()
        if(event.isCancelled) isCancelled = true
        damage *= event.damageMulti
    }

    // When an infernal causes damage

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    private fun EntityDamageByEntityEvent.onInfernalDamageDone() {
        val infernal = damager as? LivingEntity ?: (damager as? Projectile)?.shooter as? LivingEntity ?: return
        val defender = entity as? LivingEntity ?: return
        if(!infernal.isInfernalMob()) return

        val infernalType = mobsManager.getInfernalTypeOrNull(infernal) ?: return
        val event = InfernalDamageDoneEvent(infernal, defender, damage, cause, infernalType)
        event.callEvent()
        if(event.isCancelled) isCancelled = true
        damage *= event.damageMulti
    }

    // Infernal cannot damage itself

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    private fun EntityDamageByEntityEvent.onInfernalDamageItselfOrMount() {
        val attacker = damager as? LivingEntity ?: (damager as? Projectile)?.shooter as? LivingEntity ?: return
        val defender = entity as? LivingEntity ?: return
        if(!attacker.isInfernalMob()) return

        // if infernals cannot damage itself (or its mount)
        if(attacker.isDamagingItselfOrMount(defender) && cannotDamageItself) isCancelled = true
    }

    private fun LivingEntity.isDamagingItselfOrMount(defender: LivingEntity) = uniqueId == defender.uniqueId || defender.passengers.any { it.uniqueId == uniqueId }

    private fun LivingEntity.isInfernalMob() = mobsManager.isValidInfernalMob(this)

    private val cannotDamageItself
        get() = config.get<Boolean>(ConfigKeys.INFERNALS_CANNOT_DAMAGE_THEMSELVES)
}
