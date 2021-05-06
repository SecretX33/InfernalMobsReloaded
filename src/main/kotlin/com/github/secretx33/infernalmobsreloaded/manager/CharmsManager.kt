package com.github.secretx33.infernalmobsreloaded.manager

import com.github.secretx33.infernalmobsreloaded.model.CharmEffect
import com.github.secretx33.infernalmobsreloaded.model.CharmParticleMode
import com.github.secretx33.infernalmobsreloaded.model.PotionEffectApplyMode
import com.github.secretx33.infernalmobsreloaded.repositories.CharmsRepo
import com.github.secretx33.infernalmobsreloaded.utils.isAir
import com.google.common.cache.CacheBuilder
import com.google.common.collect.HashBasedTable
import kotlinx.coroutines.*
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.koin.core.component.KoinApiExtension
import java.util.*
import java.util.concurrent.TimeUnit

@KoinApiExtension
class CharmsManager(private val charmsRepo: CharmsRepo) {

    private val permanentEffects = HashBasedTable.create<UUID, String, Boolean>()  // PlayerUuid, , State
    private val periodicEffects  = HashBasedTable.create<UUID, String, Job>()      // PlayerUuid, , Coroutine
    private var cache = CacheBuilder.newBuilder().expireAfterWrite(12345L, TimeUnit.MILLISECONDS).build<Pair<UUID, String>, Long>()

    fun updateEffects(player: Player) {
        val charms = player.inventoryMap.filterValues { charmsRepo.isCharm(it) }
        // if player has no charms in his inventory
        if(charms.isEmpty()) {
            cancelAllCharmTasks(player)
            return
        }

        val lootItems = charms.mapValues { (_, charm) -> charmsRepo.getLootItemTag(charm) }

        for((slot, item) in charms) {
            val charmEffect = charmsRepo.getCharmEffect(item)

            // item is not in the right slot
            if(slot !in charmEffect.requiredSlots){
                player.cancelEffect(charmEffect)
                continue
            }
            // set is not fully equipped
            if(charmEffect.requiredItems.any { it !in lootItems.values }) {
                player.cancelEffect(charmEffect)
                continue
            }

            // if effect is not running yet, start it
            if(charmEffect.isPermanent && (!permanentEffects.contains(player.uniqueId, charmEffect.name) || permanentEffects.get(player.uniqueId, charmEffect) == false)
                || charmEffect.isRecurrent && periodicEffects.contains(player.uniqueId, charmEffect.name)) continue

            player.startCharmEffect(charmEffect)
        }
    }

    private fun Player.startCharmEffect(charmEffect: CharmEffect) {
        val particleMode = charmEffect.particleMode

        // permanent buffs, like speed
        if(charmEffect.isPermanent) {
            addPotionEffect(PotionEffect(charmEffect.potionEffect, Int.MAX_VALUE, charmEffect.getPotency()))
            spawnCharmParticles(charmEffect)
            permanentEffects.put(uniqueId, charmEffect.name, true)
            return
        }

        // recurrent effects, like healing
        if(charmEffect.isRecurrent) {
            val job = CoroutineScope(Dispatchers.Default).launch {
                delay((charmEffect.getDelay() * 1000).toLong())
                addPotionEffect(PotionEffect(charmEffect.potionEffect, (charmEffect.getDuration() * 20.0).toInt(), charmEffect.getPotency()))
                this@startCharmEffect.spawnCharmParticles(charmEffect)
            }
            periodicEffects.put(uniqueId, charmEffect.name, job)
        }
    }

    private fun Player.spawnCharmParticles(charmEffect: CharmEffect) {
        val particle = charmEffect.particle?.takeIf { charmEffect.particleMode != CharmParticleMode.NONE } ?: return
        world.spawnParticle(particle, eyeLocation, 100, 0.5, 1.0, 0.5)
    }

    private fun Player.cancelEffect(charmEffect: CharmEffect) {
        periodicEffects.remove(uniqueId, charmEffect)?.cancel()
    }

    private val Player.inventoryMap: Map<Int, ItemStack>
        get() {
            val inventoryMap = HashMap<Int, ItemStack>()
            inventory.iterator().withIndex().forEachRemaining { (index, item: ItemStack?) ->
                item?.let { inventoryMap[index] = item}
            }
            return inventoryMap.filter { !it.value.isAir() }
        }

    fun cancelAllCharmTasks(player: Player) {
        periodicEffects.row(player.uniqueId).values.forEach { it.cancel() }
        periodicEffects.rowKeySet().remove(player.uniqueId)
        player.activePotionEffects.clear()
        permanentEffects.rowKeySet().remove(player.uniqueId)
    }
}
