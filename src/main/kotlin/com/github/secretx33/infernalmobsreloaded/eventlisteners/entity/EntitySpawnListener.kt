package com.github.secretx33.infernalmobsreloaded.eventlisteners.entity

import com.github.secretx33.infernalmobsreloaded.config.Config
import com.github.secretx33.infernalmobsreloaded.config.ConfigKeys
import com.github.secretx33.infernalmobsreloaded.events.InfernalSpawnEvent
import com.github.secretx33.infernalmobsreloaded.manager.InfernalMobsManager
import com.github.secretx33.infernalmobsreloaded.repositories.InfernalMobTypesRepo
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.entity.Ageable
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason
import org.bukkit.plugin.Plugin
import org.koin.core.component.KoinApiExtension
import java.util.*

@KoinApiExtension
class EntitySpawnListener (
    plugin: Plugin,
    private val config: Config,
    private val infernalManager: InfernalMobsManager,
    private val infernalTypesRepo: InfernalMobTypesRepo,
): Listener {

    init { Bukkit.getPluginManager().registerEvents(this, plugin) }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun CreatureSpawnEvent.onNaturalEntitySpawn() {
        val world = entity.world
        if(!spawnReason.isAllowed() || entity.cannotBeInfernal() || entity.alreadyIsInfernal() || !world.isWhitelisted()) return
        val infernoType = infernalTypesRepo.getInfernalTypes(entityType).firstOrNull { random.nextDouble() <= it.spawnChance } ?: return

        // fire event InfernalMobSpawnEvent to spawn a new infernal
        Bukkit.getPluginManager().callEvent(InfernalSpawnEvent(entity, infernoType, spawnReason))
    }

    private fun LivingEntity.cannotBeInfernal() = !infernalTypesRepo.canTypeBecomeInfernal(type) || (this is Ageable && !isAdult && blacklistedBabies.contains(type)) || infernalManager.isMountOfAnotherInfernal(this)

    private fun LivingEntity.alreadyIsInfernal() = infernalManager.isPossibleInfernalMob(this)

    private fun SpawnReason.isAllowed() = validReasons.contains(this)

    private fun World.isWhitelisted() = validWorlds.let { worlds -> worlds.contains("<ALL>") || worlds.any { it.equals(name, ignoreCase = true) } }

    private val validWorlds get() = config.get<List<String>>(ConfigKeys.INFERNAL_ALLOWED_WORLDS)

    private val validReasons get() = config.getEnumSet(ConfigKeys.INFERNAL_ALLOWED_SPAWN_REASONS, SpawnReason::class.java)

    private val blacklistedBabies get() = config.getEnumSet(ConfigKeys.INFERNAL_BLACKLISTED_BABY_MOBS, EntityType::class.java)

    private companion object {
        val random = Random()
    }
}

