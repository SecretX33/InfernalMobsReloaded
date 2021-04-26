package com.github.secretx33.infernalmobsreloaded.config

import com.github.secretx33.infernalmobsreloaded.utils.YamlManager
import org.bukkit.plugin.Plugin
import org.koin.core.component.KoinApiExtension
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger

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
    HARVEST_BAR_QUANTITY("harvest.bar-amount", 10),
    HARVEST_BONUS_SPEED("harvest.bonus_speed", emptyList<Int>()),
    HARVEST_MAX_WALK_DISTANCE("harvest.max-walk-distance", 0.4),
    PLAYERS_ONLY_SEE_WANDS_WITH_PERMISSION("players-only-see-dropped-wands-with-permission", true),
    REMOVE_HARVEST_BLOCK_WORLD_MISSING("remove-harvest-block-if-missing-world", false),
}
