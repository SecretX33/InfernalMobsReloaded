package com.github.secretx33.infernalmobsreloaded.model

import com.github.secretx33.infernalmobsreloaded.model.items.LootItem
import com.github.secretx33.infernalmobsreloaded.utils.getRandomBetween
import net.kyori.adventure.text.Component
import org.bukkit.Particle
import org.bukkit.potion.PotionEffectType
import org.koin.core.component.KoinApiExtension

@KoinApiExtension
data class CharmEffect (
    val name: String,
    val playerMessage: Component?,   // null = don't send anything
    val targetMessage: Component?,   // null = don't send anything
    val potionEffect: PotionEffectType,
    private val potency: Pair<Int, Int>,
    private val duration: Pair<Double, Double>,
    private val delay: Pair<Double, Double>,
    val effectApplyMode: PotionEffectApplyMode,
    val particle: Particle?,
    val particleMode: CharmParticleMode,
    val requiredItems: Set<String>,
    val requiredSlots: Set<Int>,
) {

    init {
        // potency
        require(potency.first >= 0 && potency.second >= 0) { "potency cannot be lower than 0, potency = $potency" }
        require(potency.first <= potency.second) { "potency first value has to be lower or equal than the second value, potency = $potency" }

        // duration
        require(duration.first >= 0 && duration.second >= 0) { "duration cannot be lower than 0, duration = $duration" }
        require(duration.first <= duration.second) { "duration first value has to be lower or equal than the second value, duration = $duration" }

        // delay
        require(delay.first >= 0 && delay.second >= 0) { "delay cannot be lower than 0, delay = $delay" }
        require(delay.first <= delay.second) { "delay first value has to be lower or equal than the second value, delay = $delay" }

        require(requiredSlots.all { it >= 0 }) { "requiredSlots requires that all slots are not less than 0, but there's some number that is lower than 0 in it, set = '${requiredSlots.joinToString()}'" }

        require(effectApplyMode in particleMode.validApplyModes) { "effectApplyMode has to be inside valid list of particleMode, but $effectApplyMode is not inside $particleMode's validApplyModes = ${particleMode.validApplyModes}" }
    }

    fun getPotency() = potency.getRandomBetween()

    fun getDuration() = duration.getRandomBetween()

    fun getDelay() = delay.getRandomBetween()

    val isPermanent
        get() = effectApplyMode == PotionEffectApplyMode.SELF_PERMANENT

    val isRecurrent
        get() = effectApplyMode == PotionEffectApplyMode.SELF_RECURRENT

    val triggersOnDamage
        get() = effectApplyMode == PotionEffectApplyMode.TARGET_TEMPORARY
}

enum class PotionEffectApplyMode {
    SELF_PERMANENT, SELF_RECURRENT, TARGET_TEMPORARY
}

enum class CharmParticleMode(val validApplyModes: Set<PotionEffectApplyMode>) {
    NONE(PotionEffectApplyMode.values().toSet()),
    SELF_ONCE(setOf(PotionEffectApplyMode.SELF_PERMANENT)),
    ON_SELF_WHEN_APPLIED(setOf(PotionEffectApplyMode.SELF_RECURRENT, PotionEffectApplyMode.TARGET_TEMPORARY)),
    ON_TARGET_WHEN_APPLIED(setOf(PotionEffectApplyMode.TARGET_TEMPORARY)),
    ON_BOTH_WHEN_APPLIED(setOf(PotionEffectApplyMode.TARGET_TEMPORARY)),
}
