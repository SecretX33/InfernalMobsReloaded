package com.github.secretx33.infernalmobsreloaded.eventlisteners.sideeffectsmitigation

import com.github.secretx33.infernalmobsreloaded.model.KeyChain
import com.github.secretx33.infernalmobsreloaded.utils.pdc
import com.github.secretx33.infernalmobsreloaded.utils.toUuid
import org.bukkit.Bukkit
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin
import org.koin.core.component.KoinApiExtension
import java.util.*

@KoinApiExtension
class LightningDamageWorkaroundListener(plugin: Plugin, private val keyChain: KeyChain): Listener {

    init { Bukkit.getPluginManager().registerEvents(this, plugin) }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    private fun EntityDamageByEntityEvent.onLightningFall() {
        if(!isLightningDamagingLivingEntity()) return

        val ownerUuid = damager.pdc.get(keyChain.fireworkOwnerUuidKey, PersistentDataType.STRING)?.toUuid() ?: return
        // if the lightning was not fired by the damaged entity (or its rider), return
        if(!entity.isOwnerOrMountUuid(ownerUuid)) return
        // prevent the infernal mob, owner of that lightning, from damaging itself or its mount
        isCancelled = true
        damage = 0.0
    }

    private fun Entity.isOwnerOrMountUuid(uuid: UUID) = uniqueId == uuid || passengers.any { it.uniqueId == uuid }

    private fun EntityDamageByEntityEvent.isLightningDamagingLivingEntity() = damager.type == EntityType.LIGHTNING && cause == EntityDamageEvent.DamageCause.LIGHTNING && entity is LivingEntity
}
