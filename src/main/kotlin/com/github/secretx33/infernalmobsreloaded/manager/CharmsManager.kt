package com.github.secretx33.infernalmobsreloaded.manager

import com.github.secretx33.infernalmobsreloaded.eventbus.EventBus
import com.github.secretx33.infernalmobsreloaded.eventbus.internalevent.PluginLoad
import com.github.secretx33.infernalmobsreloaded.eventbus.internalevent.PluginReloaded
import com.github.secretx33.infernalmobsreloaded.eventbus.internalevent.PluginUnload
import com.github.secretx33.infernalmobsreloaded.model.CharmEffect
import com.github.secretx33.infernalmobsreloaded.model.CharmParticleMode
import com.github.secretx33.infernalmobsreloaded.model.PotionEffectApplyMode
import com.github.secretx33.infernalmobsreloaded.repository.CharmsRepo
import com.github.secretx33.infernalmobsreloaded.repository.LootItemsRepo
import com.github.secretx33.infernalmobsreloaded.util.extension.isAir
import com.github.secretx33.infernalmobsreloaded.util.extension.isInvisibleOrVanished
import com.github.secretx33.infernalmobsreloaded.util.extension.runSync
import com.google.common.cache.CacheBuilder
import com.google.common.collect.HashBasedTable
import com.google.common.collect.MultimapBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import toothpick.InjectConstructor
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.logging.Logger
import javax.inject.Singleton

