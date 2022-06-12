package com.github.secretx33.infernalmobsreloaded.eventlisteners.entity

import com.github.secretx33.infernalmobsreloaded.events.InfernalDeathEvent
import com.github.secretx33.infernalmobsreloaded.manager.InfernalMobsManager
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent
import toothpick.InjectConstructor

@InjectConstructor
class EntityDeathListener(private val mobsManager: InfernalMobsManager) : Listener {

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    private fun EntityDeathEvent.onEntityDeath() {
        if (!entity.isInfernalMob()) return
        val infernalType = mobsManager.getInfernalTypeOrNull(entity) ?: return
        val event = InfernalDeathEvent(entity, infernalType)
        if (!event.callEvent()) isCancelled = true
    }

    private fun LivingEntity.isInfernalMob() = mobsManager.isValidInfernalMob(this)
}
