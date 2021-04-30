package com.github.secretx33.infernalmobsreloaded.eventlisteners.sideeffectsmitigation

import com.github.secretx33.infernalmobsreloaded.model.KeyChain
import com.github.secretx33.infernalmobsreloaded.utils.pdc
import com.github.secretx33.infernalmobsreloaded.utils.toUuid
import org.bukkit.Bukkit
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

@KoinApiExtension
class FireworkDamageWorkaroundListener(plugin: Plugin, private val keyChain: KeyChain): Listener {

    init { Bukkit.getPluginManager().registerEvents(this, plugin) }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    private fun EntityDamageByEntityEvent.onFireworkExplosion() {
        if(!isFireworkDamagingLivingEntity()) return

        val ownerUuid = damager.pdc.get(keyChain.fireworkOwnerUuidKey, PersistentDataType.STRING)?.toUuid() ?: return
        // if the firework was not fired by the damaged entity, return
        if(entity.uniqueId != ownerUuid) return
        // prevent the infernal mob, owner of that firework, from damaging itself
        isCancelled = true
        damage = 0.0
    }

    private fun EntityDamageByEntityEvent.isFireworkDamagingLivingEntity() = damager.type == EntityType.FIREWORK && cause == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION && entity is LivingEntity
}
