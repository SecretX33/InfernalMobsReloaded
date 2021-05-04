package com.github.secretx33.infernalmobsreloaded.model.items

import net.kyori.adventure.text.Component
import org.bukkit.inventory.ItemStack
import java.util.*

data class LootBook (
    override val name: String,
    val displayName: Component,
    val title: Component?,
    val author: Component?,
    val pages: List<Component>,
) : LootItem {

    override fun makeItem(): ItemStack {
        TODO("bugs")
//        val book = ItemStack(Material.WRITABLE_BOOK)
//        val meta = (book.itemMeta as BookMeta).toBuilder()
//        val a = meta.author(author)
//            .title(title)
//            .pages
//
//        BookMeta.BookMetaBuilder.
//        builder.pages(pages)
//        author?.let { builder.author(it) }
//        return book.apply { itemMeta = builder.build() }
    }

    private companion object {
        val random = Random()
    }
}

