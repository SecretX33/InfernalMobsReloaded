package com.github.secretx33.infernalmobsreloaded.model

import com.github.secretx33.infernalmobsreloaded.model.items.LootItem
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
    private val minAbilities: Int,
    private val maxAbilities: Int,
    private val minFollowRangeMulti: Double,
    private val maxFollowRangeMulti: Double,
    private val minDamageMulti: Double,
    private val maxDamageMulti: Double,
    private val minAttackKnockbackMod: Double,
    private val maxAttackKnockbackMod: Double,
    private val minHealthMulti: Double,
    private val maxHealthMulti: Double,
    private val minSpeedMulti: Double,
    private val maxSpeedMulti: Double,
    val consoleCommand: String,
    private val loots: Map<LootItem, Double>,
) {
    val entityClass: Class<out Entity> = entityType.entityClass ?: throw IllegalArgumentException("entityClass cannot be null")

    init {
        require(spawnChance in 0.0..1.0) { "spawnChance needs to be within 0 and 1, spawnChance = $spawnChance" }
        require(mobSpawnerDropChance in 0.0..1.0) { "mobSpawnerDropChance needs to be within 0 and 1, mobSpawnerDropChance = $mobSpawnerDropChance" }
        require(entityType.isSpawnable) { "entityType needs to be spawnable, $entityType is not" }
        require(entityClass !is ComplexLivingEntity) { "entityClass cannot be a ComplexLivingEntity" }
        require(loots.values.all { it in 0.0..1.0 }) { "all loot chances must be within 0 and 1, something inside the loop map was not, map = $loots"}

        // abilities amount
        require(minAbilities >= 0) { "minAbilities has to be at least 0, minAbilities = $minAbilities" }
        require(maxAbilities >= 0) { "maxAbilities has to be at least 0, maxAbilities = $maxAbilities" }
        require(minAbilities <= maxAbilities) { "minAbilities cannot be higher than maxAbilities, minAbilities = $minAbilities, maxAbilities = $maxAbilities" }

        // follow range multiplier
        require(minFollowRangeMulti >= 0) { "minFollowRangeMulti has to be higher than 0, minFollowRangeMulti = $minFollowRangeMulti" }
        require(maxFollowRangeMulti >= 0) { "maxFollowRangeMulti has to be higher than 0, maxFollowRangeMulti = $maxFollowRangeMulti" }
        require(minFollowRangeMulti <= maxFollowRangeMulti) { "minFollowRangeMulti cannot be higher than maxFollowRangeMulti, minFollowRangeMulti = $minFollowRangeMulti, maxFollowRangeMulti = $maxFollowRangeMulti" }

        // damage multiplier
        require(minDamageMulti >= 0) { "minDamageMulti has to be higher than 0, minDamageMulti = $minDamageMulti" }
        require(maxDamageMulti >= 0) { "maxDamageMulti has to be higher than 0, maxDamageMulti = $maxDamageMulti" }
        require(minDamageMulti <= maxDamageMulti) { "minDamageMulti cannot be higher than maxDamageMulti, minDamageMulti = $minDamageMulti, maxDamageMulti = $maxDamageMulti" }

        // attack knockback multiplier
        require(minAttackKnockbackMod <= maxAttackKnockbackMod) { "minAttackKnockbackMulti cannot be higher than maxAttackKnockbackMulti, minAttackKnockbackMulti = $minAttackKnockbackMod, maxAttackKnockbackMulti = $maxAttackKnockbackMod" }

        // health multiplier
        require(minHealthMulti > 0) { "minHealthMulti has to be higher than 0, minHealthMulti = $minHealthMulti" }
        require(maxHealthMulti > 0) { "maxHealthMulti has to be higher than 0, maxHealthMulti = $maxHealthMulti" }
        require(minHealthMulti <= maxHealthMulti) { "minHealthMulti cannot be higher than maxHealthMulti, minHealthMulti = $minHealthMulti, maxHealthMulti = $maxHealthMulti" }

        // speed multiplier
        require(minSpeedMulti >= 0) { "minSpeedMulti has to be higher than 0, minSpeedMulti = $minSpeedMulti" }
        require(maxSpeedMulti >= 0) { "maxSpeedMulti has to be higher than 0, maxSpeedMulti = $maxSpeedMulti" }
        require(minSpeedMulti <= maxSpeedMulti) { "minSpeedMulti cannot be higher than maxSpeedMulti, minSpeedMulti = $minSpeedMulti, maxSpeedMulti = $maxSpeedMulti" }
    }

    fun getAbilityNumber() = random.nextInt(maxAbilities - minAbilities + 1) + minAbilities

    fun getFollowRangeMulti() = minFollowRangeMulti + (maxFollowRangeMulti - minFollowRangeMulti) * random.nextDouble()

    fun getDamageMulti() = minDamageMulti + (maxDamageMulti - minDamageMulti) * random.nextDouble()

    fun getAtkKnockbackMod() = minAttackKnockbackMod + (maxAttackKnockbackMod - minAttackKnockbackMod) * random.nextDouble()

    fun getHealthMulti() = minHealthMulti + (maxHealthMulti - minHealthMulti) * random.nextDouble()

    fun getSpeedMulti() = minSpeedMulti + (maxSpeedMulti - minSpeedMulti) * random.nextDouble()

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
