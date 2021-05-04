package com.github.secretx33.infernalmobsreloaded.config

import com.github.secretx33.infernalmobsreloaded.model.DisplayCustomNameMode
import com.github.secretx33.infernalmobsreloaded.utils.YamlManager
import com.google.common.base.Enums
import com.google.common.base.Predicate
import org.bukkit.Particle
import org.bukkit.entity.EntityType
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.plugin.Plugin
import org.koin.core.component.KoinApiExtension
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger
import kotlin.Enum
import kotlin.math.max
import kotlin.math.min
import java.lang.Enum as JavaEnum

@KoinApiExtension
class Config(plugin: Plugin, private val log: Logger) {

    private val manager = YamlManager(plugin, "config")
    private val cache = ConcurrentHashMap<String, Any>()

    fun reload() {
        cache.clear()
        manager.reload()
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: String, default: T): T {
        return cache.getOrPut(key) {
            manager.get(key, default) as? T ?: run {
                log.severe("On config key $key, expected value of type ${default!!::class.java.simpleName} but got ${manager.get(key)?.javaClass?.simpleName} instead, please fix your configuration file and reload")
                default
            }
        } as T
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: ConfigKeys): T = get(key.configEntry, key.defaultValue) as T

    fun <T> get(key: ConfigKeys, default: T): T = get(key.configEntry, default)

    fun getInt(key: String, default: Int, minValue: Int = 0, maxValue: Int = Int.MAX_VALUE): Int {
        return cache.getOrPut(key) {
            (manager.get(key, default) as? Int)?.let { int -> max(minValue, min(maxValue, int)) } ?: run {
                log.severe("On config entry $key, expected value of type Int but got ${manager.get(key)?.javaClass?.simpleName} instead, please fix your ${manager.fileName} file and reload")
                default
            }
        } as Int
    }

    fun getInt(key: ConfigKeys, default: Int = key.defaultValue as Int, minValue: Int = 0, maxValue: Int = Int.MAX_VALUE)
        = getInt(key.configEntry, default, minValue, maxValue)

    fun getDouble(key: String, default: Double, minValue: Double = 0.0, maxValue: Double = Double.MAX_VALUE): Double {
        return cache.getOrPut(key) {
            (manager.get(key, default) as? Double)?.let { double -> max(minValue, min(maxValue, double)) } ?: run {
                log.severe("On config entry $key, expected value of type Double but got ${manager.get(key)?.javaClass?.simpleName} instead, please fix your ${manager.fileName} file and reload")
                default
            }
        } as Double
    }

    fun getDouble(key: ConfigKeys, default: Double = key.defaultValue as Double, minValue: Double = 0.0, maxValue: Double = Double.MAX_VALUE)
        = getDouble(key.configEntry, default, minValue, maxValue)

    @Suppress("UNCHECKED_CAST")
    fun <T : Enum<T>> getEnum(key: ConfigKeys): T {
        return cache.getOrPut(key.configEntry) {
            manager.getString(key.configEntry)?.let {
                try {
                    JavaEnum.valueOf(key.defaultValue::class.java as Class<out Enum<T>>, it.toUpperCase(Locale.US))
                } catch(e: IllegalArgumentException) {
                    log.severe("Error while trying to get config key '$key', value passed ${it.toUpperCase(Locale.US)} is an invalid value, please fix this entry in the config.yml and reload the configs, defaulting to ${(key.defaultValue as Enum<T>).name}")
                }
            } ?: key.defaultValue
        } as T
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Enum<T>> getEnumSet(key: ConfigKeys, clazz: Class<out Enum<T>>, predicate: Predicate<T>? = null): Set<T> {
        return cache.getOrPut(key.configEntry) {
            if(!manager.contains(key.configEntry)) return@getOrPut key.defaultValue
            manager.getStringList(key.configEntry).mapNotNullTo(HashSet()) { item ->
                val optional = Enums.getIfPresent(clazz, item.toUpperCase(Locale.US)).takeIf { opt -> opt.isPresent } ?: run {
                    log.severe("Error while trying to get config key '$key', value passed '${item.toUpperCase(Locale.US)}' is an invalid value, please fix this entry in the ${manager.fileName} and reload the configs")
                    return@mapNotNullTo null
                }
                optional.get().takeIf { it != null && (predicate == null || predicate.apply(it as T)) }
            }
        } as Set<T>
    }

    fun has(path: String): Boolean = manager.contains(path)

    fun set(key: String, value: Any) {
        cache[key] = value
        manager.set(key, value)
    }

    fun set(key: ConfigKeys, value: Any) = set(key.configEntry, value)

    fun save() = manager.save()
}

enum class ConfigKeys(val configEntry: String, val defaultValue: Any) {
    INFERNAL_BOSS_BAR_REQUIRE_LOS("boss-bar-require-los", true),
    MOB_TYPES_THAT_CAN_WEAR_ARMOR("mob-types-that-can-wear-armor", emptySet<EntityType>()),
    INFERNALS_CANNOT_DAMAGE_THEMSELVES("infernals-cannot-damage-themselves", true),
    BOSS_BAR_SHOW_RANGE("boss-bar-show-range", 25.0),
    DELAY_BETWEEN_INFERNAL_PARTICLES("delay-between-infernal-particles", 1.5),
    DISPLAY_INFERNAL_NAME_MODE("display-infernal-custom-name-mode", DisplayCustomNameMode.LOOKING_AT),
    ENABLE_BOSS_BARS("enable-boss-bars", true),
    ENABLE_INFERNAL_DEATH_MESSAGE("enable-infernal-death-messages", false),
    ENABLE_INFERNAL_SPAWN_MESSAGE("enable-infernal-spawn-messages", false),
    ENABLE_INFERNAL_PARTICLES("enable-infernal-particles", true),
    ENABLE_GLOBAL_PARTICLE_EFFECTS("enable-particle-effects", true),
    INFERNAL_ALLOWED_SPAWN_REASONS("spawn-reasons-which-infernal-mobs-can-spawn", setOf(CreatureSpawnEvent.SpawnReason.NATURAL)),
    INFERNAL_ALLOWED_WORLDS("worlds-in-which-infernal-mobs-can-spawn", emptyList<String>()),
    INFERNAL_BLACKLISTED_BABY_MOBS("blacklisted-baby-mob-types", emptySet<EntityType>()),
    INFERNAL_DEATH_MESSAGE_RADIUS("infernal-death-message-radius", 20),
    MOBS_THAT_CAN_BE_RIDED_BY_MOUNTED_INFERNALS("mobs-that-can-be-rided-by-mounted-infernals", emptySet<EntityType>()),
    INFERNAL_MOBS_THAT_CAN_SPAWN_MOUNTED("infernal-mobs-that-can-spawn-mounted", emptySet<EntityType>()),
    INFERNAL_PARTICLE_TYPE("infernal-particle-type", Particle.LAVA),
    INFERNAL_PARTICLES_AMOUNT("infernal-particles-amount", 10),
    INFERNAL_PARTICLES_SPREAD("infernal-particles-spread", 2.0),
    INFERNAL_SPAWN_MESSAGE_RADIUS("infernal-spawn-message-radius", 30),
}
