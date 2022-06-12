package com.github.secretx33.infernalmobsreloaded.repository

import com.github.secretx33.infernalmobsreloaded.eventbus.EventBus
import com.github.secretx33.infernalmobsreloaded.eventbus.internalevent.PluginReload
import com.github.secretx33.infernalmobsreloaded.model.items.LootItem
import com.github.secretx33.infernalmobsreloaded.util.other.YamlManager
import org.bukkit.plugin.Plugin
import toothpick.InjectConstructor
import java.util.logging.Logger
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

@Singleton
@InjectConstructor
class GlobalDropsRepo (
    plugin: Plugin,
    eventBus: EventBus,
    private val logger: Logger,
    private val lootItemsRepo: LootItemsRepo,
) {
    private val manager = YamlManager(plugin, "global_drops")
    private var globalDrops = emptyMap<LootItem, Double>()

    init {
        reload()
        eventBus.subscribe<PluginReload>(this, 60) { reload() }
    }

    fun getGlobalDrops() = globalDrops

    private fun reload() {
        manager.reload()
        loadGlobalDrops()
    }

    private fun loadGlobalDrops() {
        globalDrops = getGlobalLootTable()
    }

    // get map containing <Item, DropChance> what items should be globally dropped by infernal mobs and what is the chance of them being dropped
    private fun getGlobalLootTable(): Map<LootItem, Double> {
        val loots = manager.getStringList("global-drops").filter { it.isNotBlank() }

        // if there's no loot
        if(loots.isEmpty()) return emptyMap()

        // print error in console for any missing loot item
        loots.filter { !lootItemsRepo.hasLootItem(it.split(':')[0]) }.forEach {
            logger.warning("[${manager.fileName}] Loot item named '$it' could not be found, please fix your loot item configurations or your global loot configurations and reload.")
        }

        val lootItems = HashMap<LootItem, Double>()

        loots.filter { lootItemsRepo.hasLootItem(it.split(':')[0]) }.forEach { line ->
            val fields = line.split(':')
            val item = lootItemsRepo.getLootItem(fields[0])

            if(fields.size == 1) {
                lootItems[item] = 1.0
                return@forEach
            }
            // if 'chance' part of this item loot is invalid (by typoing the number)
            val chance = fields[1].toDoubleOrNull()?.let { max(0.0, min(1.0, it)) } ?: run {
                logger.warning("[${manager.fileName}] Inside global drop of item '${fields[0]}, chance '${fields[1]}' is invalid, please fix your configurations and reload. Defaulting this item drop chance to 100%.")
                1.0
            }
            lootItems[item] = chance
        }
        return lootItems
    }
}
