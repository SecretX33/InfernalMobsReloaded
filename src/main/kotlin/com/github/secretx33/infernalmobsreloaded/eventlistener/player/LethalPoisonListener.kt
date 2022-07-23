package com.github.secretx33.infernalmobsreloaded.eventlistener.player

import com.github.secretx33.infernalmobsreloaded.config.Config
import com.github.secretx33.infernalmobsreloaded.config.ConfigKeys
import com.github.secretx33.infernalmobsreloaded.model.KilledByPoison
import com.github.secretx33.infernalmobsreloaded.util.extension.runSync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
import toothpick.InjectConstructor
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@InjectConstructor
class LethalPoisonListener(
    private val plugin: Plugin,
    private val config: Config,
    private val coroutineScope: CoroutineScope,
) : Listener {

    private val scheduledLethalTicks = ConcurrentHashMap.newKeySet<UUID>()

    @EventHandler
    private fun EntityDamageEvent.onPoisonTick() {
        if (isNotPoison() || lethalPoisonDisabled) return

        val entity = entity as? LivingEntity ?: return
        val killedBy = killedByPoison

        // if the entity doesn't fit the target requirements
        if (killedBy == KilledByPoison.PLAYERS && entity !is Player || killedBy == KilledByPoison.MONSTERS && entity !is Monster) return

        // if the entity HP is higher than the threshold, do nothing
        if (entity.health > MIN_HP_VALUE_TO_DIE_OF_POISON) return

        // die, filth creature
        entity.scheduleLethalTick()
    }

    private fun EntityDamageEvent.isNotPoison(): Boolean = cause != DamageCause.POISON

    private fun LivingEntity.scheduleLethalTick() {
        val poison = getAffectedPoison() ?: return

        // If lethal tick is already scheduled
        if (uniqueId in scheduledLethalTicks) return

        // add entity to the lethal ticks schedule
        scheduledLethalTicks.add(uniqueId)

        coroutineScope.launch {
            delay(poison.getTickDelay())
            // if entity is not poisoned anymore, or has its health above the threshold, don't do anything
            if (isDead || !isValid || !isPoisoned() || uniqueId !in scheduledLethalTicks || health > (MIN_HP_VALUE_TO_DIE_OF_POISON + 0.5)) {
                scheduledLethalTicks -= uniqueId
                return@launch
            }
            killByPoison()
        }
    }

    private fun LivingEntity.getAffectedPoison(): PotionEffect? =
        activePotionEffects.firstOrNull { it.type == PotionEffectType.POISON }

    private fun LivingEntity.isPoisoned(): Boolean = getAffectedPoison() != null

    /**
     * Returns the poison tick delay in millis.
     */
    private fun PotionEffect.getTickDelay(): Long = when {
        amplifier == 0 -> 1250L
        amplifier <= 3 -> 600L
        else -> 500L
    }

    @EventHandler
    private fun PlayerQuitEvent.onPlayerQuit() {
        if (lethalPoisonDisabled || player.uniqueId !in scheduledLethalTicks) return
        // If player tries to logs off and he is on the scheduledLethalTicks, he's low hp and probably logging off to
        // avoid dying poisoned, so he'll be killed to prevent this exploit
        player.killByPoison()
    }

    private fun LivingEntity.killByPoison() =
        runSync(plugin) {
            val event = EntityDamageEvent(this, DamageCause.POISON, 100_000.0)
            if (!event.callEvent()) return@runSync
            lastDamageCause = event
            damage(event.finalDamage)
            scheduledLethalTicks -= uniqueId
        }

    private val killedByPoison: KilledByPoison
        get() = config.getEnum(ConfigKeys.LETHAL_POISON_TARGETS)

    private val lethalPoisonDisabled: Boolean
        get() = killedByPoison == KilledByPoison.NONE

    private companion object {
        const val MIN_HP_VALUE_TO_DIE_OF_POISON = 2.0
    }
}
