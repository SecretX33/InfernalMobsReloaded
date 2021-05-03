package com.github.secretx33.infernalmobsreloaded.config

import com.github.secretx33.infernalmobsreloaded.model.Ability
import com.github.secretx33.infernalmobsreloaded.utils.YamlManager
import org.bukkit.plugin.Plugin
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class AbilityConfig (
    plugin: Plugin,
    private val log: Logger,
) {
    private val manager = YamlManager(plugin, "abilities")
    private val cache = ConcurrentHashMap<String, Any>()

    fun reload() {
        cache.clear()
        manager.reload()
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: String, default: T): T {
        return cache.getOrPut(key) {
            manager.get(key, default) as? T ?: run {
                log.severe("On ability entry $key, expected value of type ${default!!::class.java.simpleName} but got ${manager.get(key)?.javaClass?.simpleName} instead, please fix your ${manager.fileName} file and reload")
                default
            }
        } as T
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: AbilityConfigKeys): T = get(key.configEntry, key.defaultValue) as T

    fun <T> get(key: AbilityConfigKeys, default: T): T = get(key.configEntry, default)

    fun getInt(key: String, default: Int, minValue: Int = 0, maxValue: Int = Int.MAX_VALUE): Int {
        return cache.getOrPut(key) {
            (manager.get(key, default) as? Int)?.let { int -> max(minValue, min(maxValue, int)) } ?: run {
                log.severe("On ability entry $key, expected value of type Int but got ${manager.get(key)?.javaClass?.simpleName} instead, please fix your ${manager.fileName} file and reload")
                default
            }
        } as Int
    }

    fun getInt(key: AbilityConfigKeys, default: Int = key.defaultValue as Int, minValue: Int = 0, maxValue: Int = Int.MAX_VALUE)
        = getInt(key.configEntry, default, minValue, maxValue)

    fun getDouble(key: String, default: Double, minValue: Double = 0.0, maxValue: Double = Double.MAX_VALUE): Double {
        return cache.getOrPut(key) {
            (manager.get(key, default) as? Double)?.let { double -> max(minValue, min(maxValue, double)) } ?: run {
                log.severe("On ability entry $key, expected value of type Double but got ${manager.get(key)?.javaClass?.simpleName} instead, please fix your ${manager.fileName} file and reload")
                default
            }
        } as Double
    }

    fun getDouble(key: AbilityConfigKeys, default: Double = key.defaultValue as Double, minValue: Double = 0.0, maxValue: Double = Double.MAX_VALUE)
        = getDouble(key.configEntry, default, minValue, maxValue)

    fun getPotionIsAmbient(ability: Ability) = get("${ability.configEntry}.potion-is-ambient", true)

    fun getPotionEmitParticles(ability: Ability) = get("${ability.configEntry}.potion-emit-particles", true)

    fun getAbilityChance(ability: Ability, default: Double, minValue: Double = 0.0, maxValue: Double = Double.MAX_VALUE)
        = getDouble("${ability.configEntry}.chance", default, minValue, maxValue)

    fun getAbilityChanceOnDamageDone(ability: Ability, default: Double, minValue: Double = 0.0, maxValue: Double = Double.MAX_VALUE)
        = getDouble("${ability.configEntry}.chance-on-damage-done", default, minValue, maxValue)

    fun getAbilityChanceOnDamageTaken(ability: Ability, default: Double, minValue: Double = 0.0, maxValue: Double = Double.MAX_VALUE)
        = getDouble("${ability.configEntry}.chance-on-damage-taken", default, minValue, maxValue)

    fun getRecheckDelay(ability: Ability, default: Double, minValue: Double = 0.01, maxValue: Double = Double.MAX_VALUE)
        = getDouble("${ability.configEntry}.recheck-delay", default, minValue, maxValue)

    fun getDuration(ability: Ability, default: Double, minValue: Double = 0.0, maxValue: Double = Double.MAX_VALUE)
        = getDoublePair("${ability.configEntry}.duration", default, minValue, maxValue)

    fun getProjectileSpeed(ability: Ability, default: Double, minValue: Double = 0.05, maxValue: Double = Double.MAX_VALUE)
        = getDouble("${ability.configEntry}.projectile-speed", default, minValue, maxValue)

    fun getAbilityPotency(ability: Ability, default: Int, minValue: Int = 0, maxValue: Int = Int.MAX_VALUE)
        = getIntPair("${ability.configEntry}.potency", default, minValue, maxValue)

    fun getDurabilityLoss(ability: Ability, default: Double, minValue: Double = 0.0, maxValue: Double = 1.0)
        = getDoublePair("${ability.configEntry}.durability-loss", default, minValue, maxValue)

    fun getAffectsOnlyPlayers(ability: Ability, default: Boolean) = get("${ability.configEntry}.affect-only-players", default)

    fun getNearbyRange(ability: Ability, default: Double, minValue: Double = 0.0, maxValue: Double = Double.MAX_VALUE)
        = getDouble("${ability.configEntry}.nearby-entities-range", default, minValue, maxValue)

    fun getDistanceMultiplier(ability: Ability, default: Double = 1.0, minValue: Double = 0.0, maxValue: Double = Double.MAX_VALUE)
        = getDouble("${ability.configEntry}.distance-multiplier", default, minValue, maxValue).let { if(it <= 1.0) it else sqrt(it) }

    fun getHeightMultiplier(ability: Ability, default: Double = 1.0, minValue: Double = 0.0, maxValue: Double = Double.MAX_VALUE)
        = getDouble("${ability.configEntry}.height-multiplier", default, minValue, maxValue).let { if(it <= 1.0) it else sqrt(it) }

    fun doesRequireLineOfSight(ability: Ability, default: Boolean = true) = get("${ability.configEntry}.require-line-of-sight", default)

    fun getSendMessage(ability: Ability, default: Boolean = true) = get("${ability.configEntry}.send-message-to-player", default)

    fun getIntAmounts(ability: Ability, default: Int, minValue: Int = 0, maxValue: Int = Int.MAX_VALUE) = getIntPair("${ability.configEntry}.amount", default, minValue, maxValue)

    // returns a pair of ints containing the <Min, Max> value of that property
    @Suppress("UNCHECKED_CAST")
    fun getIntPair(key: String, default: Int, minValue: Int = 0, maxValue: Int = Int.MAX_VALUE): Pair<Int, Int> {
        return cache.getOrPut(key) {
            val amounts = (manager.getString(key) ?: "").split('-', limit = 2)

            // if there's no amount field, use default value
            if (amounts[0].isBlank()) return@getOrPut Pair(default, default)

            // if typed amount is not an integer
            val minAmount = amounts[0].toIntOrNull()?.let { max(minValue, min(maxValue, it)) } ?: run {
                log.severe("Oops, while trying to get ability '$key' value, could not parse '${amounts[0]}' because it's not an integer, please fix your configurations and reload. Defaulting '$key' value to $default.")
                return Pair(default, default)
            }

            // if there's only one number, min and max amounts should be equal
            if (amounts.size < 2 || amounts[1].isBlank()) return@getOrPut Pair(minAmount, minAmount)

            val maxAmount = amounts[1].toIntOrNull()?.let { max(minAmount, min(maxValue, it)) } ?: run {
                log.severe("Max value '${amounts[1]}' provided for ability entry '$key' is not an integer, please fix the typo and reload the configurations. Defaulting '$key' max value to its minimum amount, which is $minAmount.")
                minAmount
            }
            Pair(minAmount, maxAmount)
        } as Pair<Int, Int>
    }

    fun getIntPair(key: AbilityConfigKeys, default: Int = key.defaultValue as Int, minValue: Int = 0, maxValue: Int = Int.MAX_VALUE)
        = getIntPair(key.configEntry, default, minValue, maxValue)

    // returns a pair of doubles containing the <Min, Max> value of that property
    @Suppress("UNCHECKED_CAST")
    fun getDoublePair(key: String, default: Double, minValue: Double = 0.0, maxValue: Double = Double.MAX_VALUE): Pair<Double, Double> {
        return cache.getOrPut(key) {
            val amounts = (manager.getString(key) ?: "").split('-', limit = 2)

            // if there's no amount field, use default value
            if (amounts[0].isBlank()) return@getOrPut Pair(default, default)

            // if typed amount is not an integer
            val minAmount = amounts[0].toDoubleOrNull()?.let { max(minValue, min(maxValue, it)) } ?: run {
                log.severe("Oops, while trying to get ability '${key}' value, could not parse '${amounts[0]}' because it's not a double, please fix your configurations and reload. Defaulting '${key}' value to $default.")
                return Pair(default, default)
            }

            // if there's only one number, min and max amounts should be equal
            if (amounts.size < 2 || amounts[1].isBlank()) return@getOrPut Pair(minAmount, minAmount)

            val maxAmount = amounts[1].toDoubleOrNull()?.let { max(minAmount, min(maxValue, it)) } ?: run {
                log.severe("Max value '${amounts[1]}' provided for entry '${key}' is not a double, please fix the typo and reload the configurations. Defaulting '${key}' max value to its minimum amount, which is $minAmount.")
                minAmount
            }
            Pair(minAmount, maxAmount)
        } as Pair<Double, Double>
    }

    fun getDoublePair(key: AbilityConfigKeys, default: Double = key.defaultValue as Double, minValue: Double = 0.0, maxValue: Double = Double.MAX_VALUE)
        = getDoublePair(key.configEntry, default, minValue, maxValue)
}

