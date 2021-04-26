package com.github.secretx33.infernalmobsreloaded.model

import org.bukkit.inventory.ItemStack
import java.util.*

data class LootItem (
    val name: String,
    private val dropItem: ItemStack,
) {
    fun getDrop(): ItemStack = dropItem.clone()

    init {
        require(name.isNotBlank()) { "LootItem name is invalid, it cannot be blank or empty" }
        require(dropItem.type.isItem) { "dropItem needs to be an item, and ${dropItem.type} is not" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LootItem
        if (name.equals(other.name, ignoreCase = true)) return false
        if (dropItem != other.dropItem) return false

        return true
    }

    override fun hashCode() = Objects.hash(name.toLowerCase(Locale.US), dropItem)
}
