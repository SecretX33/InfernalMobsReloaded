package com.github.secretx33.infernalmobsreloaded.eventlisteners.world

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent
import com.github.secretx33.infernalmobsreloaded.manager.BossBarManager
import com.github.secretx33.infernalmobsreloaded.manager.InfernalMobsManager
import com.github.secretx33.infernalmobsreloaded.utils.formattedTypeName
import com.github.secretx33.infernalmobsreloaded.utils.runSync
import org.bukkit.Bukkit
import org.bukkit.entity.*
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.world.ChunkUnloadEvent
import org.bukkit.plugin.Plugin
import org.koin.core.component.KoinApiExtension

@KoinApiExtension
class EntityUnloadListener (
    plugin: Plugin,
    private val mobsManager: InfernalMobsManager,
    private val barManager: BossBarManager,
): Listener {

    init { Bukkit.getPluginManager().registerEvents(this, plugin) }

    @EventHandler(priority = EventPriority.MONITOR)
    private fun EntityRemoveFromWorldEvent.onInfernalMobsDespawn() {
        // when infernal mob gets removed from the world
        if(!entity.isInfernalMob()) return
        mobsManager.unloadInfernalMob(entity as LivingEntity)
        barManager.removeBossBar(entity as LivingEntity)
    }

    private fun Entity.isInfernalMob() = this is LivingEntity && mobsManager.isValidInfernalMob(this)
}

