package com.github.secretx33.infernalmobsreloaded.repositories

import com.github.secretx33.infernalmobsreloaded.config.Config
import com.github.secretx33.infernalmobsreloaded.config.ConfigKeys
import com.github.secretx33.infernalmobsreloaded.model.Ability
import com.github.secretx33.infernalmobsreloaded.model.InfernalMobType
import com.github.secretx33.infernalmobsreloaded.model.items.LootItem
import com.github.secretx33.infernalmobsreloaded.utils.YamlManager
import com.github.secretx33.infernalmobsreloaded.utils.formattedTypeName
import com.github.secretx33.infernalmobsreloaded.utils.matchOrNull
import com.google.common.collect.ImmutableSet
import com.google.common.collect.ImmutableSetMultimap
import me.mattstudios.msg.adventure.AdventureMessage
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import org.bukkit.entity.ComplexLivingEntity
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.plugin.Plugin
import org.koin.core.component.KoinApiExtension
import java.util.*
import java.util.logging.Logger
import kotlin.math.max
import kotlin.math.min

@KoinApiExtension
class InfernalMobTypesRepo (
    plugin: Plugin,
    private val log: Logger,
    private val config: Config,
    private val adventureMessage: AdventureMessage,
    private val lootItemsRepo: LootItemsRepo,
) {
    private val manager = YamlManager(plugin, "mobs")
    private var infernalTypeNames = emptyList<String>()                    // original groupNames
    private var infernalTypeCache = emptyMap<String, InfernalMobType>()    // lowercase groupName, infernalType
    private var infernalTypeMultimap = ImmutableSetMultimap.of<EntityType, InfernalMobType>()  // entityType, set<infernalType>
    private var userDefinedInfernalTypes = emptyList<Pair<EntityType, InfernalMobType>>()      // list containing all mobs types, except the internal ones like 'ghost' and 'evil_ghost', used as cache for the 'getRandomInfernalType' method

    init { reload() }

    fun reload() {
        manager.reload()
        ensureUniqueKeys()
        loadMobTypes()
    }

    fun canTypeBecomeInfernal(type: EntityType) = infernalTypeMultimap.containsKey(type)

    fun getInfernalTypeOrNull(name: String) = infernalTypeCache[name.lowercase(Locale.US)]

    fun getInfernalTypes(entityType: EntityType): ImmutableSet<InfernalMobType> = infernalTypeMultimap[entityType]

    fun isValidInfernalType(name: String) = infernalTypeCache.containsKey(name.lowercase(Locale.US))

    fun getAllInfernalTypeNames() = infernalTypeNames

    // try to get another type of infernal, but if there's just a single type of mob, just return one of those as fallback
    fun getRandomInfernalType(morphingEntity: LivingEntity) = userDefinedInfernalTypes.filter { morphingEntity.type != it.first }.randomOrNull()?.second ?: userDefinedInfernalTypes.random().second

    private fun ensureUniqueKeys() {
        val duplicatedKeys = manager.getKeys(false).groupBy { it.lowercase(Locale.US) }.filterValues { it.size > 1 }
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
        infernalTypeNames = manager.getKeys(false).sorted()
        infernalTypeCache = infernalTypeNames.map { it.lowercase(Locale.US) }.associateWithTo(HashMap(infernalTypeNames.size)) { makeMobType(it) }
        val builder = ImmutableSetMultimap.builder<EntityType, InfernalMobType>()
        infernalTypeCache.forEach { (_, infernalType) -> builder.put(infernalType.entityType, infernalType) }
        infernalTypeMultimap = builder.build()
        userDefinedInfernalTypes = infernalTypeMultimap.asMap()
            .mapValues { (type, list) -> list.map { Pair(type, it) } }
            .values.flatten()
            .filter { (_, infernal) -> !infernal.name.equals("ghost", ignoreCase = true) && !infernal.name.equals("haunted_ghost", ignoreCase = true) }
    }

    private fun makeMobType(name: String): InfernalMobType {
        val type = getMobType(name)
        val displayName = getMobDisplayName(name, type)
        val bossBarName = getMobBossBarName(name, type)
        val bossBarColor = getMobBossBarColor(name)
        val bossBarOverlay = getMobBossBarOverlay(name)
        val bossBarFlags = getMobBossBarFlags(name)
        val spawnChance = getMobSpawnChance(name)
        val spawnerDropChance = getSpawnerDropChance(name)
        val spawnerName = getSpawnerName(name, type, spawnerDropChance)
        val abilityAmounts = getAbilityAmounts(name)
        val followRangeMultiplierAmounts = getFollowRangeMultiplierAmounts(name)
        val damageMultiplierAmounts = getDamageMultiplierAmounts(name)
        val atkKnockbackAmounts = getAtkKnockbackModAmounts(name)
        val hpMultiplierAmounts = getHealthMultiplierAmounts(name)
        val speedMultiplierAmounts = getSpeedMultiplierAmounts(name)
        val lootTable = getMobLootTable(name)
        return InfernalMobType(name,
            displayName = displayName,
            bossBarName = bossBarName,
            bossBarColor = bossBarColor,
            bossBarOverlay = bossBarOverlay,
            bossBarFlags = bossBarFlags,
            entityType = type,
            spawnChance = spawnChance,
            mobSpawnerName = spawnerName,
            mobSpawnerDropChance = spawnerDropChance,
            minAbilities = abilityAmounts.first,
            maxAbilities = abilityAmounts.second,
            minFollowRangeMulti = followRangeMultiplierAmounts.first,
            maxFollowRangeMulti = followRangeMultiplierAmounts.second,
            minDamageMulti = damageMultiplierAmounts.first,
            maxDamageMulti = damageMultiplierAmounts.second,
            minAttackKnockbackMod = atkKnockbackAmounts.first,
            maxAttackKnockbackMod = atkKnockbackAmounts.second,
            minHealthMulti = hpMultiplierAmounts.first,
            maxHealthMulti = hpMultiplierAmounts.second,
            minSpeedMulti = speedMultiplierAmounts.first,
            maxSpeedMulti = speedMultiplierAmounts.second,
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
        val displayName = manager.getString("$name.boss-bar-text") ?: ""

        // if boss bar name is absent or blank, fallback to entity type
        if(displayName.isBlank()) {
            if(bossBarEnabled) log.warning("You must provide a boss bar name for the mob category '$name'! Defaulting $name display name to its type.")
            return Component.text(type.formattedTypeName())
        }
        return adventureMessage.parse(displayName)
    }

    private fun getMobBossBarColor(name: String): BossBar.Color {
        val barColor = manager.getString("$name.boss-bar-color") ?: ""

        // if boss bar color is absent or blank, fallback to PURPLE
        if(barColor.isBlank()) {
            if(bossBarEnabled) log.warning("You must provide a boss bar color for the mob category '$name'! Defaulting $name boss bar color to Purple.")
            return BossBar.Color.PURPLE
        }

        return BossBar.Color.values().firstOrNull { it.name.equals(barColor, ignoreCase = true) } ?: run {
            log.warning("Inside mob category '$name', boss bar color named '$barColor' is invalid or doesn't exist, please fix your mobs configurations. Defaulting $name boss bar color to Purple.")
            BossBar.Color.PURPLE
        }
    }

    private fun getMobBossBarOverlay(name: String): BossBar.Overlay {
        val bossBarOverlay = manager.getString("$name.boss-bar-overlay") ?: ""

        // if boss bar overlay style is absent or blank, fallback to NOTCHED_12
        if(bossBarOverlay.isBlank()) {
            if(bossBarEnabled) log.warning("You must provide a boss bar overlay style for the mob category '$name'! Defaulting $name boss bar overlay style to NOTCHED_12.")
            return BossBar.Overlay.NOTCHED_12
        }

        return BossBar.Overlay.values().firstOrNull { it.name.equals(bossBarOverlay, ignoreCase = true) } ?: run {
            log.warning("Inside mob category '$name', boss bar overlay style named '$bossBarOverlay' is invalid or doesn't exist, please fix your mobs configurations. Defaulting $name boss bar overlay style to NOTCHED_12.")
            BossBar.Overlay.NOTCHED_12
        }
    }

    private fun getMobBossBarFlags(name: String): Set<BossBar.Flag> {
        val bossBarFlags = manager.getStringList("$name.boss-bar-flags")

        // if there's no flag set or boss bars are disabled, return empty set
        if(bossBarFlags.isEmpty() || !bossBarEnabled) return emptySet()

        return bossBarFlags.mapNotNullTo(HashSet()) { line ->
            BossBar.Flag.values().firstOrNull { it.name.equals(line, ignoreCase = true) } ?: run {
                log.warning("Inside mob category '$name', boss bar flag named '$line' is invalid or doesn't exist, please fix your mobs configurations.")
                null
            }
        }
    }

    private fun getMobSpawnChance(name: String): Double {
        val spawnChance = manager.getDouble("$name.spawn-chance", Double.NEGATIVE_INFINITY)

        // if user forgot to insert the spawnChance of that mob category
        if(spawnChance == Double.NEGATIVE_INFINITY) {
            log.warning("You must provide a spawn chance for the mob category '$name'! Please fix your mobs configurations and reload, defaulting $name spawn chance to 15%.")
            return 0.15
        }
        return max(0.0, min(1.0, spawnChance))
    }

    private fun getSpawnerDropChance(name: String): Double {
        val spawnerDropChance = manager.getDouble("$name.mob-spawn-drop-chance", Double.NEGATIVE_INFINITY)

        // if user didn't insert the spawnerDropChance of that mob category, means he doesn't want that infernal to drop any spawner
        if(spawnerDropChance == Double.NEGATIVE_INFINITY) return 0.0

        return max(0.0, min(1.0, spawnerDropChance))
    }

    private fun getSpawnerName(name: String, type: EntityType, dropChance: Double): Component {
        val displayName = manager.getString("$name.mob-spawner-name") ?: ""

        // if boss bar name is absent or blank, fallback to entity type
        if(displayName.isBlank()) {
            if(dropChance > 0) log.warning("You must provide a mob spawner name for the mob category '$name'! Defaulting $name mob spawner name to its type.")
            return Component.text("Mob Spawner (${type.formattedTypeName()})")
        }
        return adventureMessage.parse(displayName)
    }

    // returns a pair with the <Min, Max> amount of abilities that that infernal mob will have
    private fun getAbilityAmounts(name: String) = getIntPair(name, "ability-amount", maxValue = Ability.MAX_AMOUNT_OF_SIMULTANEOUS_ABILITIES)

    // returns a pair with the <Min, Max> amount of the follow range multiplier that that infernal mob will have
    private fun getFollowRangeMultiplierAmounts(name: String) = getDoublePair(name, "follow-range-multiplier")

    // returns a pair with the <Min, Max> amount of the damage multiplier that that infernal mob will have
    private fun getDamageMultiplierAmounts(name: String) = getDoublePair(name, "damage-multiplier")

    // returns a pair with the <Min, Max> amount of the attack knockback modifier that that infernal mob will have
    private fun getAtkKnockbackModAmounts(name: String) = getDoublePair(name, "attack-knockback-modifier", default = 0.0, minValue = Double.NEGATIVE_INFINITY)

    // returns a pair with the <Min, Max> amount of the health multiplier that that infernal mob will have
    private fun getHealthMultiplierAmounts(name: String) = getDoublePair(name, "health-multiplier", minValue = 0.01)

    // returns a pair with the <Min, Max> amount of the speed multiplier that that infernal mob will have
    private fun getSpeedMultiplierAmounts(name: String) = getDoublePair(name, "speed-multiplier")

    // returns a pair with the <Min, Max> amount of the key property
    private fun getIntPair(name: String, key: String, default: Int = 0, minValue: Int = 0, maxValue: Int = Int.MAX_VALUE): Pair<Int, Int> {
        val values = manager.getString("$name.$key") ?: ""

        // if there's no amount field, return pair with default values
        if(values.isBlank()) return Pair(default, default)

        // value is only one value
        SIGNED_INT.matchOrNull(values, 1)
            ?.let { min(maxValue, max(minValue, it.toInt())) }
            ?.let { return Pair(it, it) }

        // value is a range of values
        SIGNED_INT_RANGE.matchEntire(values)?.groupValues
            ?.subList(1, 3)
            ?.map { min(maxValue, max(minValue, it.toInt())) }
            ?.let { return Pair(it[0], max(it[0], it[1])) }

        // typed amount is not an integer
        log.warning("Inside mob category '$name', $key provided '$values' is not an integer nor an integer range, please fix your configurations and reload. Defaulting '$name' $key to $default.")
        return Pair(default, default)
    }

    // returns a pair with the <Min, Max> amount of the key property
    private fun getDoublePair(name: String, key: String, default: Double = 1.0, minValue: Double = 0.0, maxValue: Double = Double.MAX_VALUE): Pair<Double, Double> {
        val values = manager.getString("$name.$key") ?: ""

        // if there's no amount field, return pair with default values
        if(values.isBlank()) return Pair(default, default)

        // value is only one value
        SIGNED_DOUBLE.matchOrNull(values, 1)
            ?.let { min(maxValue, max(minValue, it.toDouble())) }
            ?.let { return Pair(it, it) }

        // value is a range of values
        SIGNED_DOUBLE_RANGE.matchEntire(values)?.groupValues
            ?.subList(1, 3)
            ?.map { min(maxValue, max(minValue, it.toDouble())) }
            ?.let { return Pair(it[0], max(it[0], it[1])) }

        // typed amount is not a double
        log.warning("Inside mob category '$name', $key provided '$values' is not a double nor a double range, please fix your configurations and reload. Defaulting '$name' $key to $default.")
        return Pair(default, default)
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

    private val bossBarEnabled get() = config.get<Boolean>(ConfigKeys.ENABLE_BOSS_BARS)

    private companion object {                                                             // regex matches examples
        val SIGNED_INT = """^\s*(-?\d{1,11})\s*$""".toRegex()                              // "-5"     (-5)
        val SIGNED_INT_RANGE = """^\s*(-?\d{1,11}?)\s*-\s*(-?\d{1,11})\s*$""".toRegex()    // "-5 - -1"  (-5 until -1)
        val SIGNED_DOUBLE = """^\s*(-?\d+?(?:\.\d+?)?)\s*$""".toRegex()                    // "-5.0"   (-5.0)
        val SIGNED_DOUBLE_RANGE = """^\s*(-?\d+?(?:\.\d+?)?)\s*-\s*(-?\d+?(?:\.\d+)?)\s*$""".toRegex()  // "-5.0 - -1.0"  (-5.0 until -1.0)
    }
}
