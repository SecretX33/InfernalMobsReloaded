package com.github.secretx33.infernalmobsreloaded.eventlisteners

import com.github.secretx33.infernalmobsreloaded.manager.InfernalMobsManager
import org.bukkit.Bukkit
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.plugin.Plugin
import org.koin.core.component.KoinApiExtension

@KoinApiExtension
class ChunkLoadListener(plugin: Plugin, private val mobsManager: InfernalMobsManager): Listener {

    init { Bukkit.getPluginManager().registerEvents(this, plugin) }

    @EventHandler(priority = EventPriority.MONITOR)
    private fun ChunkLoadEvent.onInfernalMobsRespawn() {
        chunk.entities.asSequence()
            .filter { it.isPossibleInfernalMob() }
            .forEach { mobsManager.loadInfernalMob(it as LivingEntity) }
    }

    // isValid may not return true since the entity is being loaded again (?)
    private fun Entity.isPossibleInfernalMob() = this is LivingEntity && isValid && !isDead && mobsManager.isPossibleInfernalMob(this)
}
