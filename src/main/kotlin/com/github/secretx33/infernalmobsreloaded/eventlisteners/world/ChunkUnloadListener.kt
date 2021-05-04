package com.github.secretx33.infernalmobsreloaded.eventlisteners.world

import com.github.secretx33.infernalmobsreloaded.manager.BossBarManager
import com.github.secretx33.infernalmobsreloaded.manager.InfernalMobsManager
import org.bukkit.Bukkit
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.world.ChunkUnloadEvent
import org.bukkit.plugin.Plugin
import org.koin.core.component.KoinApiExtension

@KoinApiExtension
class ChunkUnloadListener (
    plugin: Plugin,
    private val mobsManager: InfernalMobsManager,
    private val barManager: BossBarManager,
): Listener {

    init { Bukkit.getPluginManager().registerEvents(this, plugin) }

    @EventHandler(priority = EventPriority.MONITOR)
    private fun ChunkUnloadEvent.onInfernalMobsDespawn() {
        chunk.entities.filter { it.isInfernalMob() }
            .forEach {
                mobsManager.unloadInfernalMob(it as LivingEntity)
                barManager.removeBossBar(it)
            }
    }

    private fun Entity.isInfernalMob() = this is LivingEntity && mobsManager.isValidInfernalMob(this)
}

