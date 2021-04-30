package com.github.secretx33.infernalmobsreloaded.model

import net.kyori.adventure.text.Component
import org.bukkit.entity.ComplexLivingEntity
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import java.util.*

data class InfernalMobType (
    val name: String,
    val displayName: Component,
    val bossBarName: Component,
    val entityType: EntityType,
    val spawnChance: Double,
    private val minAbilities: Int,
    private val maxAbilities: Int,
    private val minHealthMulti: Double,
    private val maxHealthMulti: Double,
    private val loots: Map<LootItem, Double>,
) {
    val entityClass: Class<out Entity> = entityType.entityClass ?: throw IllegalArgumentException("entityClass cannot be null")

    init {
        require(spawnChance in 0.0..1.0) { "spawnChance needs to be within 0 and 1, spawnChance = $spawnChance" }
        require(entityType.isSpawnable) { "entityType needs to be spawnable, $entityType is not" }
        require(entityClass !is ComplexLivingEntity) { "entityClass cannot be a ComplexLivingEntity" }
        require(loots.values.all { it in 0.0..1.0 }) { "all loot chances must be within 0 and 1, something inside the loop map was not, map = $loots"}

        // abilities amount
        require(minAbilities >= 0) { "minAbilities has to be at least 0, minAbilities = $minAbilities" }
        require(maxAbilities >= 0) { "maxAbilities has to be at least 0, maxAbilities = $maxAbilities" }
        require(minAbilities <= maxAbilities) { "minAbilities cannot be higher than maxAbilities, minAbilities = $minAbilities, maxAbilities = $maxAbilities" }

        // health multiplier
        require(minHealthMulti > 0) { "minHealthMulti has to be higher than 0, minHealthMulti = $minHealthMulti" }
        require(maxHealthMulti > 0) { "maxHealthMulti has to be higher than 0, maxHealthMulti = $maxHealthMulti" }
        require(minHealthMulti <= maxHealthMulti) { "minHealthMulti cannot be higher than maxHealthMulti, minHealthMulti = $minHealthMulti, maxHealthMulti = $maxHealthMulti" }
    }

    fun getDrops() = loots.filterValues { random.nextDouble() <= it }.keys

    fun getAbilityNumber() = random.nextInt(maxAbilities - minAbilities + 1) + minAbilities

    fun getHealthMulti() = minHealthMulti + (maxHealthMulti - minHealthMulti) * random.nextDouble()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as InfernalMobType
        if (!name.equals(other.name, ignoreCase = true)) return false
        if (entityType != other.entityType) return false

        return true
    }

    fun getLoots() = loots.asSequence().filter { random.nextDouble() <= it.value }.map { it.key.makeItem() }.toList()

    override fun hashCode() = Objects.hash(name.toLowerCase(Locale.US), entityType)

    private companion object {
        val random = Random()
    }
}
