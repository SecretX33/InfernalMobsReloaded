package com.github.secretx33.infernalmobsreloaded.repositories

import com.cryptomorin.xseries.XPotion
import com.github.secretx33.infernalmobsreloaded.annotations.InjectSingleton
import com.github.secretx33.infernalmobsreloaded.model.CharmEffect
import com.github.secretx33.infernalmobsreloaded.model.CharmParticleMode
import com.github.secretx33.infernalmobsreloaded.model.KeyChain
import com.github.secretx33.infernalmobsreloaded.model.PotionEffectApplyMode
import com.github.secretx33.infernalmobsreloaded.utils.extension.pdc
import com.github.secretx33.infernalmobsreloaded.utils.extension.matchOrNull
import com.github.secretx33.infernalmobsreloaded.utils.other.YamlManager
import com.google.common.collect.ImmutableSetMultimap
import me.mattstudios.msg.adventure.AdventureMessage
import net.kyori.adventure.text.Component
import org.bukkit.Particle
import org.bukkit.World
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin
import org.bukkit.potion.PotionEffectType
import toothpick.InjectConstructor
import java.util.Locale
import java.util.logging.Logger
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

@InjectSingleton
class CharmsRepo (
    plugin: Plugin,
    private val log: Logger,
    private val adventureMessage: AdventureMessage,
    private val lootItemsRepo: LootItemsRepo,
    private val keyChain: KeyChain,
) {
    private val manager = YamlManager(plugin, "charms")
    private var charmsCache = ImmutableSetMultimap.of<String, CharmEffect>()    // lowercase lootItemName, charmEffect
    private var worldWhitelist = emptySet<String>()

    init { reload() }

    fun reload() {
        manager.reload()
        loadCharmEffects()
        loadWorldWhitelist()
    }

    fun areCharmsAllowedOnWorld(world: World): Boolean = worldWhitelist.contains("<all>") || worldWhitelist.contains(world.name.lowercase(Locale.US))

    fun getCharmEffectsOrNull(name: String): Set<CharmEffect>? = charmsCache[name.lowercase(Locale.US)]

    fun getCharmEffects(lootItemName: String): Set<CharmEffect> = getCharmEffectsOrNull(lootItemName) ?: throw IllegalStateException("Tried to get $lootItemName charm effect but there's none")

    fun getCharmEffects(item: ItemStack): Set<CharmEffect> = getCharmEffects(lootItemsRepo.getLootItemTag(item))

    // effectively, this checks if the item is a charm
    fun isItemRequiredByCharmEffect(item: ItemStack, acceptBroken: Boolean = false): Boolean {
        val pdc = item.itemMeta?.pdc ?: return false
        pdc.apply {
            val name = get(keyChain.infernalItemNameKey, PersistentDataType.STRING) ?: return false
            return (acceptBroken || !has(keyChain.brokenCharmKey, PersistentDataType.SHORT)) && charmsCache.containsKey(name)
        }
    }

    fun getHighestEffectDelay(): Double = charmsCache.values().maxOfOrNull { it.getMaxDelay() } ?: 0.0

    private fun loadCharmEffects() {
        val keys = manager.getConfigurationSection("charm-effects")?.getKeys(false) ?: throw IllegalStateException("missing charms section charm-effects")
        val charmEffects = keys.mapNotNull { makeCharmEffect(it.lowercase(Locale.US)) }

        val builder = ImmutableSetMultimap.builder<String, CharmEffect>()
        charmEffects.forEach { effect ->
            effect.requiredItems.forEach { lootItem ->
                builder.put(lootItem, effect)
            }
        }
        charmsCache = builder.build()
    }

    private fun loadWorldWhitelist() {
        worldWhitelist = manager.getStringList("charm-effects-world-whitelist").mapTo(HashSet()) { it.lowercase(Locale.US) }
    }

    private fun makeCharmEffect(name: String): CharmEffect {
        val particle = getParticle(name, "particle-type")
        val mode = getEffectApplyMode(name, "effect-mode")

        return CharmEffect(name,
            playerMessage = getComponentMessage(name, "player-message"),
            targetMessage = getComponentMessage(name, "target-message"),
            potionEffect = getPotionEffect(name, "effect"),
            potency = getIntPair(name, "potency", minValue = 1),
            duration = getDoublePair(name, "duration", default = 0.05, minValue = 0.05),
            delay = getDelay(name, mode),
            particle = particle,
            effectApplyMode = mode,
            particleMode = getParticleMode(name, "particle-mode", particle, mode),
            requiredMainHand = getRequiredMainHand(name, "main-hand"),
            requiredItems = getRequiredItems(name, "required-items"),
            requiredSlots = getRequiredSlots(name),
        )
    }

    private fun getDelay(name: String, mode: PotionEffectApplyMode): Pair<Double, Double> {
        val delay = getDoublePair(name, "delay", default = 0.0)

        if(mode == PotionEffectApplyMode.SELF_RECURRENT && delay.first <= 0.1) {
            log.warning("Warning: You have set the minimum delay of '$name' charm effect to ${delay.first}s, which is less or equal to 100ms, this might overload or even crash your server and/or your players clients")
        }
        return delay
    }

    private fun getComponentMessage(name: String, key: String): Component? {
        val message = manager.getString("charm-effects.$name.$key") ?: ""
        if(message.isBlank()) return null
        return adventureMessage.parse(message)
    }

    private fun getPotionEffect(name: String, key: String): PotionEffectType {
        val potionEffect = manager.getString("charm-effects.$name.$key") ?: ""

        // if type is absent or blank
        if(potionEffect.isBlank()) {
            log.warning("You must provide a $key for the charm type '$name'! Please fix your charms configurations and reload, defaulting $name $key to Luck.")
            return PotionEffectType.LUCK
        }

        return XPotion.parsePotionEffectFromString(potionEffect)?.type ?: run {
            log.warning("Inside charm effect '$name', $key '$potionEffect' is invalid or doesn't exist, please fix your charms configurations. Defaulting $name $key to Luck")
            PotionEffectType.LUCK
        }
    }


    private fun getEffectApplyMode(name: String, key: String): PotionEffectApplyMode {
        val applyMode = manager.getString("charm-effects.$name.$key") ?: ""

        // if type is absent or blank
        if(applyMode.isBlank()) {
            log.warning("You must provide a $key for the charm type '$name'! Please fix your charms configurations and reload, defaulting $name $key to SELF_PERMANENT.")
            return PotionEffectApplyMode.SELF_PERMANENT
        }

        return PotionEffectApplyMode.values().firstOrNull { it.name.equals(applyMode, ignoreCase = true) } ?: run {
            log.warning("Inside charm effect '$name', $key '$applyMode' is invalid or doesn't exist, please fix your charms configurations. Defaulting $name $key to SELF_PERMANENT")
            PotionEffectApplyMode.SELF_PERMANENT
        }
    }

    private fun getParticle(name: String, key: String): Particle? {
        val particleEffect = manager.getString("charm-effects.$name.$key") ?: ""

        // if type is absent or blank
        if(particleEffect.isBlank()) return null

        return Particle.values().firstOrNull { it.name.equals(particleEffect, ignoreCase = true) } ?: run {
            log.warning("Inside charm effect '$name', $key '$particleEffect' is invalid or doesn't exist, please fix your charms configurations. Disablind particle effects for $name $key")
            null
        }
    }

    private fun getParticleMode(name: String, key: String, particle: Particle?, effectMode: PotionEffectApplyMode): CharmParticleMode {
        // no particle selected
        if(particle == null) return CharmParticleMode.NONE

        val typedParticleMode = manager.getString("charm-effects.$name.$key") ?: ""

        // if type is absent or blank
        if(typedParticleMode.isBlank()) {
            log.warning("You must provide a $key for the charm type '$name'! Please fix your charms configurations and reload, defaulting $name $key to NONE.")
            return CharmParticleMode.NONE
        }

        // particle is invalid/non existent
        val particleMode = CharmParticleMode.values().firstOrNull { it.name.equals(typedParticleMode, ignoreCase = true) } ?: run {
            log.warning("Inside charm effect '$name', $key '$typedParticleMode' is invalid or doesn't exist, please fix your charms configurations and reload. Defaulting $name $key to NONE.")
            return CharmParticleMode.NONE
        }

        // particleMode is incompatible with effectMode, warn and disable particles
        if(effectMode !in particleMode.validApplyModes) {
            log.warning("Inside charm effect '$name', $key '$typedParticleMode' cannot be used in conjunction with $effectMode, valid modes for $typedParticleMode are: '${particleMode.validApplyModes.joinToString()}, please fix your charms configurations and reload. Defaulting $name $key to NONE.")
            return CharmParticleMode.NONE
        }
        return particleMode
    }

    private fun getRequiredMainHand(name: String, key: String): String? {
        val item = manager.getString("charm-effects.$name.$key")?.takeIf { it.isNotBlank() } ?: return null

        if(!lootItemsRepo.hasLootItem(item)) {
            log.warning("Inside ${manager.fileName} key '$name.$key', loot item named '$item' was not found, please fix your charm configurations and reload.")
            return null
        }
        return lootItemsRepo.getLootItem(item).name
    }

    private fun getRequiredItems(name: String, key: String): Set<String> {
        val items = manager.getStringList("charm-effects.$name.$key")

        items.filter { !lootItemsRepo.hasLootItem(it) }.forEach {
            log.warning("Inside ${manager.fileName} key '$name.$key', loot item named '$it' was not found, please fix your charm configurations and reload.")
        }
        return items.filter { lootItemsRepo.hasLootItem(it) }.mapTo(HashSet()) { lootItemsRepo.getLootItem(it).name }
    }

    private fun getRequiredSlots(name: String): Set<Int> {
        val slots = manager.getStringList("charm-effects.$name.override-charm-slots").takeIf { it.isNotEmpty() }
            ?: manager.getStringList("valid-charm-slots")

        return slots.mapNotNull { range ->
            // value is only one value
            SIGNED_INT.matchOrNull(range, 1)
                ?.let { max(0, it.toInt()) }
                ?.let { return@mapNotNull setOf(it) }

            // value is a range of values
            SIGNED_INT_RANGE.matchEntire(range)?.groupValues
                ?.subList(1, 3)
                ?.map { max(0, it.toInt()) }
                ?.let { return@mapNotNull IntRange(it[0], it[1]).toSet() }

        }.flatten().toHashSet()
    }

    // returns a pair with the <Min, Max> amount of the key property
    private fun getIntPair(name: String, key: String, default: Int = 0, minValue: Int = 0, maxValue: Int = Int.MAX_VALUE): Pair<Int, Int> {
        val values = manager.getString("charm-effects.$name.$key") ?: ""

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
        log.warning("Inside ${manager.fileName} '$name', $key provided '$values' is not an integer nor an integer range, please fix your configurations and reload. Defaulting '$name' $key to $default.")
        return Pair(default, default)
    }

    // returns a pair with the <Min, Max> amount of the key property
    private fun getDoublePair(name: String, key: String, default: Double = 1.0, minValue: Double = 0.0, maxValue: Double = Double.MAX_VALUE): Pair<Double, Double> {
        val values = manager.getString("charm-effects.$name.$key") ?: ""

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
        log.warning("Inside ${manager.fileName} '$name', $key provided '$values' is not a double nor a double range, please fix your configurations and reload. Defaulting '$name' $key to $default.")
        return Pair(default, default)
    }

    private companion object {                                                             // regex matches examples
        val SIGNED_INT = """^\s*(-?\d{1,11})\s*$""".toRegex()                              // "-5"     (-5)
        val SIGNED_INT_RANGE = """^\s*(-?\d{1,11}?)\s*-\s*(-?\d{1,11})\s*$""".toRegex()    // "-5 - -1"  (-5 until -1)
        val SIGNED_DOUBLE = """^\s*(-?\d+?(?:\.\d+?)?)\s*$""".toRegex()                    // "-5.0"   (-5.0)
        val SIGNED_DOUBLE_RANGE = """^\s*(-?\d+?(?:\.\d+?)?)\s*-\s*(-?\d+?(?:\.\d+)?)\s*$""".toRegex()  // "-5.0 - -1.0"  (-5.0 until -1.0)
    }
}
