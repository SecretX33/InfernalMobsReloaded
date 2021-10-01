package com.github.secretx33.infernalmobsreloaded.eventlisteners.integration

import com.github.secretx33.infernalmobsreloaded.config.Config
import com.github.secretx33.infernalmobsreloaded.config.ConfigKeys
import com.github.secretx33.infernalmobsreloaded.model.KeyChain
import com.github.secretx33.infernalmobsreloaded.repositories.InfernalMobTypesRepo
import com.github.secretx33.infernalmobsreloaded.utils.extension.pdc
import com.github.secretx33.infernalmobsreloaded.utils.extension.turnIntoSpawner
import de.dustplanet.silkspawners.events.SilkSpawnersSpawnerBreakEvent
import de.dustplanet.util.SilkUtil
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.block.CreatureSpawner
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin
import java.util.EnumSet

class SpawnerBreakWithSilkSpawnersListener(
    plugin: Plugin,
    private val config: Config,
    private val infernalMobTypeRepo: InfernalMobTypesRepo,
    private val keyChain: KeyChain,
): Listener {

    init { Bukkit.getPluginManager().registerEvents(this, plugin) }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private fun SilkSpawnersSpawnerBreakEvent.infernalSpawnerBreak() {
        if(!dropSpawners) return

        val spawnerType = spawner?.getSpawnerCategory() ?: return
        val infernalType = infernalMobTypeRepo.getInfernalTypeOrNull(spawnerType) ?: return

        val spawnerItemStack = drop ?: SilkUtil.hookIntoSilkSpanwers().newSpawnerItem(entityID, null, 1, false)
        drop = spawnerItemStack.turnIntoSpawner(infernalType)
    }

    private val dropSpawners
        get() = config.get<Boolean>(ConfigKeys.ENABLE_SPAWNER_DROPS)

    private fun CreatureSpawner.getSpawnerCategory() = pdc.get(keyChain.spawnerCategoryKey, org.bukkit.persistence.PersistentDataType.STRING)

    private companion object {
        /**
         * What gamemodes the player can be in that are allowed to drop infernal mob spawners.
         */
        val allowedGameModes: Set<GameMode> = EnumSet.of(GameMode.SURVIVAL, GameMode.ADVENTURE)
    }
}
