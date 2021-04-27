package com.github.secretx33.infernalmobsreloaded.eventlisteners

import com.github.secretx33.infernalmobsreloaded.events.InfernalMobSpawnEvent
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin

class InfernalMobSpawnListener(plugin: Plugin): Listener {

    init { Bukkit.getPluginManager().registerEvents(this, plugin) }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun InfernalMobSpawnEvent.onInfernoSpawn() {
        entity.apply {
            isPersistent = true
            customName(infernoType.displayName)
            isCustomNameVisible = true
        }
        // TODO("Apply the right effects to this mob and start all its scheduled tasks")
    }
}