enum class AbilityConfigKeys(val configEntry: String, val defaultValue: Any) {
    TOSSER_SNEAK_MULTIPLIER_PERCENTAGE("${Ability.TOSSER.configEntry}.sneaking-multiplier-percentage", 0.4),
    INVISIBLE_DISABLE_ENTITY_SOUNDS("${Ability.INVISIBLE.configEntry}.disable-entity-sounds", true),
    INVISIBLE_DISABLE_EQUIPMENT_VISIBLITY("${Ability.INVISIBLE.configEntry}.disable-equipment-visibility", true),
    INVISIBLE_DISABLE_INFERNAL_PARTICLES("${Ability.INVISIBLE.configEntry}.disable-infernal-particles", true),
    FIREWORK_DAMAGE_MULTIPLIER("${Ability.FIREWORK.configEntry}.damage-multiplier", 1.0),
    LIFESTEAL_HEALING_PERCENTAGE("${Ability.LIFESTEAL.configEntry}.healing-percentage", 0.5),
    THORMAIL_REFLECTED_AMOUNT("${Ability.THORNMAIL.configEntry}.reflected-amount", 0.5),
    BERSERK_CAUSED_DAMAGE_BONUS("${Ability.BERSERK.configEntry}.damage-caused-bonus", 1.3),
    BERSERK_RECEIVED_DAMAGE_BONUS("${Ability.BERSERK.configEntry}.damage-received-bonus", 1.25),
    GHOST_EVIL_CHANCE("${Ability.GHOST.configEntry}.evil-chance", 0.3),
    GHOST_ITEM_DROP_CHANCE("${Ability.GHOST.configEntry}.item-drop-chance", 0.2),
    MORPH_KEEP_HP_PERCENTAGE("${Ability.MORPH.configEntry}.keep-hp-percentage", true),
    ARCHER_ARROW_AMOUNT("${Ability.ARCHER.configEntry}.arrow-amount", 10),
    ARCHER_ARROW_DELAY("${Ability.ARCHER.configEntry}.arrow-delay", 0.2),
    SPEEDY_BONUS("${Ability.SPEEDY.configEntry}.bonus", 1.5),
    ARMOURED_ARMOR_DROP_CHANCE("${Ability.ARMOURED.configEntry}.armor-drop-chance", 0.001),
    ARMOURED_POTION_LEVEL("${Ability.ARMOURED.configEntry}.fallback-dmg-resist-potion-level", 1),
    HEAVY_RESIST_PERCENTAGE("${Ability.HEAVY.configEntry}.knockback-resist-percentage", 0.4),
    WEBBER_TRAP_DENSITY("${Ability.WEBBER.configEntry}.trap-density", 0.6),
    WEBBER_MAX_RADIUS("${Ability.WEBBER.configEntry}.max-radius", 1),
    KAMIZAZE_EXPLOSION_POWER("${Ability.KAMIKAZE.configEntry}.explosion-power", 3.0),
    KAMIZAZE_SET_ON_FIRE("${Ability.KAMIKAZE.configEntry}.set-on-fire", true),
    KAMIZAZE_BREAK_BLOCKS("${Ability.KAMIKAZE.configEntry}.break-blocks", true),
}

