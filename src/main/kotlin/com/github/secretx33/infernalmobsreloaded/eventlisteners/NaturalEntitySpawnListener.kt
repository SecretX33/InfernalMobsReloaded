package com.github.secretx33.infernalmobsreloaded.eventlisteners

import com.github.secretx33.infernalmobsreloaded.config.Config
import com.github.secretx33.infernalmobsreloaded.config.ConfigKeys
import com.github.secretx33.infernalmobsreloaded.repositories.InfernalMobTypesRepo
import com.github.secretx33.infernalmobsreloaded.utils.pdc
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.event.entity.CreatureSpawnEvent.*
import org.bukkit.plugin.Plugin
import org.koin.core.component.KoinApiExtension

@KoinApiExtension
class NaturalEntitySpawnListener(plugin: Plugin, private val config: Config, private val infernalMobTypesRepo: InfernalMobTypesRepo): Listener {

    init { Bukkit.getPluginManager().registerEvents(this, plugin) }

    @EventHandler(priority = EventPriority.NORMAL)
    private fun CreatureSpawnEvent.onNaturalEntitySpawn() {
        if(!validReasons.contains(spawnReason) || spawnedMobCannotBecomeInferno()) return
        entity.isPersistent
        entity.isCustomNameVisible = true
        entity.customName = "dsadsaas"
        entity.pdc.
    }

    private fun CreatureSpawnEvent.isNaturalSpawn(){}

    private fun CreatureSpawnEvent.spawnedMobCannotBecomeInferno() = !infernalMobTypesRepo.canTypeBecomeInfernal(entityType)

    private val validReasons: Set<SpawnReason>
        get() = config.getEnumSet(ConfigKeys.INFERNAL_SPAWN_REASONS)
}

