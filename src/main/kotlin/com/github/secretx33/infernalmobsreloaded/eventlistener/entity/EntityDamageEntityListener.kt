package com.github.secretx33.infernalmobsreloaded.eventlistener.entity

import com.github.secretx33.infernalmobsreloaded.config.Config
import com.github.secretx33.infernalmobsreloaded.config.ConfigKeys
import com.github.secretx33.infernalmobsreloaded.event.InfernalDamageDoneEvent
import com.github.secretx33.infernalmobsreloaded.event.InfernalDamageTakenEvent
import com.github.secretx33.infernalmobsreloaded.manager.InfernalMobsManager
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Projectile
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import toothpick.InjectConstructor

@InjectConstructor
class EntityDamageEntityListener(
    private val config: Config,
    private val mobsManager: InfernalMobsManager,
) : Listener {

    // When an infernal takes damage

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    private fun EntityDamageByEntityEvent.onInfernalDamageTaken() {
        val infernal = entity as? LivingEntity ?: return
        val attacker = damager as? LivingEntity ?: (damager as? Projectile)?.shooter as? LivingEntity ?: return
        if (!infernal.isInfernalMob()) return

        val infernalType = mobsManager.getInfernalTypeOrNull(infernal) ?: return
        val event = InfernalDamageTakenEvent(infernal, attacker, damage, cause, infernalType)
        if (!event.callEvent()) isCancelled = true
        damage *= event.damageMulti
    }

    // When an infernal causes damage

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    private fun EntityDamageByEntityEvent.onInfernalDamageDone() {
        val infernal = damager as? LivingEntity ?: (damager as? Projectile)?.shooter as? LivingEntity ?: return
        val defender = entity as? LivingEntity ?: return
        if (!infernal.isInfernalMob()) return

        val infernalType = mobsManager.getInfernalTypeOrNull(infernal) ?: return
        val event = InfernalDamageDoneEvent(infernal, defender, damage, cause, infernalType)
        if (!event.callEvent()) isCancelled = true
        damage *= event.damageMulti
    }

    // Infernal cannot damage itself

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private fun EntityDamageByEntityEvent.onInfernalDamageItselfOrMount() {
        val attacker = damager as? LivingEntity ?: (damager as? Projectile)?.shooter as? LivingEntity ?: return
        val defender = entity as? LivingEntity ?: return
        if (!attacker.isInfernalMob()) return

        // if infernals cannot damage itself (or its mount)
        if (cannotDamageItself && attacker.isDamagingItselfOrMount(defender)) isCancelled = true
    }

    private fun LivingEntity.isDamagingItselfOrMount(defender: LivingEntity): Boolean =
        uniqueId == defender.uniqueId || defender.passengers.any { it.uniqueId == uniqueId }

    private fun LivingEntity.isInfernalMob(): Boolean = mobsManager.isValidInfernalMob(this)

    private val cannotDamageItself: Boolean
        get() = config.get(ConfigKeys.INFERNALS_CANNOT_DAMAGE_THEMSELVES)
}
