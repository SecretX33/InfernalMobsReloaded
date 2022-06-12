package com.github.secretx33.infernalmobsreloaded.event

import com.github.secretx33.infernalmobsreloaded.model.InfernalMobType
import org.bukkit.entity.LivingEntity

class InfernalHealedEvent (
    entity: LivingEntity,
    infernalType: InfernalMobType,
    val amountHealed: Double,  // effective heal
) : InfernalEntityEvent(entity, infernalType) {

    init { require(amountHealed > 0) { "amountHealed cannot be 0 or less, amountHealed = $amountHealed" } }
}

