package com.github.secretx33.infernalmobsreloaded.eventlistener.entity

import com.github.secretx33.infernalmobsreloaded.config.Config
import com.github.secretx33.infernalmobsreloaded.config.ConfigKeys
import com.github.secretx33.infernalmobsreloaded.event.InfernalSpawnEvent
import com.github.secretx33.infernalmobsreloaded.manager.InfernalMobsManager
import com.github.secretx33.infernalmobsreloaded.repository.InfernalMobTypesRepo
import com.github.secretx33.infernalmobsreloaded.util.extension.random
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
import toothpick.InjectConstructor

@InjectConstructor
class EntitySpawnListener (
    private val config: Config,
    private val infernalManager: InfernalMobsManager,
    private val infernalTypesRepo: InfernalMobTypesRepo,
) : Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun CreatureSpawnEvent.onNaturalEntitySpawn() {
        val world = entity.world
        if (!spawnReason.isAllowed() || entity.cannotBeInfernal() || entity.alreadyIsInfernal() || !world.isWhitelisted()) return
        val infernalType = infernalTypesRepo.getInfernalTypes(entityType).firstOrNull { random.nextDouble() <= it.spawnChance } ?: return

        // fire event InfernalMobSpawnEvent to spawn a new infernal
        Bukkit.getPluginManager().callEvent(InfernalSpawnEvent(entity, infernalType, spawnReason))
    }

    private fun LivingEntity.cannotBeInfernal(): Boolean = !infernalTypesRepo.canTypeBecomeInfernal(type)
            || (this is Ageable && !isAdult && blacklistedBabies.contains(type))
            || infernalManager.isMountOfAnotherInfernal(this)

    private fun LivingEntity.alreadyIsInfernal(): Boolean = infernalManager.isPossibleInfernalMob(this)

    private fun SpawnReason.isAllowed(): Boolean = this in validReasons

    private fun World.isWhitelisted(): Boolean = validWorlds.let { worlds -> "<ALL>" in worlds || worlds.any { it.equals(name, ignoreCase = true) } }

    private val validWorlds: List<String> get() = config.get(ConfigKeys.INFERNAL_ALLOWED_WORLDS)

    private val validReasons get() = config.getEnumSet<SpawnReason>(ConfigKeys.INFERNAL_ALLOWED_SPAWN_REASONS)

    private val blacklistedBabies get() = config.getEnumSet<EntityType>(ConfigKeys.INFERNAL_BLACKLISTED_BABY_MOBS)
}

