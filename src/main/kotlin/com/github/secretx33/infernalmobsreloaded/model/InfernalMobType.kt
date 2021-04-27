package com.github.secretx33.infernalmobsreloaded.model

import net.kyori.adventure.text.Component
import org.bukkit.entity.ComplexLivingEntity
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import java.util.*

data class InfernalMobType (
    val name: String,
    val displayName: Component,
    val type: EntityType,
    val spawnChance: Double,
    private val loots: Map<LootItem, Double>,
) {
    val entityClass: Class<out Entity> = type.entityClass ?: throw IllegalArgumentException("entityClass cannot be null")

    init {
        require(spawnChance in 0.0..1.0) { "spawnChance needs to be within 0 and 1" }
        require(type.isSpawnable) { "type needs to be spawnable" }
        require(entityClass !is ComplexLivingEntity) { "entityClass cannot be a ComplexLivingEntity" }
        require(loots.values.all { it in 0.0..1.0 }) { "all loot chances must be within 0 and 1, something inside the loop map was not, map = $loots"}
    }

    fun getDrops() = loots.filterValues { random.nextDouble() <= it }.keys

    private companion object {
        val random = Random()
    }
}
