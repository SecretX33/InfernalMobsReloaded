package com.github.secretx33.infernalmobsreloaded.model

import org.bukkit.enchantments.Enchantment
import java.util.*

data class CustomEnchantment (
    private val type: Enchantment,
    private val minLevel: Int,
    private val maxLevel: Int,
    private val chance: Double,
) {
    init {
        require(minLevel >= 0) { "minLevel has to be a number equal to or higher than 0, value passed was $minLevel" }
        require(maxLevel >= 0) { "maxLevel has to be a number equal to or higher than 0, value passed was $maxLevel" }
        require(minLevel <= maxLevel) { "minLevel cannot be higher than maxLevel, values passed minLevel = $minLevel and maxLevel = $maxLevel" }
        require(chance in 0.0..1.0) { "chance has to be a value between 0 and 1, but chance = $chance" }
    }

    fun get(): Optional<Pair<Enchantment, Int>> {
        // returns empty optional if the 'try' for get the enchantment is not successful
        if(random.nextDouble() > chance) return Optional.empty()
        val level = random.nextInt(maxLevel - minLevel + 1) + minLevel
        return Optional.of(Pair(type, level))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (type != (other as CustomEnchantment).type) return false
        return true
    }

    override fun hashCode() = type.hashCode()

    private companion object {
        val random = Random()
    }
}

