package com.github.secretx33.infernalmobsreloaded.utils

import com.github.secretx33.infernalmobsreloaded.model.CustomEnchantment
import net.kyori.adventure.text.Component
import org.bukkit.Color
import org.bukkit.DyeColor
import org.bukkit.Material
import org.bukkit.block.Banner
import org.bukkit.block.banner.Pattern
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BannerMeta
import org.bukkit.inventory.meta.BlockStateMeta
import org.bukkit.inventory.meta.LeatherArmorMeta
import org.bukkit.material.Colorable
import org.bukkit.persistence.PersistentDataContainer

class ItemBuilder private constructor(material: Material) {

    private val item = ItemStack(material)
    private var meta = item.itemMeta

    init { require(material.isItem) { "Material $material is not an item" } }

    fun amount(amount: Int): ItemBuilder {
        item.amount = amount
        return this
    }

    fun displayName(name: Component): ItemBuilder {
        meta?.displayName(name)
        return this
    }

    fun flags(vararg flags: ItemFlag): ItemBuilder {
        meta?.addItemFlags(*flags)
        return this
    }

    fun flags(flags: Collection<ItemFlag>): ItemBuilder {
        flags.forEach { meta?.addItemFlags(it) }
        return this
    }

    fun hasFlag(flag: ItemFlag): Boolean = meta?.hasItemFlag(flag) == true

    fun addEnchantment(enchant: Enchantment, level: Int): ItemBuilder {
        meta?.addEnchant(enchant, level, true)
        return this
    }

    fun addEnchantments(enchants: Collection<CustomEnchantment>): ItemBuilder {
        enchants.forEach { it.get().ifPresent { (enchant, level) -> meta?.addEnchant(enchant, level, true) } }
        return this
    }

    fun color(color: Color?): ItemBuilder {
        (meta as? LeatherArmorMeta)?.setColor(color)
        return this
    }

    fun dyeColor(dyeColor: DyeColor?): ItemBuilder {
        if(dyeColor == null) return this
        item.itemMeta = meta
        (item as? Colorable)?.color = dyeColor
        meta = item.itemMeta
        return this
    }

    fun bannerPatterns(patterns: List<Pattern>): ItemBuilder {
        if(patterns.isEmpty()) return this
        (meta as? BannerMeta)?.patterns = patterns
        return this
    }

    fun shieldPatterns(baseColor: DyeColor?, patterns: Collection<Pattern>): ItemBuilder {
        if(baseColor == null && patterns.isEmpty()) return this
        val blockMeta = meta as? BlockStateMeta ?: return this
        val banner = blockMeta.blockState as? Banner ?: return this
        banner.let {
            if(baseColor != null) it.baseColor = baseColor
            patterns.forEach { pattern -> it.addPattern(pattern) }
            it.update()
            blockMeta.blockState = it
        }
        return this
    }

    fun addLore(lines: List<Component>): ItemBuilder {
        meta?.lore(meta.lore()?.plus(lines))
        return this
    }

    fun addLore(vararg lines: Component) = addLore(lines.toList())

    fun lore(lines: List<Component>): ItemBuilder {
        meta?.lore(lines)
        return this
    }

    fun lore(vararg lines: Component) = lore(lines.toList())

    fun markWithInfernalTag(name: String): ItemBuilder {
        meta?.markWithInfernalTag(name)
        return this
    }

    fun pdc(block: (PersistentDataContainer) -> Unit): ItemBuilder {
        meta?.persistentDataContainer?.let { block(it) }
        return this
    }

    fun build() = item.apply { itemMeta = meta }

    companion object {
        fun from(material: Material) = ItemBuilder(material)
    }
}
