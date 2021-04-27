package com.github.secretx33.infernalmobsreloaded.eventlisteners

import com.github.secretx33.infernalmobsreloaded.events.InfernalMobSpawnEvent
import com.github.secretx33.infernalmobsreloaded.manager.InfernalMobsManager
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin

class InfernalMobSpawnListener(plugin: Plugin, private val mobsManager: InfernalMobsManager): Listener {

    init { Bukkit.getPluginManager().registerEvents(this, plugin) }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun InfernalMobSpawnEvent.onInfernoSpawn() {
        mobsManager.makeInfernalMob(this)
        // TODO("Apply the right effects to this mob and start all its scheduled tasks")
    }
}
