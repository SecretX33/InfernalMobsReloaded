package com.github.secretx33.infernalmobsreloaded.repositories

import com.github.secretx33.infernalmobsreloaded.model.InfernalMobType
import com.github.secretx33.infernalmobsreloaded.model.LootItem
import com.github.secretx33.infernalmobsreloaded.utils.YamlManager
import com.github.secretx33.infernalmobsreloaded.utils.formattedTypeName
import me.mattstudios.msg.adventure.AdventureMessage
import net.kyori.adventure.text.Component
import org.bukkit.entity.EntityType
import org.bukkit.plugin.Plugin
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.logging.Logger
import kotlin.math.max
import kotlin.math.min

class InfernalMobTypesRepo(plugin: Plugin, private val log: Logger, private val adventureMessage: AdventureMessage, private val lootItemsRepo: LootItemsRepo) {

    private val manager   = YamlManager(plugin, "mobs")
    private val typeCache = ConcurrentHashMap<String, InfernalMobType>()     // lowercase groupName, allInfos
    private val typeList  = CopyOnWriteArrayList<String>()                   // original groupNames
    private val validEntities = HashSet<EntityType>()

    init { loadMobTypes() }

    fun reload() {
        manager.reload()
        typeCache.clear()
        typeList.clear()
        validEntities.clear()
        loadMobTypes()
        validEntities.addAll(typeCache.values.map { it.type })
    }

    fun canTypeBecomeInfernal(type: EntityType) = validEntities.contains(type)

//    fun getTypeOrNull(group: String): HarvestBlockGroup? = typeCache[group.toLowerCase(Locale.US)]
//
//    fun getGroup(group: String): HarvestBlockGroup = getGroupOrNull(group) ?: throw NoSuchElementException("HarvestBlock for group $group was not found.")
//
//    fun hasGroup(group: String) = typeCache.containsKey(group.toLowerCase(Locale.US))
//
//    fun getAllGroups(): List<String> = typeList

    // used to get "current case" version of a group name, because wands store the group name the way it were, before possible case changes
//    fun getGroupFromName(group: String): String? = typeList.firstOrNull { it.equals(group, ignoreCase = true) }

    private fun loadMobTypes() {
        typeList.apply {
            addAll(manager.getKeys(false))
            sort()
            forEach { group -> typeCache[group.toLowerCase(Locale.US)] = makeMobType(group) }
        }
    }

    private fun makeMobType(name: String): InfernalMobType {
        val type = getMobType(name)
        val displayName = getMobDisplayName(name, type)
        val spawnChance = getMobSpawnChance(name)
        // TODO("Add the spawner and spawner drop chance")
        val lootTable = getMobLootTable(name)
        return InfernalMobType(name,
            displayName = displayName,
            type = type,
            spawnChance = spawnChance,
            loots = lootTable,
        )
    }

    private fun getMobLootTable(name: String): Map<LootItem, Double> {
        val loots = manager.getStringList("$name.loot-table")

        // if there's no loot
        if(loots.isEmpty()) return emptyMap()

        // print error in console for any missing loot item
        loots.filter { !lootItemsRepo.hasLootItem(it.split(':')[0]) }.forEach {
            log.severe("Inside mob category $name, loot item named '$it' was not found! Please fix your mobs or item loot configurations and reload.")
        }

        val lootItems = HashMap<LootItem, Double>()

        loots.filter { lootItemsRepo.hasLootItem(it.split(':')[0]) }.forEach { line ->
            val fields = line.split(':')
            val item = lootItemsRepo.getLootItem(fields[0])

            if(fields.size == 1) {
                lootItems[item] = 1.0
            } else {
                // if 'chance' part of this item loot is invalid (by typoing the number)
                val chance = fields[1].toDoubleOrNull()?.let { max(0.0, min(1.0, it)) } ?: run {
                    log.severe("Inside drop for item '${fields[0]} of mob category '$name', chance '${fields[1]}' is invalid, please fix your configurations and reload. Defaulting this item drop chance to 100%.")
                    1.0
                }
                lootItems[item] = chance
            }
        }
        return lootItems
    }

    private fun getMobSpawnChance(name: String): Double {
        val spawnChance = manager.getDouble("$name.spawn-chance", Double.MIN_VALUE)

        // if user forgot to insert the spawnChance of that mob category
        if(spawnChance == Double.MIN_VALUE) {
            log.severe("You must provide a spawn chance for the mob category '$name'! Please fix your mobs configurations and reload, defaulting $name spawn chance to 15%.")
            return 0.15
        }
        return max(0.0, min(1.0, spawnChance))
    }

    private fun getMobType(name: String): EntityType {
        val mobType = manager.getString("$name.type") ?: ""

        // if name is absent or blank
        if(mobType.isBlank()) {
            log.severe("You must provide a type of mob for the category '$name'! Please fix your mobs configurations and reload, defaulting $name mob type to Zombie.")
            return EntityType.ZOMBIE
        }

         return EntityType.values().firstOrNull { it.name.equals(mobType, ignoreCase = true) } ?: run {
             log.severe("Inside mob category '$name', mob of type '$mobType' doesn't exist, please fix your mobs configurations. Defaulting $name mob type to Zombie.")
             EntityType.ZOMBIE
         }
    }

    private fun getMobDisplayName(name: String, type: EntityType): Component {
        val displayName = manager.getString("$name.display-name") ?: ""

        if(displayName.isBlank()) {
            log.severe("You must provide a display name for the mob category '$name'! Defaulting $name display name to its type.")
            return Component.text(type.formattedTypeName())
        }
        return adventureMessage.parse(displayName)
    }
}
