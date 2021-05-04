package com.github.secretx33.infernalmobsreloaded.eventlisteners.ability

import com.github.secretx33.infernalmobsreloaded.config.AbilityConfig
import com.github.secretx33.infernalmobsreloaded.config.AbilityConfigKeys
import com.github.secretx33.infernalmobsreloaded.config.Config
import com.github.secretx33.infernalmobsreloaded.config.ConfigKeys
import com.github.secretx33.infernalmobsreloaded.model.KeyChain
import com.github.secretx33.infernalmobsreloaded.utils.getRandomBetween
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
class FireworkAbilityListener (
    plugin: Plugin,
    private val config: Config,
    private val abilityConfig: AbilityConfig,
    private val keyChain: KeyChain,
): Listener {

    init { Bukkit.getPluginManager().registerEvents(this, plugin) }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    private fun EntityDamageByEntityEvent.fireworkDamagingOwner() {
        if(!isFireworkDamagingLivingEntity() || !cannotDamageItself) return

        val ownerUuid = damager.pdc.get(keyChain.fireworkOwnerUuidKey, PersistentDataType.STRING)?.toUuid() ?: return
        // if the firework was not fired by the damaged entity (or its rider), return
        if(!entity.isOwnerOrMountUuid(ownerUuid)) return
        // prevent the infernal mob, owner of that firework, from damaging itself or its mount
        isCancelled = true
        damage = 0.0
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    private fun EntityDamageByEntityEvent.onFireworkExplosion() {
        // it's not a firework or the firework was not fired by an infernal
        if(!isFireworkDamagingLivingEntity() || !damager.wasFiredByInfernal()) return

        // multiply the damage caused by fireworks
        damage *= fireworkDmgMulti
    }

    private val fireworkDmgMulti
        get() = abilityConfig.getDoublePair(AbilityConfigKeys.FIREWORK_DAMAGE_MULTIPLIER).getRandomBetween()

    private fun Entity.wasFiredByInfernal() = pdc.has(keyChain.fireworkOwnerUuidKey, PersistentDataType.STRING)

    private fun EntityDamageByEntityEvent.isFireworkDamagingLivingEntity() = damager.type == EntityType.FIREWORK && cause == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION && entity is LivingEntity

    private fun Entity.isOwnerOrMountUuid(uuid: UUID) = uniqueId == uuid || passengers.any { it.uniqueId == uuid }

    private val cannotDamageItself
        get() = config.get<Boolean>(ConfigKeys.INFERNALS_CANNOT_DAMAGE_THEMSELVES)
}
