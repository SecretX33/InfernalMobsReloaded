package com.github.secretx33.infernalmobsreloaded.events

import com.github.secretx33.infernalmobsreloaded.model.InfernalMobType
import org.bukkit.Location
import org.bukkit.entity.LivingEntity

class InfernalLightningStrike (
    entity: LivingEntity,
    infernalType: InfernalMobType,
    val location: Location,
) : InfernalEntityEvent(entity, infernalType)
