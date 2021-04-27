package com.github.secretx33.infernalmobsreloaded.eventlisteners

import com.github.secretx33.infernalmobsreloaded.events.InfernalMobSpawnEvent
import com.github.secretx33.infernalmobsreloaded.manager.InfernalMobsManager
import org.bukkit.Bukkit
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.world.ChunkUnloadEvent
import org.bukkit.plugin.Plugin

class ChunkUnloadListener(plugin: Plugin, private val mobsManager: InfernalMobsManager): Listener {

    init { Bukkit.getPluginManager().registerEvents(this, plugin) }

    @EventHandler(priority = EventPriority.MONITOR)
    private fun ChunkUnloadEvent.onInfernalMobsDespawn() {
        chunk.entities.asSequence()
            .filter { it.isInfernalMob() }
            .forEach { mobsManager.unloadInfernalMob(it as LivingEntity) }
    }

    private fun Entity.isInfernalMob() = this is LivingEntity && mobsManager.isInfernalMob(this)
}

