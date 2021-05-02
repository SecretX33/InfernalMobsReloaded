package com.github.secretx33.infernalmobsreloaded.eventlisteners.ability

import com.github.secretx33.infernalmobsreloaded.config.AbilityConfig
import com.github.secretx33.infernalmobsreloaded.config.AbilityConfigKeys
import com.github.secretx33.infernalmobsreloaded.model.KeyChain
import com.github.secretx33.infernalmobsreloaded.utils.pdc
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
class FireworkDamageIncreaseListener (
    plugin: Plugin,
    private val abilityConfig: AbilityConfig,
    private val keyChain: KeyChain,
): Listener {

    init { Bukkit.getPluginManager().registerEvents(this, plugin) }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    private fun EntityDamageByEntityEvent.onFireworkExplosion() {
        // it's not a firework or the firework was not fired by an infernal
        if(!isFireworkDamagingLivingEntity() || !damager.wasFiredByInfernal()) return

        val damageMulti = abilityConfig.getDoublePair(AbilityConfigKeys.FIREWORK_DAMAGE_MULTIPLIER).getRandomBetween()
        damage *= damageMulti
    }

    private fun Entity.wasFiredByInfernal() = pdc.has(keyChain.fireworkOwnerUuidKey, PersistentDataType.STRING)

    private fun EntityDamageByEntityEvent.isFireworkDamagingLivingEntity() = damager.type == EntityType.FIREWORK && cause == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION && entity is LivingEntity

    private fun Pair<Double, Double>.getRandomBetween(): Double {
        val (minValue, maxValue) = this
        return minValue + (maxValue - minValue) * random.nextDouble()
    }

    private companion object {
        val random = Random()
    }
}
