package com.github.secretx33.infernalmobsreloaded.utils

import com.github.secretx33.infernalmobsreloaded.model.CustomEnchantment
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataContainer

class ItemBuilder private constructor(material: Material) {

    private val item = ItemStack(material)
    private val meta = item.itemMeta

    init { require(material.isItem) { "Material $material is not an item" } }

    fun amount(amount: Int): ItemBuilder {
        item.amount = amount
        return this
    }

    fun displayName(name: Component): ItemBuilder {
        meta?.displayName(name)
        return this
    }

    fun addFlags(vararg flags: ItemFlag): ItemBuilder {
        meta?.addItemFlags(*flags)
        return this
    }

    fun addEnchantment(enchant: Enchantment, level: Int): ItemBuilder {
        meta?.addEnchant(enchant, level, true)
        return this
    }

    fun addEnchantments(enchants: Collection<CustomEnchantment>): ItemBuilder {
        enchants.forEach { it.get().ifPresent { (enchant, level) -> meta?.addEnchant(enchant, level, true) } }
        return this
    }

    fun addLore(lines: List<Component>): ItemBuilder {
        meta?.lore(meta.lore()?.plus(lines))
        return this
    }

    fun addLore(vararg lines: Component) = addLore(lines.toList())

    fun setLore(lines: List<Component>): ItemBuilder {
        meta.lore(lines)
        return this
    }

    fun setLore(vararg lines: Component) = setLore(lines.toList())

    fun pdc(block: (PersistentDataContainer) -> Unit): ItemBuilder {
        meta?.persistentDataContainer?.let { block(it) }
        return this
    }

    fun build() = item.apply { itemMeta = meta }

    companion object {
        fun from(material: Material) = ItemBuilder(material)
    }
}
