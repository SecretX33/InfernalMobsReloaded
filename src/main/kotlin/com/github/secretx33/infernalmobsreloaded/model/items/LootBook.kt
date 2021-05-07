package com.github.secretx33.infernalmobsreloaded.model.items

import com.github.secretx33.infernalmobsreloaded.utils.formattedTypeName
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BookMeta

data class LootBook (
    override val name: String,
    private val material: Material,
    private val title: Component?,
    private val author: Component?,
    private val generation: BookMeta.Generation,
    private val pages: List<Component>,
) : LootItem {

    init {
        require(material == Material.WRITTEN_BOOK || material == Material.WRITABLE_BOOK) { "book material has to be either written or writtable book, but ${material.formattedTypeName()} is not" }
    }

    private val book: ItemStack = generateBook()

    private fun generateBook(): ItemStack {
        val item = ItemStack(material)
        (item.itemMeta as? BookMeta)?.let { meta ->
            title?.let { meta.title(title) }
            author?.let { meta.author(author) }
            meta.generation = generation
            meta.pages(pages)
            book.itemMeta = meta
        }
        return item
    }

    override fun makeItem(): ItemStack = book.clone()
}

