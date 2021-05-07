package com.github.secretx33.infernalmobsreloaded.events

import com.github.secretx33.infernalmobsreloaded.model.InfernalMobType
import org.bukkit.entity.LivingEntity
import org.bukkit.event.Cancellable
import org.koin.core.component.KoinApiExtension

@KoinApiExtension
class InfernalDeathEvent (
    entity: LivingEntity,
    infernalType: InfernalMobType,
) : InfernalEntityEvent(entity, infernalType), Cancellable {

    private var isCancelled = false

    override fun isCancelled(): Boolean = isCancelled

    override fun setCancelled(isCancelled: Boolean) {
        this.isCancelled = isCancelled
    }
}
