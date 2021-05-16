package com.github.secretx33.infernalmobsreloaded.eventlisteners.world

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent
import com.github.secretx33.infernalmobsreloaded.manager.InfernalMobsManager
import org.bukkit.Bukkit
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin

class EntityLoadListener(plugin: Plugin, private val mobsManager: InfernalMobsManager): Listener {

    init { Bukkit.getPluginManager().registerEvents(this, plugin) }

    @EventHandler(priority = EventPriority.MONITOR)
    private fun EntityAddToWorldEvent.onInfernalMobsRespawn() {
        // when infernal mob gets respawned on the world
        if(!entity.isPossibleInfernalMob()) return
        mobsManager.loadInfernalMob(entity as LivingEntity)
    }

    private fun Entity.isPossibleInfernalMob() = this is LivingEntity && isValid && !isDead && mobsManager.isPossibleInfernalMob(this)
}
