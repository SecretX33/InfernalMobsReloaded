package com.github.secretx33.infernalmobsreloaded.model

import com.github.secretx33.infernalmobsreloaded.model.items.LootItem
import com.github.secretx33.infernalmobsreloaded.utils.random
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import org.bukkit.entity.ComplexLivingEntity
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import java.util.*

data class InfernalMobType (
    val name: String,
    val displayName: Component,
    val bossBarName: Component,
    val mobSpawnerName: Component,
    val bossBarColor: BossBar.Color,
    val bossBarOverlay: BossBar.Overlay,
    val bossBarFlags: Set<BossBar.Flag>,
    val entityType: EntityType,
    val spawnChance: Double,
    val mobSpawnerDropChance: Double,
    private val numAbilities: Pair<Int, Int>,
    private val followRangeMulti: Pair<Double, Double>,
    private val damageMulti: Pair<Double, Double>,
    private val attackKnockbackMod: Pair<Double, Double>,
    private val healthMulti: Pair<Double, Double>,
    private val speedMulti: Pair<Double, Double>,
    val consoleCommands: List<String>,
    val forcedAbilities: Set<Ability>,
    private val loots: Map<LootItem, Double>,
) {
    val entityClass: Class<out Entity> = entityType.entityClass ?: throw IllegalArgumentException("entityClass cannot be null")

    init {
        require(name.isNotBlank()) { "name cannot be blank, name = '$name'" }
        require(spawnChance in 0.0..1.0) { "spawnChance needs to be within 0 and 1, spawnChance = $spawnChance" }
        require(mobSpawnerDropChance in 0.0..1.0) { "mobSpawnerDropChance needs to be within 0 and 1, mobSpawnerDropChance = $mobSpawnerDropChance" }
        require(entityType.isSpawnable) { "entityType needs to be spawnable, $entityType is not" }
        require(entityClass !is ComplexLivingEntity) { "entityClass cannot be a ComplexLivingEntity" }
        require(loots.values.all { it in 0.0..1.0 }) { "all loot chances must be within 0 and 1, something inside the loop map was not, map = $loots"}

        // abilities amount
        require(numAbilities.first >= 0 && numAbilities.second >= 0) { "numAbilities cannot be lower than 0, numAbilities = $numAbilities" }
        require(numAbilities.first <= numAbilities.second) { "numAbilities first value has to be lower or equal than the second value, numAbilities = $numAbilities" }

        // follow range multiplier
        require(followRangeMulti.first >= 0 && followRangeMulti.second >= 0) { "followRangeMulti cannot be lower than 0, followRangeMulti = $followRangeMulti" }
        require(followRangeMulti.first <= followRangeMulti.second) { "followRangeMulti first value has to be lower or equal than the second value, followRangeMulti = $followRangeMulti" }

        // damage multiplier
        require(damageMulti.first >= 0 && damageMulti.second >= 0) { "damageMulti cannot be lower than 0, damageMulti = $damageMulti" }
        require(damageMulti.first <= damageMulti.second) { "damageMulti first value has to be lower or equal than the second value, damageMulti = $damageMulti" }

        // attack knockback multiplier
        require(attackKnockbackMod.first <= attackKnockbackMod.second) { "attackKnockbackMod first value has to be lower or equal than the second value, attackKnockbackMod = $attackKnockbackMod" }

        // health multiplier
        require(healthMulti.first > 0 && healthMulti.second > 0) { "healthMulti cannot be lower than 0, healthMulti = $healthMulti" }
        require(healthMulti.first <= healthMulti.second) { "healthMulti first value has to be lower or equal than the second value, healthMulti = $healthMulti" }

        // speed multiplier
        require(speedMulti.first >= 0 && speedMulti.second >= 0) { "speedMulti cannot be lower than 0, speedMulti = $speedMulti" }
        require(speedMulti.first <= speedMulti.second) { "speedMulti first value has to be lower or equal than the second value, speedMulti = $speedMulti" }
    }

    fun getAbilityNumber() = numAbilities.random()

    fun getFollowRangeMulti() = followRangeMulti.random()

    fun getDamageMulti() = damageMulti.random()

    fun getAtkKnockbackMod() = attackKnockbackMod.random()

    fun getHealthMulti() = healthMulti.random()

    fun getSpeedMulti() = speedMulti.random()

    fun getLoots() = loots.asSequence().filter { random.nextDouble() <= it.value }.map { it.key.makeItem() }.toList()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as InfernalMobType
        if (!name.equals(other.name, ignoreCase = true)) return false
        if (entityType != other.entityType) return false

        return true
    }

    override fun hashCode() = Objects.hash(name.lowercase(Locale.US), entityType)

    private companion object {
        val random = Random()
    }
}
