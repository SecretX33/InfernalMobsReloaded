package com.github.secretx33.infernalmobsreloaded.eventlistener.ability

import com.github.secretx33.infernalmobsreloaded.config.AbilityConfig
import com.github.secretx33.infernalmobsreloaded.config.AbilityConfigKeys
import com.github.secretx33.infernalmobsreloaded.config.Config
import com.github.secretx33.infernalmobsreloaded.config.ConfigKeys
import com.github.secretx33.infernalmobsreloaded.event.InfernalLightningStrike
import com.github.secretx33.infernalmobsreloaded.util.extension.random
import com.google.common.cache.CacheBuilder
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityCombustByEntityEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import toothpick.InjectConstructor
import java.util.UUID
import java.util.concurrent.TimeUnit

@InjectConstructor
class LightningAbilityListener(
    private val config: Config,
    private val abilityConfig: AbilityConfig,
) : Listener {

    private val strikeLocations = CacheBuilder.newBuilder()
        .expireAfterWrite(1000L, TimeUnit.MILLISECONDS)
        .build<Location, UUID>()

    @EventHandler(priority = EventPriority.NORMAL)
    private fun InfernalLightningStrike.cacheLightningLocation() {
        strikeLocations.put(location, entity.uniqueId)
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    private fun EntityDamageByEntityEvent.lightningDamagingOwner() {
        if (!isLightningDamagingLivingEntity() || !cannotDamageItself) return

        val ownerUuid = strikeLocations.asMap().entries.firstOrNull { it.key.isAt(damager.location) }?.value ?: return
        // if the lightning was not fired by the damaged entity (or its rider), return
        if (!entity.isOwnerOrMountUuid(ownerUuid)) return
        // prevent the infernal mob, owner of that lightning, from damaging itself or its mount
        isCancelled = true
        damage = 0.0
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    private fun EntityCombustByEntityEvent.lightningCombustingOwner() {
        if (!isLightningDamagingLivingEntity() || !combuster.isInfernalLightning()) return

        val ownerUuid = strikeLocations.asMap().entries.firstOrNull { it.key.isAt(combuster.location) }?.value ?: return
        // if the lightning was not fired by the damaged entity (or its rider), return
        if (!entity.isOwnerOrMountUuid(ownerUuid)) return
        // prevent the infernal mob, owner of that lightning, from combusting itself or its mount
        isCancelled = true
        duration = 0
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    private fun EntityDamageByEntityEvent.lightningDamageIncrease() {
        if (!isLightningDamagingLivingEntity() || !damager.isInfernalLightning()) return

        // multiply the damage caused by lightnings
        damage *= lightningDmgMulti
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    private fun EntityCombustByEntityEvent.lightningCombustionEvent() {
        if (!isLightningDamagingLivingEntity() || !combuster.isInfernalLightning()) return

        // multiply the damage caused by lightnings
        duration = (duration.toDouble() * lightningDmgMulti).toInt()
    }

    private val lightningDmgMulti: Double
        get() = abilityConfig.getDoublePair(AbilityConfigKeys.LIGHTNING_DAMAGE_MULTIPLIER).random()

    private fun Entity.isInfernalLightning(): Boolean =
        strikeLocations.asMap().entries.firstOrNull { it.key.isAt(location) }?.value != null

    private fun Location.isAt(other: Location): Boolean =
        x == other.x && y == other.y && z == other.z && world.uid == other.world.uid

    private fun Entity.isOwnerOrMountUuid(uuid: UUID): Boolean = uniqueId == uuid || passengers.any { it.uniqueId == uuid }

    private fun EntityDamageByEntityEvent.isLightningDamagingLivingEntity(): Boolean =
        damager.type == EntityType.LIGHTNING
                && cause == EntityDamageEvent.DamageCause.LIGHTNING
                && entity is LivingEntity

    private fun EntityCombustByEntityEvent.isLightningDamagingLivingEntity(): Boolean =
        combuster.type == EntityType.LIGHTNING && entity is LivingEntity

    private val cannotDamageItself: Boolean
        get() = config.get(ConfigKeys.INFERNALS_CANNOT_DAMAGE_THEMSELVES)
}
