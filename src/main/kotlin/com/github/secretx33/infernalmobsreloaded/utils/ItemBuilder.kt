package com.github.secretx33.infernalmobsreloaded.utils

import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataContainer

class ItemBuilder(material: Material) {

    private val item = ItemStack(material)
    private val meta = item.itemMeta

    init { require(material.isItem) { "Material $material is not an item" } }

    fun amount(amount: Int): ItemBuilder {
        item.amount = amount
        return this
    }

    fun setDisplayName(name: String): ItemBuilder {
        meta?.setDisplayName(name)
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

    fun addLore(lines: List<String>): ItemBuilder {
        meta?.lore = meta?.lore?.plus(lines)
        return this
    }

    fun addLore(vararg lines: String) = addLore(lines.toList())

    fun setLore(lines: List<String>): ItemBuilder {
        meta?.lore = lines
        return this
    }

    fun setLore(vararg lines: String) = setLore(lines.toList())

    fun pdc(block: (PersistentDataContainer) -> Unit): ItemBuilder {
        meta?.persistentDataContainer?.let { block(it) }
        return this
    }

    fun build() = item.apply { itemMeta = meta }
}
