package com.github.secretx33.infernalmobsreloaded.repositories

import com.github.secretx33.infernalmobsreloaded.model.InfernalMobType
import com.github.secretx33.infernalmobsreloaded.model.LootItem
import com.github.secretx33.infernalmobsreloaded.utils.YamlManager
import com.github.secretx33.infernalmobsreloaded.utils.formattedTypeName
import com.google.common.collect.ImmutableSet
import com.google.common.collect.ImmutableSetMultimap
import me.mattstudios.msg.adventure.AdventureMessage
import net.kyori.adventure.text.Component
import org.bukkit.entity.ComplexLivingEntity
import org.bukkit.entity.EntityType
import org.bukkit.plugin.Plugin
import java.util.*
import java.util.logging.Logger
import kotlin.math.max
import kotlin.math.min

class InfernalMobTypesRepo (
    plugin: Plugin,
    private val log: Logger,
    private val adventureMessage: AdventureMessage,
    private val lootItemsRepo: LootItemsRepo,
) {
    private val manager = YamlManager(plugin, "mobs")
    private var infernoTypeNames = emptyList<String>()                    // original groupNames
    private var infernoTypeCache = emptyMap<String, InfernalMobType>()    // lowercase groupName, infernoType
    private var infernoTypeMultimap = ImmutableSetMultimap.of<EntityType, InfernalMobType>()  // entityType, set<infernoType>

    init { reload() }

    fun reload() {
        manager.reload()
        ensureUniqueKeys()
        loadMobTypes()
    }

    fun canTypeBecomeInfernal(type: EntityType) = infernoTypeMultimap.containsKey(type)

    fun getInfernalTypeOrNull(name: String) = infernoTypeCache[name.toLowerCase(Locale.US)]

    fun getInfernalTypes(entityType: EntityType): ImmutableSet<InfernalMobType> = infernoTypeMultimap[entityType]

    fun isValidInfernalType(name: String) = infernoTypeCache.containsKey(name.toLowerCase(Locale.US))

    // weighted by mob type, so it won't produce more than one type of entity vs another
    fun getRandomInfernalType() = infernoTypeMultimap[infernoTypeMultimap.keys().random()].random()

    private fun ensureUniqueKeys() {
        val duplicatedKeys = manager.getKeys(false).groupBy { it.toLowerCase(Locale.US) }.filterValues { it.size > 1 }
        // if there are duplicates in keys
        if(duplicatedKeys.isNotEmpty()) {
            val sb = StringBuilder("Oops, seems like there are duplicate mob categories in file '${manager.fileName}', remember that categories are caSE inSenSiTiVe, so make sure that each category has a unique name. Duplicated mob categories: ")
            duplicatedKeys.entries.forEachIndexed { index, (k, v) ->
                sb.append("\n${index + 1}) $k = {${v.joinToString()}}")
            }
            log.warning(sb.toString())
        }
    }

    private fun loadMobTypes() {
        infernoTypeNames = manager.getKeys(false).sorted()
        infernoTypeCache = infernoTypeNames.map { it.toLowerCase(Locale.US) }.associateWithTo(HashMap(infernoTypeNames.size)) { makeMobType(it) }
        val builder = ImmutableSetMultimap.builder<EntityType, InfernalMobType>()
        infernoTypeCache.forEach { (_, infernoType) -> builder.put(infernoType.entityType, infernoType) }
        infernoTypeMultimap = builder.build()
    }

    private fun makeMobType(name: String): InfernalMobType {
        val type = getMobType(name)
        val displayName = getMobDisplayName(name, type)
        val bossBarName = getMobBossBarName(name, type)
        val spawnChance = getMobSpawnChance(name)
        // TODO("Add the spawner and spawner drop chance")
        val abilityAmounts = getAbilityAmounts(name)
        val hpMultiplierAmounts = getHealthMultiplierAmounts(name)
        val lootTable = getMobLootTable(name)
        return InfernalMobType(name,
            displayName = displayName,
            bossBarName = bossBarName,
            entityType = type,
            spawnChance = spawnChance,
            minAbilities = abilityAmounts.first,
            maxAbilities = abilityAmounts.second,
            minHealthMulti = hpMultiplierAmounts.first,
            maxHealthMulti = hpMultiplierAmounts.second,
            loots = lootTable,
        )
    }

    private fun getMobType(name: String): EntityType {
        val mobType = manager.getString("$name.type") ?: ""

        // if type is absent or blank
        if(mobType.isBlank()) {
            log.warning("You must provide a type of mob for the category '$name'! Please fix your mobs configurations and reload, defaulting $name mob type to Zombie.")
            return EntityType.ZOMBIE
        }

        return EntityType.values().firstOrNull { it.entityClass != null && it.entityClass !is ComplexLivingEntity && it.isSpawnable && it.name.equals(mobType, ignoreCase = true) } ?: run {
            log.warning("Inside mob category '$name', mob of type '$mobType' is invalid or doesn't exist, please fix your mobs configurations. Defaulting $name mob type to Zombie.")
            EntityType.ZOMBIE
        }
    }

    private fun getMobDisplayName(name: String, type: EntityType): Component {
        val displayName = manager.getString("$name.display-name") ?: ""

        if(displayName.isBlank()) {
            log.warning("You must provide a display name for the mob category '$name'! Defaulting $name display name to its type.")
            return Component.text(type.formattedTypeName())
        }
        return adventureMessage.parse(displayName)
    }

    private fun getMobBossBarName(name: String, type: EntityType): Component {
        val displayName = manager.getString("$name.boss-bar-name") ?: ""

        if(displayName.isBlank()) {
            log.warning("You must provide a boss bar name for the mob category '$name'! Defaulting $name display name to its type.")
            return Component.text(type.formattedTypeName())
        }
        return adventureMessage.parse(displayName)
    }

    private fun getMobSpawnChance(name: String): Double {
        val spawnChance = manager.getDouble("$name.spawn-chance", Double.MIN_VALUE)

        // if user forgot to insert the spawnChance of that mob category
        if(spawnChance == Double.MIN_VALUE) {
            log.warning("You must provide a spawn chance for the mob category '$name'! Please fix your mobs configurations and reload, defaulting $name spawn chance to 15%.")
            return 0.15
        }
        return max(0.0, min(1.0, spawnChance))
    }

    // returns a pair with the <Min, Max> amount of abilities that that infernal mob will have
    private fun getAbilityAmounts(name: String): Pair<Int, Int> {
        val amounts = (manager.getString("$name.ability-amount") ?: "").split('-', limit = 2)

        // if there's no amount field, default it to 0
        if(amounts[0].isBlank()) return Pair(0, 0)

        // if typed amount is not an integer
        val minAmount = amounts[0].toIntOrNull()?.let { max(0, it) } ?: run {
            log.warning("Ability amount '${amounts[0]}' provided for mob category '$name' is not an integer, please fix your configurations and reload. Defaulting '$name' ability amount to 1.")
            return Pair(1, 1)
        }

        // if there's only one number, min and max amounts should be equal
        if(amounts.size < 2 || amounts[1].isBlank()) return Pair(minAmount, minAmount)

        val maxAmount = amounts[1].toIntOrNull()?.let { max(minAmount, it) } ?: run {
            log.warning("Max ability amount '${amounts[1]}' provided for mob category '$name' is not an integer, please fix the typo and reload the configurations. Defaulting '$name' max amount to its minimum amount, which is $minAmount.")
            minAmount
        }
        return Pair(minAmount, maxAmount)
    }

    // returns a pair with the <Min, Max> amount of the health multiplier that that infernal mob will have
    private fun getHealthMultiplierAmounts(name: String): Pair<Double, Double> {
        val amounts = (manager.getString("$name.health-multiplier") ?: "").split('-', limit = 2)

        // if there's no amount field, default it to 0
        if(amounts[0].isBlank()) return Pair(1.0, 1.0)

        // if typed amount is not an integer
        val minAmount = amounts[0].toDoubleOrNull()?.let { max(0.01, it) } ?: run {
            log.warning("Inside mob category '$name', health multiplier provided '${amounts[0]}' is not a double, please fix your configurations and reload. Defaulting '$name' health multiplier to 1.")
            return Pair(1.0, 1.0)
        }

        // if there's one one number, min and max amounts should be equal
        if(amounts.size < 2 || amounts[1].isBlank()) return Pair(minAmount, minAmount)

        val maxAmount = amounts[1].toDoubleOrNull()?.let { max(minAmount, it) } ?: run {
            log.warning("Inside mob category '$name', max health multiplier provided '${amounts[0]}' is not a double, please fix the typo and reload the configurations. Defaulting '$name' max amount to its minimum amount, which is $minAmount.")
            minAmount
        }
        return Pair(minAmount, maxAmount)
    }

    // get map containing <Item, DropChance> what items should be dropped by that infernal mob type and what is the chance of them being dropped
    private fun getMobLootTable(name: String): Map<LootItem, Double> {
        val loots = manager.getStringList("$name.loot-table")

        // if there's no loot
        if(loots.isEmpty()) return emptyMap()

        // print error in console for any missing loot item
        loots.filter { !lootItemsRepo.hasLootItem(it.split(':')[0]) }.forEach {
            log.warning("Inside mob category $name, loot item named '$it' was not found! Please fix your mobs or item loot configurations and reload.")
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
                log.warning("Inside drop for item '${fields[0]} of mob category '$name', chance '${fields[1]}' is invalid, please fix your configurations and reload. Defaulting this item drop chance to 100%.")
                1.0
            }
            lootItems[item] = chance
        }
        return lootItems
    }
}
