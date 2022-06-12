package com.github.secretx33.infernalmobsreloaded.eventlisteners.spawner

import com.github.secretx33.infernalmobsreloaded.config.Config
import com.github.secretx33.infernalmobsreloaded.config.ConfigKeys
import com.github.secretx33.infernalmobsreloaded.eventlisteners.hook.SpawnerBreakWithSilkSpawnersListener
import com.github.secretx33.infernalmobsreloaded.model.KeyChain
import com.github.secretx33.infernalmobsreloaded.repositories.InfernalMobTypesRepo
import com.github.secretx33.infernalmobsreloaded.utils.extension.pdc
import com.github.secretx33.infernalmobsreloaded.utils.extension.turnIntoSpawner
import org.bukkit.Material
import org.bukkit.block.CreatureSpawner
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import toothpick.InjectConstructor

/**
 * Might be replaced by [SpawnerBreakWithSilkSpawnersListener] if hook with `SilkSpawners` is enabled.
 */
@InjectConstructor
class SpawnerBreakListener(
    private val config: Config,
    private val infernalMobTypeRepo: InfernalMobTypesRepo,
    private val keyChain: KeyChain,
) : Listener {

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    private fun BlockBreakEvent.infernalSpawnerBreak() {
        if (block.type != Material.SPAWNER || !dropSpawners) return
        val spawner = block.state as CreatureSpawner

        val spawnerType = spawner.getSpawnerCategory() ?: return
        val infernalType = infernalMobTypeRepo.getInfernalTypeOrNull(spawnerType) ?: return

        isDropItems = false
        spawner.world.dropItemNaturally(spawner.location, ItemStack(Material.SPAWNER)) {
            it.itemStack.turnIntoSpawner(infernalType)
        }
    }

    private val dropSpawners
        get() = config.get<Boolean>(ConfigKeys.ENABLE_SPAWNER_DROPS)

    private fun CreatureSpawner.getSpawnerCategory() =
        pdc.get(keyChain.spawnerCategoryKey, PersistentDataType.STRING)
}
