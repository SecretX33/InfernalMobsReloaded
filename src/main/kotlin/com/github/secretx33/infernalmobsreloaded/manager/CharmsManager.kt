package com.github.secretx33.infernalmobsreloaded.manager

import com.github.secretx33.infernalmobsreloaded.model.CharmEffect
import com.github.secretx33.infernalmobsreloaded.model.CharmParticleMode
import com.github.secretx33.infernalmobsreloaded.model.PotionEffectApplyMode
import com.github.secretx33.infernalmobsreloaded.repositories.CharmsRepo
import com.github.secretx33.infernalmobsreloaded.utils.isAir
import com.google.common.cache.CacheBuilder
import com.google.common.collect.HashBasedTable
import com.google.common.collect.MultimapBuilder
import kotlinx.coroutines.*
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.koin.core.component.KoinApiExtension
import java.util.*
import java.util.concurrent.TimeUnit

@KoinApiExtension
class CharmsManager(private val charmsRepo: CharmsRepo) {

    private val permanentEffects = HashBasedTable.create<UUID, String, PotionEffectType>()                 // PlayerUuid, charmEffect, PotionEffect
    private val periodicEffects  = HashBasedTable.create<UUID, String, Job>()                              // PlayerUuid, charmEffect, Coroutine
    private val targetEffects    = MultimapBuilder.hashKeys().hashSetValues().build<UUID, CharmEffect>()   // PlayerUuid, charmEffect, CharmEffect
    private var cache = CacheBuilder.newBuilder().expireAfterWrite(12345L, TimeUnit.MILLISECONDS).build<Pair<UUID, String>, Long>()

    fun updateEffects(player: Player) {
        val charms = player.inventoryMap.filterKeys { charmsRepo.isCharm(it) }
        // if player has no charms in his inventory
        if(charms.isEmpty()) {
            cancelAllCharmTasks(player)
            return
        }
        // owned loot items, because charms may require loot items to work
        val lootItems = charms.mapKeys { (charm, _) -> charmsRepo.getLootItemTag(charm) }
        // charmEffects present in all loot items in player's inventory
        val effects = charms.flatMap { charmsRepo.getCharmEffects(it.key) }.filter { it.requiredItems.isNotEmpty() }

        // start valid effects and cancel invalid effects
        effects.forEach {
            if(it.validateEffect(lootItems)) player.startCharmEffect(it)
            else player.cancelCharmEffect(it)
        }

        println("2. lootItems = ${lootItems.keys.joinToString()}, effects = ${effects.joinToString(separator = ",\n")}")

        /*for((item, slot) in charms) {
            val effects = charmsRepo.getCharmEffects(item)

            for(effect in effects) {
                // item is not in the right slot
                if(slot !in effect.requiredSlots){
                    player.cancelEffect(effect)
                    continue
                }
                // set is not fully equipped
                if(!effect.validateEffect(lootItems)) {
                    player.cancelEffect(effect)
                    continue
                }

                // if effect is not running yet, start it
                if(effect.isPermanent && (!permanentEffects.contains(player.uniqueId, effect.name) || permanentEffects.get(player.uniqueId, effect) == false)
                    || effect.isRecurrent && periodicEffects.contains(player.uniqueId, effect.name)) continue

                player.startCharmEffect(effect)
            }
        }*/
    }

    private fun Player.startCharmEffect(charmEffect: CharmEffect) {
        println("1. Starting effect of charm '${charmEffect.name}' -> $charmEffect'")
        when(charmEffect.effectApplyMode) {
            // permanent buffs, like speed
            PotionEffectApplyMode.SELF_PERMANENT -> addPermanentCharmEffect(charmEffect)
            // recurrent effects, like healing
            PotionEffectApplyMode.SELF_RECURRENT -> addRecurrentCharmEffect(charmEffect)
            // on hit effects, like instant damage, only applied on target
            PotionEffectApplyMode.TARGET_TEMPORARY -> {
                targetEffects.put(uniqueId, charmEffect)
            }
        }
    }

    private fun Player.addPermanentCharmEffect(charmEffect: CharmEffect) {
        // if effect is already applied
        if(permanentEffects.contains(uniqueId, charmEffect.name)) return
        addPotionEffect(PotionEffect(charmEffect.potionEffect, Int.MAX_VALUE, charmEffect.getPotency()))
        spawnCharmParticles(charmEffect)
        permanentEffects.put(uniqueId, charmEffect.name, charmEffect.potionEffect)
    }

    private fun Player.addRecurrentCharmEffect(charmEffect: CharmEffect) {
        val job = CoroutineScope(Dispatchers.Default).launch {
            delay((charmEffect.getDelay() * 1000).toLong())
            addPotionEffect(PotionEffect(charmEffect.potionEffect, (charmEffect.getDuration() * 20.0).toInt(), charmEffect.getPotency()))
            spawnCharmParticles(charmEffect)
        }
        periodicEffects.put(uniqueId, charmEffect.name, job)
    }

    private fun Player.spawnCharmParticles(charmEffect: CharmEffect) {
        val particle = charmEffect.particle?.takeIf { charmEffect.particleMode != CharmParticleMode.NONE } ?: return
        world.spawnParticle(particle, eyeLocation, 100, 0.5, 1.0, 0.5)
    }

    private fun Player.cancelCharmEffect(charmEffect: CharmEffect) {
        println("1. Canceling effect of charm '${charmEffect.name}' -> $charmEffect'")
        when(charmEffect.effectApplyMode) {
            PotionEffectApplyMode.SELF_PERMANENT -> {
                permanentEffects.remove(uniqueId, charmEffect.name)?.let { removePotionEffect(it) }
            }
            PotionEffectApplyMode.SELF_RECURRENT -> periodicEffects.remove(uniqueId, charmEffect)?.cancel()
            PotionEffectApplyMode.TARGET_TEMPORARY -> targetEffects.remove(uniqueId, charmEffect)
        }
    }

    private val Player.inventoryMap: Map<ItemStack, Int>
        get() {
            val inventoryMap = HashMap<ItemStack, Int>()
            inventory.iterator().withIndex().forEachRemaining { (index, item: ItemStack?) ->
                item?.let { inventoryMap[item] = index }
            }
            return inventoryMap.filter { !it.key.isAir() }
        }

    fun cancelAllCharmTasks(player: Player) {
        permanentEffects.row(player.uniqueId).values.forEach { player.removePotionEffect(it) }
        permanentEffects.rowKeySet().remove(player.uniqueId)
        periodicEffects.row(player.uniqueId).values.forEach { it.cancel() }
        periodicEffects.rowKeySet().remove(player.uniqueId)
        targetEffects.removeAll(player.uniqueId)
    }
}
