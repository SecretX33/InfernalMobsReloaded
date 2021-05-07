package com.github.secretx33.infernalmobsreloaded.events

import com.github.secretx33.infernalmobsreloaded.model.InfernalMobType
import org.bukkit.entity.LivingEntity
import org.koin.core.component.KoinApiExtension

@KoinApiExtension
class InfernalHealedEvent (
    entity: LivingEntity,
    infernalType: InfernalMobType,
    val amountHealed: Double,  // effective heal
) : InfernalEntityEvent(entity, infernalType) {

    init { require(amountHealed > 0) { "amountHealed cannot be 0 or less, amountHealed = $amountHealed" } }
}

