package com.github.secretx33.infernalmobsreloaded.eventlisteners.world

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent
import com.github.secretx33.infernalmobsreloaded.config.Config
import com.github.secretx33.infernalmobsreloaded.config.ConfigKeys
import com.github.secretx33.infernalmobsreloaded.manager.BossBarManager
import com.github.secretx33.infernalmobsreloaded.manager.InfernalMobsManager
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import toothpick.InjectConstructor

@InjectConstructor
class EntityLoadListener(
    private val config: Config,
    private val mobsManager: InfernalMobsManager,
    private val bossBarManager: BossBarManager,
) : Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    private fun EntityAddToWorldEvent.onInfernalMobsRespawn() {
        val entity = entity as? LivingEntity ?: return
        // when infernal mob gets respawned on the world
        if(!entity.isPossibleInfernalMob()) return
        // if mob is persistent, persistence mode is off and purge mode is on, remove the mob and return
//        println("Loading ${entity.name}, isPurgeEnabled = $isPurgeEnabled, isPersistanceEnabled = $isPersistanceEnabled, entity.removeWhenFarAway = ${entity.removeWhenFarAway}")
        if(isPurgeEnabled && !isPersistanceEnabled && !entity.removeWhenFarAway) {
//            println("Purging ${entity.name}")
            entity.blackhole()
            return
        }
        mobsManager.loadInfernalMob(entity)
    }

    private fun Entity.blackhole() {
        vehicle?.let {
            it.blackhole()
            return
        }
        getSelfAndPassengersRecursively().asSequence()
            .filter { !isDead && isValid }
            .forEach {
                it.remove()
                if(it !is LivingEntity || !it.isInfernalMobOrMount()) return@forEach
                mobsManager.unloadInfernalMob(it)
                bossBarManager.removeBossBar(it)
            }
    }

    private fun Entity.getSelfAndPassengersRecursively(): Set<Entity> = passengers.flatMapTo(hashSetOf()) { it.getSelfAndPassengersRecursively() } + this

    private val isPersistanceEnabled
        get() = config.get<Boolean>(ConfigKeys.INFERNALS_ARE_PERSISTENT)

    private val isPurgeEnabled
        get() = config.get<Boolean>(ConfigKeys.INFERNALS_PERSISTENCE_PURGE_MODE)

    private fun LivingEntity.isInfernalMobOrMount() = mobsManager.isValidInfernalMob(this) || mobsManager.isMountOfAnotherInfernal(this)

    private fun Entity.isPossibleInfernalMob() = this is LivingEntity && isValid && !isDead && mobsManager.isPossibleInfernalMob(this)
}
