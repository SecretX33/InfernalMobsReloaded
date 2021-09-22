package com.github.secretx33.infernalmobsreloaded.eventlisteners.integration

import com.github.secretx33.infernalmobsreloaded.config.Config
import com.github.secretx33.infernalmobsreloaded.config.ConfigKeys
import com.github.secretx33.infernalmobsreloaded.manager.BossBarManager
import com.github.secretx33.infernalmobsreloaded.manager.InfernalMobsManager
import com.github.secretx33.infernalmobsreloaded.utils.extension.runSync
import com.google.common.cache.CacheBuilder
import com.palmergames.bukkit.towny.TownyAPI
import io.papermc.paper.event.entity.EntityMoveEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.bukkit.Bukkit
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin
import java.lang.ref.WeakReference
import java.util.UUID
import java.util.concurrent.TimeUnit

class TownyListener (
    private val plugin: Plugin,
    private val config: Config,
    private val mobsManager: InfernalMobsManager,
    private val bossBarManager: BossBarManager,
): Listener {

    private val removalDelay = (config.get<Double>(ConfigKeys.TOWNY_REMOVE_INFERNAL_IN_TOWNS_DELAY) * 1000.0).toLong().coerceAtLeast(0)

    private val removalCache = CacheBuilder.newBuilder().expireAfterWrite(removalDelay, TimeUnit.MILLISECONDS).build<UUID, Boolean>()

    private val removalTaskCache = CacheBuilder.newBuilder().expireAfterWrite(removalDelay, TimeUnit.MILLISECONDS).build<Job, Boolean>()

    init { Bukkit.getPluginManager().registerEvents(this, plugin) }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    private fun EntityMoveEvent.onInfernalMove() {
        if(!entity.isInfernalMobOrMount() || !entity.isInsideProtectedTown()) return

        // infernal removal is already pending
        if(removalCache.getIfPresent(entity.uniqueId) != null) return
        // infernal step inside a town
        scheduleMobRemoval()
    }

    private fun EntityMoveEvent.scheduleMobRemoval() {
        removalCache.put(entity.uniqueId, true)
        val wEntity = WeakReference(entity)
        CoroutineScope(Dispatchers.Default).launch {
            delay(removalDelay)
            val entity = wEntity.get()?.takeIf { it.isValid && !it.isDead && it.isInsideProtectedTown() } ?: return@launch
            runSync(plugin) { entity.blackhole() }
        }.let { removalTaskCache.put(it, true) }
    }

    private fun Entity.isInsideProtectedTown(): Boolean = when(requireHasMobsDisabled) {
        true -> towny.getTown(location)?.hasMobs()?.not() ?: false
        false -> !towny.isWilderness(location)
    }

    private fun Entity.blackhole() {
        vehicle?.let {
            it.blackhole()
            return
        }
        getSelfAndPassengersRecursively().asSequence()
            .filter { !isDead && isValid }
            .forEach {
                mobsManager.removeAndDropStolenItems(it)
                removalCache.invalidate(uniqueId)
                if(it !is LivingEntity || !it.isInfernalMobOrMount()) return@forEach
                mobsManager.unloadInfernalMob(it)
                bossBarManager.removeBossBar(it)
            }
    }

    private fun Entity.getSelfAndPassengersRecursively(): Set<Entity> = passengers.flatMapTo(HashSet()) { it.getSelfAndPassengersRecursively() } + this

    private fun LivingEntity.isInfernalMobOrMount() = mobsManager.isValidInfernalMob(this) || mobsManager.isMountOfAnotherInfernal(this)

    fun cancelRemovalTasks() {
        removalTaskCache.asMap().forEach { (job, _) -> job.cancel() }
        removalTaskCache.invalidateAll()
    }

    private val towny get() = TownyAPI.getInstance()

    private val requireHasMobsDisabled get() = config.get<Boolean>(ConfigKeys.TOWNY_REMOVE_INFERNALS_ONLY_IF_HAS_MOBS_IS_DISABLED)
}
