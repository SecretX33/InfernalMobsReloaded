package com.github.secretx33.infernalmobsreloaded.eventlisteners.integration

import com.github.secretx33.infernalmobsreloaded.config.Config
import com.github.secretx33.infernalmobsreloaded.config.ConfigKeys
import com.github.secretx33.infernalmobsreloaded.manager.BossBarManager
import com.github.secretx33.infernalmobsreloaded.manager.InfernalMobsManager
import com.github.secretx33.infernalmobsreloaded.utils.runSync
import com.google.common.cache.CacheBuilder
import com.palmergames.bukkit.towny.TownyAPI
import io.papermc.paper.event.entity.EntityMoveEvent
import kotlinx.coroutines.*
import org.bukkit.Bukkit
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin
import java.util.*
import java.util.concurrent.TimeUnit

class TownyListener (
    private val plugin: Plugin,
    config: Config,
    private val mobsManager: InfernalMobsManager,
    private val bossBarManager: BossBarManager,
): Listener {

    private val removalDelay = (config.get<Double>(ConfigKeys.TOWNY_REMOVE_INFERNAL_IN_TOWNS_DELAY) * 1000.0).toLong().coerceAtLeast(0)

    private val removalCache = CacheBuilder.newBuilder().expireAfterWrite(removalDelay, TimeUnit.MILLISECONDS).build<UUID, Boolean>()

    init { Bukkit.getPluginManager().registerEvents(this, plugin) }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    private fun EntityMoveEvent.onInfernalMove() {
        if(!entity.isInfernalMob() || towny.isWilderness(entity.location)) return

        // infernal removal is already pending
        if(removalCache.getIfPresent(entity.uniqueId) != null) return
        // infernal step inside a town
        scheduleMobRemoval()
    }

    private fun EntityMoveEvent.scheduleMobRemoval() {
        removalCache.put(entity.uniqueId, true)
        val uuid = entity.uniqueId
        CoroutineScope(Dispatchers.Default).launch {
            delay(removalDelay)
            val entity = Bukkit.getEntity(uuid)?.takeIf { it.isValid && !it.isDead } ?: return@launch
            runSync(plugin) { entity.blackhole() }
        }
    }

    private fun Entity.blackhole() {
        passengers.forEach {
            removePassenger(it)
            it.blackhole()
        }
        vehicle?.let {
            removePassenger(this)
            it.blackhole()
        }
        if(!isDead && isValid) remove()

        (this as? LivingEntity)?.let {
            if(!isInfernalMob()) return
            removalCache.invalidate(uniqueId)
            mobsManager.unloadInfernalMob(this)
            bossBarManager.removeBossBar(this)
        }
    }

    private fun LivingEntity.isInfernalMob() = mobsManager.isValidInfernalMob(this)

    private val towny get() = TownyAPI.getInstance()
}
