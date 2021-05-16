package com.github.secretx33.infernalmobsreloaded.eventlisteners.player

import com.github.secretx33.infernalmobsreloaded.config.Config
import com.github.secretx33.infernalmobsreloaded.config.ConfigKeys
import com.github.secretx33.infernalmobsreloaded.model.KilledByPoison
import com.github.secretx33.infernalmobsreloaded.utils.runSync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.bukkit.Bukkit
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Monster
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDamageEvent.DamageCause
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.Plugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class LethalPoisonListener (
    private val plugin: Plugin,
    private val config: Config,
): Listener {

    init { Bukkit.getPluginManager().registerEvents(this, plugin) }

    private val scheduledLethalTicks = ConcurrentHashMap.newKeySet<UUID>()

    @EventHandler
    private fun EntityDamageEvent.onPoisonTick() {
        if(isNotPoison() || lethalPoisonDisabled) return

        val entity = entity as? LivingEntity ?: return
        val killedBy = killedByPoison

        // if the entity doesn't fit the target requirements
        if (killedBy == KilledByPoison.PLAYERS && entity !is Player || killedBy == KilledByPoison.MONSTERS && entity !is Monster) return

        // if the entity HP is higher than the threshold, do nothing
        if (entity.health > MIN_HP_VALUE_TO_DIE_OF_POISON) return

        // die, filth creature
        entity.scheduleLethalTick()
    }

    private fun EntityDamageEvent.isNotPoison() = cause != DamageCause.POISON

    private fun LivingEntity.scheduleLethalTick() {
        val poison = getAffectedPoison() ?: return

        // If lethal tick is already scheduled
        if(uniqueId in scheduledLethalTicks) return

        // add entity to the lethal ticks schedule
        scheduledLethalTicks.add(uniqueId)

        CoroutineScope(Dispatchers.Default).launch {
            delay(poison.getTickDelay())
            // if entity is not poisoned anymore, or has its health above the threshold, don't do anything
            if(isDead || !isValid || !isPoisoned() || uniqueId !in scheduledLethalTicks || health > (MIN_HP_VALUE_TO_DIE_OF_POISON + 0.5)) {
                scheduledLethalTicks.remove(uniqueId)
                return@launch
            }
            killByPoison()
        }
    }

    private fun LivingEntity.getAffectedPoison(): PotionEffect? = activePotionEffects.firstOrNull { it.type == PotionEffectType.POISON }

    private fun LivingEntity.isPoisoned() = getAffectedPoison() != null

    private fun PotionEffect.getTickDelay(): Long {
        var delay = 500.0 // delay in milliseconds

        if (amplifier == 0) delay = 1250.0
        else if (amplifier <= 3) delay = 600.0

        return delay.toLong()
    }

    @EventHandler
    private fun PlayerQuitEvent.onPlayerQuit() {
        if(lethalPoisonDisabled || player.uniqueId !in scheduledLethalTicks) return
        // If player tries to logs off and he is on the scheduledLethalTicks, he's low hp and probably logging off to avoid dying poisoned, so he'll be killed to prevent this exploit
        player.killByPoison()
    }

    private fun LivingEntity.killByPoison() {
        runSync(plugin) {
            val event = EntityDamageEvent(this, DamageCause.POISON, 100_000.0)
            if(!event.callEvent()) return@runSync
            lastDamageCause = event
            damage(event.finalDamage)
            scheduledLethalTicks.remove(uniqueId)
        }
    }

    private val killedByPoison
        get() = config.getEnum<KilledByPoison>(ConfigKeys.LETHAL_POISON_TARGETS)

    private val lethalPoisonDisabled
        get() = killedByPoison == KilledByPoison.NONE

    private companion object {
        const val MIN_HP_VALUE_TO_DIE_OF_POISON = 2.0
    }
}
