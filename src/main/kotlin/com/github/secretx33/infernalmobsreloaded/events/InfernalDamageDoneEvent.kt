package com.github.secretx33.infernalmobsreloaded.events

import com.github.secretx33.infernalmobsreloaded.model.InfernalMobType
import org.bukkit.entity.LivingEntity
import org.bukkit.event.Cancellable
import org.bukkit.event.entity.EntityDamageEvent
import org.koin.core.component.KoinApiExtension

@KoinApiExtension
class InfernalDamageDoneEvent (
    entity: LivingEntity,
    val defender: LivingEntity,
    val damage: Double,
    val cause: EntityDamageEvent.DamageCause,
    infernalType: InfernalMobType,
) : InfernalEntityEvent(entity, infernalType), Cancellable {

    var damageMulti: Double = 1.0

    val finalDamage: Double
        get() = damage * damageMulti

    private var isCancelled = false

    override fun isCancelled(): Boolean = isCancelled

    override fun setCancelled(isCancelled: Boolean) {
        this.isCancelled = isCancelled
    }
}
