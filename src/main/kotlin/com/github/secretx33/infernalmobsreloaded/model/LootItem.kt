package com.github.secretx33.infernalmobsreloaded.model

import com.github.secretx33.infernalmobsreloaded.utils.ItemBuilder
import net.kyori.adventure.text.Component
import org.bukkit.Color
import org.bukkit.DyeColor
import org.bukkit.Material
import java.util.*

data class LootItem (
    val name: String,
    val displayName: Component,
    val material: Material,
    val color: Color,
    val dyeColor: DyeColor,
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
        .amount(random.nextInt(maxAmount - minAmount + 1) + minAmount)
        .setLore(lore)
        .color(color)
        .dyeColor(dyeColor)
        .addEnchantments(enchants)
        .build()

    private companion object {
        val random = Random()
    }
}
