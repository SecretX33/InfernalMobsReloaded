package com.github.secretx33.infernalmobsreloaded.model.items

import com.github.secretx33.infernalmobsreloaded.model.CustomEnchantment
import com.github.secretx33.infernalmobsreloaded.util.ItemBuilder
import com.github.secretx33.infernalmobsreloaded.util.extension.random
import net.kyori.adventure.text.Component
import org.bukkit.Color
import org.bukkit.DyeColor
import org.bukkit.Material
import org.bukkit.inventory.ItemFlag
import java.util.Locale

open class NormalLootItem (
    final override val name: String,
    val displayName: Component,
    val material: Material,
    val color: Color?,
    val dyeColor: DyeColor?,
    val amount: Pair<Int, Int>,
    val flags: Set<ItemFlag>,
    val lore: List<Component>,
    val enchants: Set<CustomEnchantment>,
) : LootItem {

    init {
        require(name.isNotBlank()) { "LootItem name is invalid, it cannot be blank or empty" }
        require(material.isItem) { "material needs to be an item, and $material is not" }
        require(amount.first >= 1 && amount.second >= 1) { "amount cannot have values lower than 1, but amount = $amount" }
        require(amount.first <= amount.second) { "min amount cannot be higher than the max amount, value passed was amount = $amount" }
    }

    protected val preparedItem
        get() = ItemBuilder.from(material)
            .displayName(displayName)
            .amount(amount.random())
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
}
