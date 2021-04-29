package com.github.secretx33.infernalmobsreloaded.events

import com.github.secretx33.infernalmobsreloaded.model.InfernalMobType
import org.bukkit.entity.LivingEntity
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 *
 * @property entity LivingEntity It's always an infernal mob
 * @property infernalType InfernalMobType What type the infernal entity is
 * @property world World the world the infernal is
 * @property entityType EntityType What type of vanilla entity the mob is
 * @constructor
 */
open class InfernalEntityEvent (
    val entity: LivingEntity,
    val infernalType: InfernalMobType,
) : Event() {

    val world get() = entity.world

    val entityType get() = entity.type

    init {
        require(entityType.entityClass != null) { "entity class cannot be null" }
        require(entityType.isSpawnable) { "entity needs to be spawnable" }
    }

    override fun getHandlers(): HandlerList = InfernalEntityEvent.handlers

    companion object {
        @JvmStatic
        private val handlers = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList {
            return handlers
        }
    }
}
