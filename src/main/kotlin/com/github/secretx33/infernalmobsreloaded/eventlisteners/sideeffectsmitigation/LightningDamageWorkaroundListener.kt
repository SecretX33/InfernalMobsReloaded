package com.github.secretx33.infernalmobsreloaded.eventlisteners.sideeffectsmitigation

import com.github.secretx33.infernalmobsreloaded.config.Config
import com.github.secretx33.infernalmobsreloaded.config.ConfigKeys
import com.github.secretx33.infernalmobsreloaded.events.InfernalLightningStrike
import com.google.common.cache.CacheBuilder
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.plugin.Plugin
import org.koin.core.component.KoinApiExtension
import java.util.*
import java.util.concurrent.TimeUnit

@KoinApiExtension
class LightningDamageWorkaroundListener(plugin: Plugin, private val config: Config): Listener {

    init { Bukkit.getPluginManager().registerEvents(this, plugin) }

    private val strikeLocations = CacheBuilder.newBuilder().expireAfterWrite(1000L, TimeUnit.MILLISECONDS).build<Location, UUID>()

    @EventHandler(priority = EventPriority.NORMAL)
    private fun InfernalLightningStrike.cacheLightningLocation() {
        strikeLocations.put(location, entity.uniqueId)
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    private fun EntityDamageByEntityEvent.onLightningFall() {
        if(!isLightningDamagingLivingEntity() || !cannotDamageItself) return

        val ownerUuid = strikeLocations.asMap().entries.firstOrNull { it.key.isAt(damager.location) }?.value ?: return
        // if the lightning was not fired by the damaged entity (or its rider), return
        if(!entity.isOwnerOrMountUuid(ownerUuid)) return
        // prevent the infernal mob, owner of that lightning, from damaging itself or its mount
        isCancelled = true
        damage = 0.0
    }

    private fun Location.isAt(other: Location) = x == other.x && y == other.y && z == other.z && world.uid == other.world.uid

    private fun Entity.isOwnerOrMountUuid(uuid: UUID) = uniqueId == uuid || passengers.any { it.uniqueId == uuid }

    private fun EntityDamageByEntityEvent.isLightningDamagingLivingEntity() = damager.type == EntityType.LIGHTNING && cause == EntityDamageEvent.DamageCause.LIGHTNING && entity is LivingEntity

    private val cannotDamageItself
        get() = config.get<Boolean>(ConfigKeys.INFERNALS_CANNOT_DAMAGE_THEMSELVES)
}
