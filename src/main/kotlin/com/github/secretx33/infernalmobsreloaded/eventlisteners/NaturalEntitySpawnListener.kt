package com.github.secretx33.infernalmobsreloaded.eventlisteners

import com.github.secretx33.infernalmobsreloaded.config.Config
import com.github.secretx33.infernalmobsreloaded.config.ConfigKeys
import com.github.secretx33.infernalmobsreloaded.events.InfernalMobSpawnEvent
import com.github.secretx33.infernalmobsreloaded.repositories.InfernalMobTypesRepo
import org.bukkit.Bukkit
import org.bukkit.entity.EntityType
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason
import org.bukkit.plugin.Plugin
import org.koin.core.component.KoinApiExtension

@KoinApiExtension
class NaturalEntitySpawnListener(plugin: Plugin, private val config: Config, private val infernalMobTypesRepo: InfernalMobTypesRepo): Listener {

    init { Bukkit.getPluginManager().registerEvents(this, plugin) }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun CreatureSpawnEvent.onNaturalEntitySpawn() {
        if(!spawnReason.isAllowed() || entityType.cannotBeInferno()) return
        val infernoType = infernalMobTypesRepo.getInfernoTypeOrNull(entityType) ?: return
        // TODO("Calculate the chance of this mob become a infernal mob")
        Bukkit.getPluginManager().callEvent(InfernalMobSpawnEvent(entity, infernoType, spawnReason))
    }

    private fun SpawnReason.isAllowed() = validReasons.contains(this)

    private fun EntityType.cannotBeInferno() = !infernalMobTypesRepo.canTypeBecomeInfernal(this)

    private val validReasons: Set<SpawnReason>
        get() = config.getEnumSet(ConfigKeys.INFERNAL_SPAWN_REASONS)
}

