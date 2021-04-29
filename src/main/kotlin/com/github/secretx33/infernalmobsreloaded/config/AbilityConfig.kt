package com.github.secretx33.infernalmobsreloaded.config

import com.github.secretx33.infernalmobsreloaded.model.Abilities
import com.github.secretx33.infernalmobsreloaded.utils.YamlManager
import me.mattstudios.msg.adventure.AdventureMessage
import org.bukkit.plugin.Plugin
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger
import kotlin.math.max
import kotlin.math.min

class AbilityConfig (
    plugin: Plugin,
    private val log: Logger,
    private val adventureMessage: AdventureMessage,
) {
    private val manager = YamlManager(plugin, "abilities")
    private val cache = ConcurrentHashMap<String, Any>()

    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: String, default: T): T {
        return cache.getOrPut(key) {
            manager.get(key, default)
        } as? T ?: run {
            log.severe("On ability entry $key, expected value of type ${default!!::class.java.simpleName} but got ${manager.get(key)?.javaClass?.simpleName} instead, please fix your ${manager.fileName} file and reload")
            default
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: AbilityConfigKeys): T = get(key.configEntry, key.defaultValue) as T

    fun <T> get(key: AbilityConfigKeys, default: T): T = get(key.configEntry, default)

    fun getPotionIsAmbient(ability: Abilities) = get("${ability.configEntry}.is-ambient", true)

    fun getPotionEmitParticles(ability: Abilities) = get("${ability.configEntry}.emit-particles", true)

    fun getAbilityChance(ability: Abilities, default: Double) = get("${ability.configEntry}.chance", default)

    fun getRecheckDelay(ability: Abilities, default: Double) = get("${ability.configEntry}.recheck-delay", default)

    fun getDuration(ability: Abilities, default: Double) = get("${ability.configEntry}.duration", default)

    // returns a pair of ints containing the <Min, Max> value of that property
    @Suppress("UNCHECKED_CAST")
    fun getIntPair(key: AbilityConfigKeys, default: Int = key.defaultValue as Int, minValue: Int = 0, maxValue: Int = Int.MAX_VALUE): Pair<Int, Int> {
        return cache.getOrPut(key.configEntry) {
            val amounts = (manager.getString(key.configEntry) ?: "").split('-', limit = 2)

            // if there's no amount field, use default value
            if (amounts[0].isBlank()) return@getOrPut Pair(default, default)

            // if typed amount is not an integer
            val minAmount = amounts[0].toIntOrNull()?.let { max(minValue, min(maxValue, it)) } ?: run {
                log.severe("Oops, while trying to get ability '${key.configEntry}' value, could not parse '${amounts[0]}' because it's not an integer, please fix your configurations and reload. Defaulting '${key.configEntry}' value to $default.")
                return Pair(default, default)
            }

            // if there's only one number, min and max amounts should be equal
            if (amounts.size < 2 || amounts[1].isBlank()) return@getOrPut Pair(minAmount, minAmount)

            val maxAmount = amounts[1].toIntOrNull()?.let { max(minAmount, min(maxValue, it)) } ?: run {
                log.severe("Max value '${amounts[1]}' provided for ability entry '${key.configEntry}' is not an integer, please fix the typo and reload the configurations. Defaulting '${key.configEntry}' max value to its minimum amount, which is $minAmount.")
                minAmount
            }
            Pair(minAmount, maxAmount)
        } as Pair<Int, Int>
    }

    // returns a pair of doubles containing the <Min, Max> value of that property
    @Suppress("UNCHECKED_CAST")
    fun getDoublePair(key: AbilityConfigKeys, default: Double = key.defaultValue as Double, minValue: Double = 0.0, maxValue: Double = Double.MAX_VALUE): Pair<Double, Double> {
        return cache.getOrPut(key.configEntry) {
            val amounts = (manager.getString(key.configEntry) ?: "").split('-', limit = 2)

            // if there's no amount field, use default value
            if (amounts[0].isBlank()) return@getOrPut Pair(default, default)

            // if typed amount is not an integer
            val minAmount = amounts[0].toDoubleOrNull()?.let { max(minValue, min(maxValue, it)) } ?: run {
                log.severe("Oops, while trying to get ability '${key.configEntry}' value, could not parse '${amounts[0]}' because it's not a double, please fix your configurations and reload. Defaulting '${key.configEntry}' value to $default.")
                return Pair(default, default)
            }

            // if there's only one number, min and max amounts should be equal
            if (amounts.size < 2 || amounts[1].isBlank()) return@getOrPut Pair(minAmount, minAmount)

            val maxAmount = amounts[1].toDoubleOrNull()?.let { max(minAmount, min(maxValue, it)) } ?: run {
                log.severe("Max value '${amounts[1]}' provided for entry '${key.configEntry}' is not a double, please fix the typo and reload the configurations. Defaulting '${key.configEntry}' max value to its minimum amount, which is $minAmount.")
                minAmount
            }
            Pair(minAmount, maxAmount)
        } as Pair<Double, Double>
    }

    fun reload() {
        manager.reload()
    }
}

enum class AbilityConfigKeys(val configEntry: String, val defaultValue: Any) {
    ARCHER_ARROW_SPEED("archer.arrow-speed", 2.2),
    ARCHER_ARROW_AMOUNT("archer.arrow-amount", 10),
    ARCHER_ARROW_DELAY("archer.arrow-delay", 0.2),
    SPEEDY_BONUS("speedy.bonus", 1.5),
    ARMOURED_POTION_LEVEL("armoured.fallback-dmg-resist-potion-level", 1),
    HEAVY_RESIST_PERCENTAGE("heavy.knockback-resist-percentage", 0.4),
}

