package com.github.secretx33.infernalmobsreloaded.model.items

import org.bukkit.inventory.ItemStack

/**
 * Represents anything that can be given to a player as an ItemStack
 *
 * @property name String
 */
interface LootItem {

    /**
     * The name of the loot item, basically its "configuration" name, the same name as used in the config file
     */
    val name: String

    /**
     * Create on the fly a new item stack with the configured attribute ranges.
     *
     * @return ItemStack
     */
    fun makeItem(): ItemStack
}
