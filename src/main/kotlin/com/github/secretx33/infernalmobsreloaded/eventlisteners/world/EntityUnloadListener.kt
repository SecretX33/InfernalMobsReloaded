package com.github.secretx33.infernalmobsreloaded.eventlisteners.world

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent
import com.github.secretx33.infernalmobsreloaded.manager.BossBarManager
import com.github.secretx33.infernalmobsreloaded.manager.InfernalMobsManager
import org.bukkit.Bukkit
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin
import toothpick.InjectConstructor

@InjectConstructor
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

