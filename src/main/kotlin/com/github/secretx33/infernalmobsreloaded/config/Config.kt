package com.github.secretx33.infernalmobsreloaded.config

import com.github.secretx33.infernalmobsreloaded.utils.YamlManager
import com.google.common.base.Defaults.defaultValue
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.plugin.Plugin
import org.koin.core.component.KoinApiExtension
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger
import kotlin.collections.HashSet
import java.lang.Enum as JavaEnum


@KoinApiExtension
class Config(plugin: Plugin, private val logger: Logger) {

    private val manager = YamlManager(plugin, "config")
    private val cache = ConcurrentHashMap<String, Any>()

    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: String, default: T): T {
        return cache.getOrPut(key) {
            manager.get(key, default)
        } as? T ?: run {
            logger.severe("On config key $key, expected value of type ${default!!::class.java.simpleName} but got ${manager.get(key)?.javaClass?.simpleName} instead, place fix your configuration file and reload")
            default
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: ConfigKeys): T = get(key.configEntry, key.defaultValue) as T

    fun <T> get(key: ConfigKeys, default: T): T = get(key.configEntry, default)

    @Suppress("UNCHECKED_CAST")
    fun <T : Enum<T>> getEnum(key: ConfigKeys): Enum<T> {
        return cache.getOrPut(key.configEntry) {
            manager.getString(key.configEntry)?.let {
                try {
                    JavaEnum.valueOf((key.defaultValue as Enum<*>)::class.java, it.toUpperCase(Locale.US))
                } catch(e: IllegalArgumentException) {
                    logger.severe("Error while trying to get config key '$key', value passed ${it.toUpperCase(Locale.US)} is an invalid value, please fix this entry in the config.yml and reload the configs, defaulting to ${(key.defaultValue as Enum<T>).name}")
                }
            } ?: key.defaultValue
        } as Enum<T>
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getEnumSet(key: ConfigKeys): Set<T> {
        return cache.getOrPut(key.configEntry) {
            if(!manager.contains(key.configEntry)) return@getOrPut key.defaultValue
            manager.getStringList(key.configEntry).mapNotNullTo(HashSet()) {
                try {
                    JavaEnum.valueOf((key.defaultValue as Enum<*>)::class.java, it.toUpperCase(Locale.US))
                } catch(e: IllegalArgumentException) {
                    logger.severe("Error while trying to get config key '$key', value passed ${it.toUpperCase(Locale.US)} is an invalid value, please fix this entry in the config.yml and reload the configs")
                    null
                }
            }
        } as Set<T>
    }

    fun has(path: String): Boolean = manager.contains(path)

    fun set(key: String, value: Any) {
        cache[key] = value
        manager.set(key, value)
    }

    fun set(key: ConfigKeys, value: Any) = set(key.configEntry, value)

    fun reload() {
        cache.clear()
        manager.reload()
    }

    fun save() = manager.save()
}

enum class ConfigKeys(val configEntry: String, val defaultValue: Any) {
    INFERNAL_SPAWN_REASONS("spawn-reasons-which-infernal-mobs-can-spawn", listOf(CreatureSpawnEvent.SpawnReason.NATURAL)),
    HARVEST_BONUS_SPEED("harvest.bonus_speed", emptyList<Int>()),
    HARVEST_MAX_WALK_DISTANCE("harvest.max-walk-distance", 0.4),
    PLAYERS_ONLY_SEE_WANDS_WITH_PERMISSION("players-only-see-dropped-wands-with-permission", true),
    REMOVE_HARVEST_BLOCK_WORLD_MISSING("remove-harvest-block-if-missing-world", false),
}