@Singleton
@InjectConstructor
class CharmsManager(
    private val plugin: Plugin,
    private val charmsRepo: CharmsRepo,
    private val lootItemsRepo: LootItemsRepo,
    private val log: Logger,
    private val coroutineScope: CoroutineScope,
    eventBus: EventBus,
) {

    init {
        eventBus.subscribe<PluginLoad>(this, 20) { startAllCharmTasks() }
        eventBus.subscribe<PluginUnload>(this, 20) { stopAllCharmTasks() }
        eventBus.subscribe<PluginReloaded>(this, 20) { reload() }
    }

    private val permanentEffects = HashBasedTable.create<UUID, CharmEffect, PotionEffectType>()            // PlayerUuid, charmEffect, PotionEffect
    private val periodicEffects  = HashBasedTable.create<UUID, CharmEffect, Job>()                         // PlayerUuid, charmEffect, Coroutine
    private val targetEffects = MultimapBuilder.hashKeys().hashSetValues().build<UUID, CharmEffect>()      // PlayerUuid, charmEffect
    private var cooldowns = makeCooldownsCache()

    private fun makeCooldownsCache() = CacheBuilder.newBuilder()
        .expireAfterWrite((charmsRepo.getHighestEffectDelay() * 1000.0).toLong(), TimeUnit.MILLISECONDS)
        .build<Pair<UUID, CharmEffect>, Long>()

    fun areCharmsAllowedOnWorld(world: World): Boolean = charmsRepo.areCharmsAllowedOnWorld(world)

    private fun Player.isOnCharmEnabledWorld(): Boolean = areCharmsAllowedOnWorld(world)

    fun updateCharmEffects(player: Player) {
        // player is not on a charm effects enabled world
        if (!player.isOnCharmEnabledWorld()) {
            cancelAllCharmTasks(player)
            return
        }

        val invMap = player.inventoryMap
        val charms = invMap.filterKeys(charmsRepo::isItemRequiredByCharmEffect)
        // if player has no charms in his inventory
        if (charms.isEmpty()) {
            cancelAllCharmTasks(player)
            return
        }
        // owned loot items, because charms may require loot items to work
        val lootItems = invMap.filterKeys(lootItemsRepo::isLootItem).mapKeys { lootItemsRepo.getLootItemTag(it.key) } // TODO("maybe change invMap to charms again")
        val mainHand = player.inventory.itemInMainHand.let(lootItemsRepo::getLootItemTagOrNull)

        // charmEffects present in all loot items in player's inventory
        val effects = charms.flatMap { charmsRepo.getCharmEffects(it.key) }.filter { it.requiredItems.isNotEmpty() } + player.getActiveCharms()

        // start valid effects and cancel invalid effects
        effects.forEach {
            if (it.isEffectApplicableForInventory(lootItems, mainHand)) {
                player.startCharmEffect(it)
            } else {
                player.cancelCharmEffect(it)
            }
        }
//        println("2. lootItems = ${lootItems.keys.joinToString()}, effects = ${effects.joinToString(separator = ",\n")}")
    }

    private fun Player.getActiveCharms(): Set<CharmEffect> =
        permanentEffects.row(uniqueId).keys + periodicEffects.row(uniqueId).keys + targetEffects.get(uniqueId)

    private fun Player.startCharmEffect(charmEffect: CharmEffect) {
//        println("2. Starting effect of charm '${charmEffect.name}' -> $charmEffect'")
        when (charmEffect.effectApplyMode) {
            // permanent buffs, like speed
            PotionEffectApplyMode.SELF_PERMANENT -> addPermanentCharmEffect(charmEffect)
            // recurrent effects, like healing
            PotionEffectApplyMode.SELF_RECURRENT -> addRecurrentCharmEffect(charmEffect)
            // on hit effects, like instant damage, only applied on target
            PotionEffectApplyMode.SELF_ON_HIT,
            PotionEffectApplyMode.TARGET_ON_HIT,
            PotionEffectApplyMode.BOTH_ON_HIT -> targetEffects.put(uniqueId, charmEffect)
        }
    }

    private fun Player.addPermanentCharmEffect(charmEffect: CharmEffect) {
        // if effect is already applied
        if (permanentEffects.contains(uniqueId, charmEffect)) return

        addPotionEffect(PotionEffect(charmEffect.potionEffect, Int.MAX_VALUE, charmEffect.getPotency()))
        spawnCharmParticles(charmEffect)
        permanentEffects.put(uniqueId, charmEffect, charmEffect.potionEffect)
        charmEffect.playerMessage?.let(::sendMessage)
    }

    private fun Player.addRecurrentCharmEffect(charmEffect: CharmEffect) {
        // if effect task is already running
        if (periodicEffects.contains(uniqueId, charmEffect)) return

        val job = coroutineScope.launch {
            delay((charmEffect.getDelay() * 1000.0).toLong())

            while (isActive && isValid && !isDead) {
                runSync(plugin) { addPotionEffect(PotionEffect(charmEffect.potionEffect, (charmEffect.getDuration() * 20.0).toInt(), charmEffect.getPotency())) }
                spawnCharmParticles(charmEffect)
                charmEffect.playerMessage?.let(::sendMessage)
                delay((charmEffect.getDelay() * 1000.0).toLong())
            }
        }
        periodicEffects.put(uniqueId, charmEffect, job)
    }

    private fun LivingEntity.spawnCharmParticles(charmEffect: CharmEffect) {
        if (this is Player && isInvisibleOrVanished()) return

        val particle = charmEffect.particle?.takeIf { charmEffect.particleMode != CharmParticleMode.NONE }
            ?: return
        world.spawnParticle(particle, eyeLocation, 100, 0.5, 1.0, 0.5)
    }

    fun triggerOnHitCharms(player: Player, target: LivingEntity) {
//        println("Triggering on hit effects")
        targetEffects.get(player.uniqueId).filter { it.isNotCooldown(player) }.forEach {
//            println("Triggering ${it.name} on ${target.name}")
            // effects
            if (it.effectApplyMode == PotionEffectApplyMode.SELF_ON_HIT || it.effectApplyMode == PotionEffectApplyMode.BOTH_ON_HIT)
                player.addPotionEffect(PotionEffect(it.potionEffect, (it.getDuration() * 20.0).toInt(), it.getPotency()))

            if (it.effectApplyMode == PotionEffectApplyMode.TARGET_ON_HIT || it.effectApplyMode == PotionEffectApplyMode.BOTH_ON_HIT)
                target.addPotionEffect(PotionEffect(it.potionEffect, (it.getDuration() * 20.0).toInt(), it.getPotency()))

            // particles
            if (it.enabledSelfParticle) player.spawnCharmParticles(it)
            if (it.enabledTargetParticle) target.spawnCharmParticles(it)

            // messages
            it.playerMessage?.let(player::sendMessage)
            if (target is Player) it.targetMessage?.let(target::sendMessage)
        }
    }

    private fun CharmEffect.isNotCooldown(player: Player) = cooldowns.getIfPresent(Pair(player.uniqueId, this))
        .let { it == null || it < System.currentTimeMillis() }.also { if (it) cooldowns.put(Pair(player.uniqueId, this), System.currentTimeMillis() + (getDelay() * 1000.0).toLong()) }

    private fun Player.cancelCharmEffect(charmEffect: CharmEffect) {
        when (charmEffect.effectApplyMode) {
            PotionEffectApplyMode.SELF_PERMANENT -> permanentEffects.remove(uniqueId, charmEffect)?.let(::removePotionEffect)
            PotionEffectApplyMode.SELF_RECURRENT -> periodicEffects.remove(uniqueId, charmEffect)?.cancel()
            PotionEffectApplyMode.SELF_ON_HIT,
            PotionEffectApplyMode.TARGET_ON_HIT,
            PotionEffectApplyMode.BOTH_ON_HIT -> targetEffects.remove(uniqueId, charmEffect)
        }
    }

    private val Player.inventoryMap: Map<ItemStack, Int>
        get() = inventory.withIndex()
            .filter { it.value != null && !it.value.isAir() }
            .associateTo(HashMap()) { it.value to it.index }

    fun cancelAllCharmTasks(player: Player) {
        permanentEffects.row(player.uniqueId).values.forEach(player::removePotionEffect)
        permanentEffects.rowKeySet().remove(player.uniqueId)
        periodicEffects.row(player.uniqueId).values.forEach { it.cancel() }
        periodicEffects.rowKeySet().remove(player.uniqueId)
        targetEffects.removeAll(player.uniqueId)
    }

    private fun reload() {
        stopAllCharmTasks()
        cooldowns = makeCooldownsCache().apply { putAll(cooldowns.asMap()) }
        startAllCharmTasks()
    }

    private fun stopAllCharmTasks() {
        log.info("Disabling player charms")
        Bukkit.getOnlinePlayers().forEach(::cancelAllCharmTasks)
        // most probably not needed, just as precaution
        permanentEffects.clear()
        periodicEffects.clear()
        targetEffects.clear()
    }

    private fun startAllCharmTasks() {
        log.info("Enabling player charms")
        Bukkit.getOnlinePlayers().forEach(::updateCharmEffects)
    }
}
