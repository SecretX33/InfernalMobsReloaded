package com.github.secretx33.infernalmobsreloaded.eventlisteners.hook

import com.github.secretx33.infernalmobsreloaded.config.Config
import com.github.secretx33.infernalmobsreloaded.config.ConfigKeys
import com.github.secretx33.infernalmobsreloaded.model.KeyChain
import com.github.secretx33.infernalmobsreloaded.repositories.InfernalMobTypesRepo
import com.github.secretx33.infernalmobsreloaded.utils.extension.pdc
import com.github.secretx33.infernalmobsreloaded.utils.extension.turnIntoSpawner
import de.dustplanet.silkspawners.events.SilkSpawnersSpawnerBreakEvent
import de.dustplanet.util.SilkUtil
import org.bukkit.Bukkit
import org.bukkit.block.CreatureSpawner
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin

class SpawnerBreakWithSilkSpawnersListener(
    plugin: Plugin,
    private val config: Config,
    private val infernalMobTypeRepo: InfernalMobTypesRepo,
    private val keyChain: KeyChain,
) : Listener {

    init { Bukkit.getPluginManager().registerEvents(this, plugin) }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private fun SilkSpawnersSpawnerBreakEvent.infernalSpawnerBreak() {
        if(!dropSpawners) return

        val spawnerType = spawner?.getSpawnerCategory() ?: return
        val infernalType = infernalMobTypeRepo.getInfernalTypeOrNull(spawnerType) ?: return

        val spawnerItemStack = drop ?: SilkUtil.hookIntoSilkSpanwers().newSpawnerItem(entityID, null, 1, false)
        drop = spawnerItemStack.turnIntoSpawner(infernalType)
    }

    private val dropSpawners: Boolean
        get() = config.get(ConfigKeys.ENABLE_SPAWNER_DROPS)

    private fun CreatureSpawner.getSpawnerCategory() = pdc.get(keyChain.spawnerCategoryKey, org.bukkit.persistence.PersistentDataType.STRING)
}
