package com.github.secretx33.infernalmobsreloaded.eventlisteners.spawner

import com.github.secretx33.infernalmobsreloaded.events.InfernalSpawnEvent
import com.github.secretx33.infernalmobsreloaded.model.KeyChain
import com.github.secretx33.infernalmobsreloaded.repositories.InfernalMobTypesRepo
import com.github.secretx33.infernalmobsreloaded.utils.extension.pdc
import org.bukkit.block.CreatureSpawner
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.SpawnerSpawnEvent
import org.bukkit.persistence.PersistentDataType
import toothpick.InjectConstructor

@InjectConstructor
class SpawnerSpawnListener(
    private val infernalMobTypeRepo: InfernalMobTypesRepo,
    private val keyChain: KeyChain,
) : Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    private fun SpawnerSpawnEvent.spawnerMobSpawn() {
        val entity = entity as? LivingEntity ?: return
        val spawnerType = spawner.getSpawnerCategory() ?: return
        val infernoType = infernalMobTypeRepo.getInfernalTypeOrNull(spawnerType) ?: return

        InfernalSpawnEvent(entity, infernoType).callEvent()
    }

    private fun CreatureSpawner.getSpawnerCategory() = pdc.get(keyChain.spawnerCategoryKey, PersistentDataType.STRING)
}
