package com.github.secretx33.infernalmobsreloaded.model.items

import com.github.secretx33.infernalmobsreloaded.model.CustomEnchantment
import com.github.secretx33.infernalmobsreloaded.utils.extension.formattedTypeName
import com.github.secretx33.infernalmobsreloaded.utils.extension.markWithInfernalTag
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BookMeta

data class LootBook (
    override val name: String,
    private val material: Material,
    private val title: Component,
    private val author: Component?,
    private val generation: BookMeta.Generation,
    private val pages: List<Component>,
    private val flags: Set<ItemFlag>,
    private val enchants: Set<CustomEnchantment>,
) : LootItem {

    init {
        require(material == Material.WRITTEN_BOOK || material == Material.WRITABLE_BOOK) { "book material has to be either written or writtable book, but ${material.formattedTypeName()} is not" }
    }

    private val book: ItemStack = generateBook()

    private fun generateBook(): ItemStack {
        val item = ItemStack(material)
        (item.itemMeta as? BookMeta)?.let { meta ->
            title.let {
                meta.displayName(it)
                meta.title(it)
            }
            author?.let { meta.author(author) }
            meta.generation = generation
            meta.pages(pages)
            flags.forEach { meta.addItemFlags(it) }
            enchants.forEach { it.get().ifPresent { (enchant, level) -> meta.addEnchant(enchant, level, true) } }
            item.itemMeta = meta.markWithInfernalTag(name)
        }
        return item
    }

    override fun makeItem(): ItemStack = book.clone()
}

