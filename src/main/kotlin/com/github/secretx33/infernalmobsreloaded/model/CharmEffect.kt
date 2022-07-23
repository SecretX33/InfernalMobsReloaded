package com.github.secretx33.infernalmobsreloaded.model

import com.github.secretx33.infernalmobsreloaded.util.extension.enumSetAll
import com.github.secretx33.infernalmobsreloaded.util.extension.enumSetOf
import com.github.secretx33.infernalmobsreloaded.util.extension.random
import net.kyori.adventure.text.Component
import org.bukkit.Particle
import org.bukkit.potion.PotionEffectType
import java.util.Locale
import kotlin.math.max

class CharmEffect (
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
    val requiredMainHand: String?,
    requiredItems: Set<String>,
    val requiredSlots: Set<Int>,
) {
    val requiredItems: Set<String> = if (requiredMainHand != null) requiredItems + requiredMainHand else requiredItems

    init {
        require(name.isNotBlank()) { "name cannot be blank, name = '$name'" }

        // potency
        require(potency.first >= 1 && potency.second >= 1) { "potency cannot be lower than 1, potency = $potency" }
        require(potency.first <= potency.second) { "potency first value has to be lower or equal than the second value, potency = $potency" }

        // duration
        require(duration.first >= 0 && duration.second >= 0) { "duration cannot be lower than 0, duration = $duration" }
        require(duration.first <= duration.second) { "duration first value has to be lower or equal than the second value, duration = $duration" }

        // delay
        require(delay.first >= 0 && delay.second >= 0) { "delay cannot be lower than 0, delay = $delay" }
        require(delay.first <= delay.second) { "delay first value has to be lower or equal than the second value, delay = $delay" }

        // requiresSlots
        require(requiredSlots.all { it >= 0 }) { "requiredSlots requires that all slots are not less than 0, but there's some number that is lower than 0 in it, set = '${requiredSlots.sorted().joinToString()}'" }

        require(effectApplyMode in particleMode.validApplyModes) { "effectApplyMode has to be inside valid list of particleMode, but $effectApplyMode is not inside $particleMode's validApplyModes = ${particleMode.validApplyModes}" }
    }

    fun getPotency() = max(0, potency.random() - 1)

    fun getDuration() = duration.random()

    fun getDelay() = delay.random()

    fun getMaxDelay() = delay.second

    val enabledSelfParticle
        get() = particle != null && (particleMode == CharmParticleMode.BOTH_WHEN_APPLIED || particleMode == CharmParticleMode.SELF_WHEN_APPLIED)

    val enabledTargetParticle
        get() = particle != null && (particleMode == CharmParticleMode.BOTH_WHEN_APPLIED || particleMode == CharmParticleMode.TARGET_WHEN_APPLIED)

    /**
     * For a given inventory (mapped to itemName, slot), it validates if all items are on the correct slots
     * for the effect to be granted from (or revoked of) the holder
     *
     * @param inventory Map<String, Int> a list containing all loot items owned, mapped as (lootItemTag <-> slot)
     * @return Boolean if effect can be granted to the holder
     */
    fun validateEffect(inventory: Map<String, Int>, mainHand: String?): Boolean {
        // no items should have this effect
        if (requiredItems.isEmpty()) return false

        // inventory don't have all the required items
        if (!inventory.keys.containsAll(requiredItems)) return false

        // if player doesn't have the required charm in the main hand
        if (requiredMainHand?.equals(mainHand, ignoreCase = true) == false) return false

        // all required items are in the correct slots (except main hand item)
        return (requiredItems - requiredMainHand).all { itemName ->
            inventory[itemName]?.let { slot -> slot in requiredSlots } == true
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        return name.lowercase(Locale.US) == (other as CharmEffect).name.lowercase(Locale.US)
    }

    override fun hashCode(): Int = name.lowercase(Locale.US).hashCode()

    override fun toString(): String = "CharmEffect(name='$name', potionEffect=$potionEffect, potency=$potency, duration=$duration, delay=$delay, effectApplyMode=$effectApplyMode, particle=$particle, particleMode=$particleMode, requiredItems=$requiredItems, requiredSlots=$requiredSlots)"
}

enum class PotionEffectApplyMode {
    SELF_PERMANENT, SELF_RECURRENT, SELF_ON_HIT, TARGET_ON_HIT, BOTH_ON_HIT
}

enum class CharmParticleMode(val validApplyModes: Set<PotionEffectApplyMode>) {
    NONE(enumSetAll<PotionEffectApplyMode>()),
    SELF_ONCE(enumSetOf(PotionEffectApplyMode.SELF_PERMANENT)),
    SELF_WHEN_APPLIED(enumSetOf(PotionEffectApplyMode.SELF_RECURRENT, PotionEffectApplyMode.SELF_ON_HIT, PotionEffectApplyMode.TARGET_ON_HIT, PotionEffectApplyMode.BOTH_ON_HIT)),
    TARGET_WHEN_APPLIED(enumSetOf(PotionEffectApplyMode.SELF_ON_HIT, PotionEffectApplyMode.TARGET_ON_HIT, PotionEffectApplyMode.BOTH_ON_HIT)),
    BOTH_WHEN_APPLIED(enumSetOf(PotionEffectApplyMode.SELF_ON_HIT, PotionEffectApplyMode.TARGET_ON_HIT, PotionEffectApplyMode.BOTH_ON_HIT)),
}
