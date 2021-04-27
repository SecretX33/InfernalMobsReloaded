package com.github.secretx33.infernalmobsreloaded.model

import com.github.secretx33.infernalmobsreloaded.utils.ItemBuilder
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import java.util.*

data class LootItem (
    val name: String,
    val displayName: Component,
    val material: Material,
    val minAmount: Int,
    val maxAmount: Int,
    val lore: List<Component>,
    val enchants: Set<CustomEnchantment>,
) {
    init {
        require(name.isNotBlank()) { "LootItem name is invalid, it cannot be blank or empty" }
        require(material.isItem) { "material needs to be an item, and $material is not" }
        require(minAmount >= 1) { "minAmount has to be a number equal to or higher than 0, value passed was $minAmount" }
        require(maxAmount >= 1) { "maxAmount has to be a number equal to or higher than 0, value passed was $maxAmount" }
        require(minAmount <= maxAmount) { "minAmount cannot be higher than maxAmount, values passed minAmount = $minAmount and maxAmount = $maxAmount" }
    }

    fun makeItem() = ItemBuilder.from(material)
        .displayName(displayName)
        .amount(random.nextInt(maxAmount - minAmount) + minAmount)
        .setLore(lore)
        .addEnchantments(enchants)
        .build()

    private companion object {
        val random = Random()
    }
}

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
        val level = random.nextInt(maxLevel - minLevel) + minLevel
        return Optional.of(Pair(type, level))
    }

    private companion object {
        val random = Random()
    }
}
