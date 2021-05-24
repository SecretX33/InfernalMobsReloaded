package com.github.secretx33.infernalmobsreloaded.model.items

import com.github.secretx33.infernalmobsreloaded.model.CustomEnchantment
import com.github.secretx33.infernalmobsreloaded.utils.ItemBuilder
import net.kyori.adventure.text.Component
import org.bukkit.Color
import org.bukkit.DyeColor
import org.bukkit.Material
import org.bukkit.inventory.ItemFlag
import java.util.*

open class NormalLootItem (
    final override val name: String,
    val displayName: Component,
    val material: Material,
    val color: Color?,
    val dyeColor: DyeColor?,
    val minAmount: Int,
    val maxAmount: Int,
    val flags: Set<ItemFlag>,
    val lore: List<Component>,
    val enchants: Set<CustomEnchantment>,
) : LootItem {

    init {
        require(name.isNotBlank()) { "LootItem name is invalid, it cannot be blank or empty" }
        require(material.isItem) { "material needs to be an item, and $material is not" }
        require(minAmount >= 1) { "minAmount has to be a number equal to or higher than 0, value passed was $minAmount" }
        require(maxAmount >= 1) { "maxAmount has to be a number equal to or higher than 0, value passed was $maxAmount" }
        require(minAmount <= maxAmount) { "minAmount cannot be higher than maxAmount, values passed minAmount = $minAmount and maxAmount = $maxAmount" }
    }

    protected val preparedItem = ItemBuilder.from(material)
        .displayName(displayName)
        .amount(random.nextInt(maxAmount - minAmount + 1) + minAmount)
        .flags(flags)
        .lore(lore)
        .color(color)
        .addEnchantments(enchants)
        .markWithInfernalTag(name)

    override fun makeItem() = preparedItem.dyeColor(dyeColor).build()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as NormalLootItem
        return name.equals(other.name, ignoreCase = true)
    }

    override fun hashCode() = name.lowercase(Locale.US).hashCode()

    private companion object {
        val random = Random()
    }
}
