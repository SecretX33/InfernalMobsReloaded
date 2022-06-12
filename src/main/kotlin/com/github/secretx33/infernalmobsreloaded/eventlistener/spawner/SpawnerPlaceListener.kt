package com.github.secretx33.infernalmobsreloaded.eventlistener.spawner

import com.github.secretx33.infernalmobsreloaded.model.KeyChain
import com.github.secretx33.infernalmobsreloaded.repository.InfernalMobTypesRepo
import com.github.secretx33.infernalmobsreloaded.util.extension.pdc
import org.bukkit.Material
import org.bukkit.block.CreatureSpawner
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import toothpick.InjectConstructor

@InjectConstructor
class SpawnerPlaceListener(
    private val infernalMobTypeRepo: InfernalMobTypesRepo,
    private val keyChain: KeyChain,
) : Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun BlockPlaceEvent.infernalSpawnerPlace() {
        if (blockPlaced.type != Material.SPAWNER) return

        val category = itemInHand.getSpawnerCategory() ?: return
        val infernalType = infernalMobTypeRepo.getInfernalTypeOrNull(category) ?: return

        (block.state as CreatureSpawner).apply {
            spawnedType = infernalType.entityType
            pdc.set(keyChain.spawnerCategoryKey, PersistentDataType.STRING, infernalType.name)
            update(true, true)
        }
    }

    private fun ItemStack.getSpawnerCategory() = itemMeta.pdc.get(keyChain.spawnerCategoryKey, PersistentDataType.STRING)
}

