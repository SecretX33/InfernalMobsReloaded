package com.github.secretx33.infernalmobsreloaded.eventlisteners.infernalmobs

import com.github.secretx33.infernalmobsreloaded.manager.InfernalMobsManager
import org.bukkit.Bukkit
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityTargetLivingEntityEvent
import org.bukkit.plugin.Plugin

class InfernalTargetListener(
    plugin: Plugin,
    private val mobsManager: InfernalMobsManager,
) : Listener {

    init { Bukkit.getPluginManager().registerEvents(this, plugin) }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun EntityTargetLivingEntityEvent.onInfernalTarget() {
        val entity = (entity as? LivingEntity)?.takeIf { it.isInfernalMob() } ?: return
        mobsManager.cancelAbilityTasks(entity)
        val target = target ?: return
        mobsManager.startTargetAbilityTasks(entity, target)
    }

    private fun LivingEntity.isInfernalMob(): Boolean = mobsManager.isValidInfernalMob(this)
}
