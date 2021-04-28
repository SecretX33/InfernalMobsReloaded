package com.github.secretx33.infernalmobsreloaded.config

import com.github.secretx33.infernalmobsreloaded.model.Abilities
import com.github.secretx33.infernalmobsreloaded.utils.YamlManager
import me.mattstudios.msg.adventure.AdventureMessage
import org.bukkit.plugin.Plugin
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger

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
            log.severe("On config key $key, expected value of type ${default!!::class.java.simpleName} but got ${manager.get(key)?.javaClass?.simpleName} instead, please fix your ${manager.fileName} file and reload")
            default
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: AbilityConfigKeys): T = get(key.configEntry, key.defaultValue) as T

    fun <T> get(key: AbilityConfigKeys, default: T): T = get(key.configEntry, default)

    fun getPotionIsAmbient(ability: Abilities) = get("${ability.configEntry}.is-ambient", true)

    fun getPotionEmitParticles(ability: Abilities) = get("${ability.configEntry}.emit-particles", true)

    fun reload() {
        manager.reload()
    }
}

enum class AbilityConfigKeys(val configEntry: String, val defaultValue: Any) {
    SPEEDY_MIN_BONUS("speedy.min-bonus", 1.5),
    SPEEDY_MAX_BONUS("speedy.max-bonus", 2.5),
    ARMOURED_POTION_LEVEL("armoured.fallback-dmg-resist-potion-level", 1),
    HEAVY_RESIST_PERCENTAGE("heavy.knockback-resist-percentage", 0.4),
}

