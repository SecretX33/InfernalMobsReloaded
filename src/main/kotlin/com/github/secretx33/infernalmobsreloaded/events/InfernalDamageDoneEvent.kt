package com.github.secretx33.infernalmobsreloaded.events

import com.github.secretx33.infernalmobsreloaded.model.InfernalMobType
import org.bukkit.entity.LivingEntity
import org.bukkit.event.Cancellable

class InfernalDamageDoneEvent (
    entity: LivingEntity,
    val defender: LivingEntity,
    infernalType: InfernalMobType,
) : InfernalEntityEvent(entity, infernalType), Cancellable {

    var damageMulti: Double = 1.0

    private var isCancelled = false

    override fun isCancelled(): Boolean = isCancelled

    override fun setCancelled(isCancelled: Boolean) {
        this.isCancelled = isCancelled
    }
}
