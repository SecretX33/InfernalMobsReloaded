package com.github.secretx33.infernalmobsreloaded.eventlisteners

import com.github.secretx33.infernalmobsreloaded.manager.InfernalMobsManager
import org.bukkit.Bukkit
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.plugin.Plugin

class InfernalMobDeathListener(plugin: Plugin, private val mobsManager: InfernalMobsManager): Listener {

    init { Bukkit.getPluginManager().registerEvents(this, plugin) }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun EntityDeathEvent.onInfernalMobDeath() {
        if(!entity.isInfernalMob()) return
        // TODO("Manage second live & add custom loot")
        mobsManager.unloadInfernalMob(entity)
    }

    private fun LivingEntity.isInfernalMob() = mobsManager.isInfernalMob(this)
}
