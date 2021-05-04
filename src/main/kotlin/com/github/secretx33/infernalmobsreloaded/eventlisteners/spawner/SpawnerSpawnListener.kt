package com.github.secretx33.infernalmobsreloaded.eventlisteners.spawner

import com.github.secretx33.infernalmobsreloaded.events.InfernalSpawnEvent
import com.github.secretx33.infernalmobsreloaded.model.KeyChain
import com.github.secretx33.infernalmobsreloaded.repositories.InfernalMobTypesRepo
import com.github.secretx33.infernalmobsreloaded.utils.pdc
import org.bukkit.Bukkit
import org.bukkit.block.CreatureSpawner
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.SpawnerSpawnEvent
import org.bukkit.plugin.Plugin
import org.koin.core.component.KoinApiExtension

@KoinApiExtension
class SpawnerSpawnListener (
    plugin: Plugin,
    private val infernalMobTypeRepo: InfernalMobTypesRepo,
    private val keyChain: KeyChain,
): Listener {

    init { Bukkit.getPluginManager().registerEvents(this, plugin) }

    @EventHandler(priority = EventPriority.MONITOR)
    private fun SpawnerSpawnEvent.spawnerMobSpawn() {
        val entity = entity as? LivingEntity ?: return
        val spawnerType = spawner.getSpawnerCategory() ?: return
        val infernoType = infernalMobTypeRepo.getInfernalTypeOrNull(spawnerType) ?: return

        InfernalSpawnEvent(entity, infernoType).callEvent()
    }

    private fun CreatureSpawner.getSpawnerCategory() = pdc.get(keyChain.spawnerCategoryKey, org.bukkit.persistence.PersistentDataType.STRING)
}
