package com.github.secretx33.infernalmobsreloaded.model

import org.bukkit.entity.ComplexLivingEntity
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType

data class InfernalMobType (
    val displayName: String,
    val type: EntityType,
    val spawnChance: Double,
) {
    val entityClass: Class<out Entity> = type.entityClass ?: throw IllegalArgumentException("entityClass cannot be null")

    init {
        require(displayName.isNotBlank()) { "displayName cannot be blank" }
        require(spawnChance in 0.0..1.0) { "spawnChance needs to be within 0 and 1" }
        require(type.isSpawnable) { "type needs to be spawnable" }
        require(entityClass !is ComplexLivingEntity) { "type.entityClass cannot be a ComplexLivingEntity" }
    }
}
