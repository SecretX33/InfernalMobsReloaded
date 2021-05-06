package com.github.secretx33.infernalmobsreloaded.eventlisteners.charm

import com.github.secretx33.infernalmobsreloaded.manager.CharmsManager
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.*
import org.bukkit.plugin.Plugin
import org.koin.core.component.KoinApiExtension
import java.util.logging.Logger

@KoinApiExtension
class PlayerItemMoveListener (
    plugin: Plugin,
    private val charmsManager: CharmsManager,
) : Listener {

    init { Bukkit.getPluginManager().registerEvents(this, plugin) }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun InventoryDragEvent.onItemDrag() {
        val player = whoClicked as? Player ?: return
        val topInvSize = view.topInventory.size.takeIf { view.topInventory != view.bottomInventory } ?: 0
        // player didn't drag any items inside his inventory
        if(rawSlots.none { it >= topInvSize }) return
        // update charm tasks
        charmsManager.updateEffects(player)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun InventoryClickEvent.onItemPickUp() {
        if(clickedInventory == null) return
        val player = whoClicked as? Player ?: return
        // update charm tasks
        charmsManager.updateEffects(player)
    }
}
