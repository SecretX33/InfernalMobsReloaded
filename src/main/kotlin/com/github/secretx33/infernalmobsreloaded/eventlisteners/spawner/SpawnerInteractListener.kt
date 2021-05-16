package com.github.secretx33.infernalmobsreloaded.eventlisteners.spawner

import com.github.secretx33.infernalmobsreloaded.config.Config
import com.github.secretx33.infernalmobsreloaded.model.KeyChain
import com.github.secretx33.infernalmobsreloaded.repositories.InfernalMobTypesRepo
import com.github.secretx33.infernalmobsreloaded.utils.pdc
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.CreatureSpawner
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin

class SpawnerInteractListener (
    plugin: Plugin,
    private val config: Config,
    private val infernalMobTypeRepo: InfernalMobTypesRepo,
    private val keyChain: KeyChain,
): Listener {

    init { Bukkit.getPluginManager().registerEvents(this, plugin) }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    private fun PlayerInteractEvent.onInfernalSpawnerInteract() {
        if(!isRightClickSpawner()) return

        val block = clickedBlock ?: return
        val spawnerState = block.state as CreatureSpawner
        // get spawner category, or return if none
        val spawnerType = spawnerState.getSpawnerCategory() ?: return

        // prevent any change of the type of this spawner using egg
        isCancelled = true
        // if spawner is from a removed type
        if(!infernalMobTypeRepo.isValidInfernalType(spawnerType)) {
            spawnerState.pdc.remove(keyChain.spawnerCategoryKey)
            spawnerState.update(true, true)
            return
        }
    }

    private fun PlayerInteractEvent.isRightClickSpawner() = clickedBlock?.type == Material.SPAWNER && action == Action.RIGHT_CLICK_BLOCK

    private fun CreatureSpawner.getSpawnerCategory() = pdc.get(keyChain.spawnerCategoryKey, PersistentDataType.STRING)
}
