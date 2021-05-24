package com.github.secretx33.infernalmobsreloaded.model.items

import com.github.secretx33.infernalmobsreloaded.model.CustomEnchantment
import net.kyori.adventure.text.Component
import org.bukkit.Color
import org.bukkit.DyeColor
import org.bukkit.Material
import org.bukkit.block.banner.Pattern
import org.bukkit.inventory.ItemFlag

class BannerLootItem(
    name: String,
    displayName: Component,
    material: Material,
    color: Color?,
    dyeColor: DyeColor?,
    minAmount: Int,
    maxAmount: Int,
    flags: Set<ItemFlag>,
    lore: List<Component>,
    enchants: Set<CustomEnchantment>,
    private val patterns: List<Pattern>,
): NormalLootItem(name, displayName, material, color, dyeColor, minAmount, maxAmount, flags, lore, enchants) {

    override fun makeItem() = preparedItem
        .bannerPatterns(patterns)
        .build()
}
