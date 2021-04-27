package com.github.secretx33.infernalmobsreloaded.events

import com.github.secretx33.infernalmobsreloaded.model.InfernalMobType
import org.bukkit.World
import org.bukkit.entity.LivingEntity
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason

class InfernalSpawnEvent(
    val entity: LivingEntity,
    val infernalType: InfernalMobType,
    val spawnReason: SpawnReason,
    val world: World,
) : Event(), Cancellable {

    val entityType = entity.type

    init {
        require(entityType.entityClass != null) { "entity class cannot be null" }
        require(entityType.isSpawnable) { "entity needs to be spawnable" }
    }

    override fun getHandlers(): HandlerList = InfernalSpawnEvent.handlers

    override fun isCancelled(): Boolean = isCancelled

    override fun setCancelled(isCancelled: Boolean) {
        this.isCancelled = isCancelled
    }

    companion object {
        @JvmStatic
        private val handlers = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList {
            return handlers
        }
    }
}
