package com.github.secretx33.infernalmobsreloaded.model

import com.github.secretx33.infernalmobsreloaded.util.extension.random
import org.bukkit.enchantments.Enchantment
import java.util.Optional
import java.util.Random

data class CustomEnchantment (
    private val type: Enchantment,
    private val levels: Pair<Int, Int>,
    private val chance: Double,
) {
    init {
        require(levels.first >= 1 && levels.second >= 1) { "level cannot have values lower than 1, but level = $levels" }
        require(levels.first <= levels.second) { "min level cannot be higher than the max level, value passed was level = $levels" }
        require(chance in 0.0..1.0) { "chance has to be a value between 0 and 1, but chance = $chance" }
    }

    fun get(): Optional<Pair<Enchantment, Int>> {
        // returns empty optional if the 'try' for get the enchantment is not successful
        if(random.nextDouble() > chance) return Optional.empty()
        return Optional.of(Pair(type, levels.random()))
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

