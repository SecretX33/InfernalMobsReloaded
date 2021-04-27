package com.github.secretx33.infernalmobsreloaded.eventlisteners

import com.github.secretx33.infernalmobsreloaded.config.Config
import com.github.secretx33.infernalmobsreloaded.config.ConfigKeys
import com.github.secretx33.infernalmobsreloaded.events.InfernalMobSpawnEvent
import com.github.secretx33.infernalmobsreloaded.repositories.InfernalMobTypesRepo
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.entity.EntityType
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason
import org.bukkit.plugin.Plugin
import org.koin.core.component.KoinApiExtension
import java.util.*

@KoinApiExtension
class NaturalEntitySpawnListener(plugin: Plugin, private val config: Config, private val infernalMobTypesRepo: InfernalMobTypesRepo): Listener {

    init { Bukkit.getPluginManager().registerEvents(this, plugin) }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun CreatureSpawnEvent.onNaturalEntitySpawn() {
        val world = entity.world
        if(!spawnReason.isAllowed() || entityType.cannotBeInferno() || !world.isWhitelisted()) return
        val infernoType = infernalMobTypesRepo.getInfernoTypes(entityType).firstOrNull { random.nextDouble() <= it.spawnChance } ?: return

        // fire event InfernalMobSpawnEvent to spawn a new inferno
        Bukkit.getPluginManager().callEvent(InfernalMobSpawnEvent(entity, infernoType, spawnReason, world))
    }

    private fun EntityType.cannotBeInferno() = !infernalMobTypesRepo.canTypeBecomeInfernal(this)

    private fun SpawnReason.isAllowed() = validReasons.contains(this)

    private fun World.isWhitelisted() = validWorlds.contains("<ALL>") || validWorlds.any { it.equals(name, ignoreCase = true) }

    private val validReasons: Set<SpawnReason>
        get() = config.getEnumSet(ConfigKeys.INFERNAL_ALLOWED_SPAWN_REASONS)

    private val validWorlds: List<String>
        get() = config.get(ConfigKeys.INFERNAL_ALLOWED_WORLDS)

    private companion object {
        val random = Random()
    }
}

