package com.github.secretx33.infernalmobsreloaded.eventlisteners

import com.github.secretx33.infernalmobsreloaded.utils.pdc
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.plugin.Plugin

class EntitySpawnListener(plugin: Plugin): Listener {

    init { Bukkit.getPluginManager().registerEvents(this, plugin) }

    @EventHandler(priority = EventPriority.NORMAL)
    private fun CreatureSpawnEvent.onBlockPlace() {
        val player = Bukkit.getPlayer("")!!
        entity.isCustomNameVisible = true
        entity.customName = "dsadsaas"
        entity.pdc.
    }
}

