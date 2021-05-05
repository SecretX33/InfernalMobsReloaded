package com.github.secretx33.infernalmobsreloaded.model

import com.github.secretx33.infernalmobsreloaded.model.items.LootItem
import net.kyori.adventure.text.Component
import org.bukkit.Particle
import org.bukkit.potion.PotionEffectType

data class CharmEffect (
    private val playerMessage: Component?,   // null = don't send anything
    private val targetMessage: Component?,   // null = don't send anything
    private val potionEffect: PotionEffectType,
    private val potency: Pair<Int, Int>,
    private val duration: Pair<Double, Double>,
    private val delay: Pair<Double, Double>,
    private val effectApplyMode: PotionEffectApplyMode,
    private val particle: Particle?,
    private val particleMode: CharmParticleDisplayMode,
    private val requiredItems: Set<LootItem>,
    private val requiredSlots: Set<Int>,
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
}

enum class PotionEffectApplyMode() {
    SELF_PERMANENT, SELF_RECURRENT, TARGET_TEMPORARY
}

enum class CharmParticleDisplayMode(val validApplyModes: Set<PotionEffectApplyMode>) {
    SELF_ONCE(setOf(PotionEffectApplyMode.SELF_PERMANENT)),
    ON_SELF_WHEN_APPLIED(setOf(PotionEffectApplyMode.SELF_RECURRENT, PotionEffectApplyMode.TARGET_TEMPORARY)),
    ON_TARGET_WHEN_APPLIED(setOf(PotionEffectApplyMode.TARGET_TEMPORARY)),
    ON_BOTH_WHEN_APPLIED(setOf(PotionEffectApplyMode.TARGET_TEMPORARY)),
}
