package com.github.secretx33.infernalmobsreloaded.eventlistener.hook

import com.github.secretx33.infernalmobsreloaded.annotation.SkipAutoRegistration
import com.github.secretx33.infernalmobsreloaded.config.Config
import com.github.secretx33.infernalmobsreloaded.config.ConfigKeys
import com.github.secretx33.infernalmobsreloaded.eventlistener.spawner.SpawnerBreakListener
import com.github.secretx33.infernalmobsreloaded.model.KeyChain
import com.github.secretx33.infernalmobsreloaded.repository.InfernalMobTypesRepo
import com.github.secretx33.infernalmobsreloaded.util.extension.pdc
import com.github.secretx33.infernalmobsreloaded.util.extension.turnIntoSpawner
import de.dustplanet.silkspawners.events.SilkSpawnersSpawnerBreakEvent
import de.dustplanet.util.SilkUtil
import org.bukkit.block.CreatureSpawner
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.persistence.PersistentDataType
import toothpick.InjectConstructor

/**
 * Replaces [SpawnerBreakListener] in the presence of `SilkSpawners` (and its hook is enabled).
 */
@SkipAutoRegistration
@InjectConstructor
class SpawnerBreakWithSilkSpawnersListener(
    private val config: Config,
    private val infernalMobTypeRepo: InfernalMobTypesRepo,
    private val keyChain: KeyChain,
) : Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private fun SilkSpawnersSpawnerBreakEvent.infernalSpawnerBreak() {
        if (!dropSpawners) return

        val spawnerType = spawner?.getSpawnerCategory() ?: return
        val infernalType = infernalMobTypeRepo.getInfernalTypeOrNull(spawnerType) ?: return

        val spawnerItemStack = drop ?: SilkUtil.hookIntoSilkSpanwers().newSpawnerItem(entityID, null, 1, false)
        drop = spawnerItemStack.turnIntoSpawner(infernalType)
    }

    private val dropSpawners: Boolean
        get() = config.get(ConfigKeys.ENABLE_SPAWNER_DROPS)

    private fun CreatureSpawner.getSpawnerCategory(): String? = pdc.get(keyChain.spawnerCategoryKey, PersistentDataType.STRING)
}
