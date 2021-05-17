package com.github.secretx33.infernalmobsreloaded.model.items

import com.github.secretx33.infernalmobsreloaded.model.CustomEnchantment
import net.kyori.adventure.text.Component
import org.bukkit.Color
import org.bukkit.DyeColor
import org.bukkit.Material
import org.bukkit.block.banner.PatternType

class ShieldWithPatternLootItem(
    name: String,
    displayName: Component,
    material: Material,
    color: Color?,
    dyeColor: DyeColor?,
    minAmount: Int,
    maxAmount: Int,
    lore: List<Component>,
    enchants: Set<CustomEnchantment>,
    private val patterns: List<Pair<PatternType, DyeColor>>,
): NormalLootItem(name, displayName, material, color, dyeColor, minAmount, maxAmount, lore, enchants) {

    override fun makeItem() = preparedItem
        .patterns(dyeColor, patterns)
        .build()
}
