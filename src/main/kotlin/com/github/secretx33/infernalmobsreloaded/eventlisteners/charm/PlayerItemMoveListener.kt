package com.github.secretx33.infernalmobsreloaded.eventlisteners.charm

import com.github.secretx33.infernalmobsreloaded.manager.CharmsManager
import com.github.secretx33.infernalmobsreloaded.utils.runSync
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.inventory.*
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.plugin.Plugin
import org.koin.core.component.KoinApiExtension

@KoinApiExtension
class PlayerItemMoveListener (
    private val plugin: Plugin,
    private val charmsManager: CharmsManager,
) : Listener {

    init { Bukkit.getPluginManager().registerEvents(this, plugin) }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun InventoryDragEvent.onItemDrag() {
        val player = whoClicked as? Player ?: return
        println("Called InventoryDragEvent")
        val topInvSize = view.topInventory.size.takeIf { view.topInventory != view.bottomInventory } ?: 0
        // player didn't drag any items inside his inventory
        if(rawSlots.all { it < topInvSize }) return
        // update charm tasks
        runSync(plugin, 50L) { player.updateCharmEffects() }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun InventoryClickEvent.onItemMove() {
        println("clickedInventory.type = ${clickedInventory?.type}, action = $action, inventory.type = ${inventory.type}")
        if(didNothing()) return
        val player = whoClicked as? Player ?: return
        // update charm tasks
        runSync(plugin, 50L) { player.updateCharmEffects() }
    }

    private fun InventoryClickEvent.didNothing() = clickedInventory == null || clickedInventory?.type != InventoryType.PLAYER || action == InventoryAction.NOTHING

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun PlayerDropItemEvent.onItemDrop() {
        println("1. PlayerDropItemEvent triggered")
        runSync(plugin, 50L) { player.updateCharmEffects() }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun EntityPickupItemEvent.onItemPickup() {
        val player = entity as? Player ?: return
        runSync(plugin, 50L) { player.updateCharmEffects() }
    }

    private fun Player.updateCharmEffects() = charmsManager.updateCharmEffects(this)
}
