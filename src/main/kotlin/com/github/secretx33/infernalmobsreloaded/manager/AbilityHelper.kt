package com.github.secretx33.infernalmobsreloaded.manager

import com.github.secretx33.infernalmobsreloaded.config.AbilityConfig
import com.github.secretx33.infernalmobsreloaded.config.AbilityConfigKeys
import com.github.secretx33.infernalmobsreloaded.model.Abilities
import com.github.secretx33.infernalmobsreloaded.model.KeyChain
import com.github.secretx33.infernalmobsreloaded.utils.pdc
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Job
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Bee
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Parrot
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.lang.reflect.Type
import java.util.*
import kotlin.math.max


class AbilityHelper (
    private val keyChain: KeyChain,
    private val abilityConfig: AbilityConfig,
){

    fun removeAbilityEffects(entity: LivingEntity) {
        TODO()
    }

    fun addAbilityEffects(entity: LivingEntity) {
        val abilityList = entity.getAbilities() ?: return
        abilityList.forEach {
            when(it) {
                Abilities.ARMOURED -> addArmouredAbility(entity)
                Abilities.FIREWORK -> TODO()
                Abilities.FLYING -> TODO()
                Abilities.GHASTLY -> TODO()
                Abilities.HEAVY -> addHeavyAbility(entity)
                Abilities.INVISIBLE -> addInvisibleAbility(entity)
                Abilities.MOLTEN -> addMoltenAbility(entity)
                Abilities.MORPH -> TODO()
                Abilities.MOUNTED -> TODO()
                Abilities.POTIONS -> TODO()
                Abilities.QUICKSAND -> TODO()
                Abilities.RESILIENT -> TODO()
                Abilities.SPEEDY -> addSpeedyAbility(entity)
                else -> {}
            }
        }
    }

    private fun addSpeedyAbility(entity: LivingEntity) {
        val movSpeed = (if(entity.doesFly()) entity.getAttribute(Attribute.GENERIC_FLYING_SPEED) else entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED))
            ?: return
        val mod = AttributeModifier(movSpeedUID, Abilities.SPEEDY.name, speedyBonus, AttributeModifier.Operation.ADD_SCALAR)
        movSpeed.removeModifier(mod)
        movSpeed.addModifier(mod)
    }

    private fun addArmouredAbility(entity: LivingEntity) {
        if (entity.equipWithArmor()) return
        // fallback to potion effect if entity cannot wear armor
        entity.addPotionEffect(PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, Int.MAX_VALUE, max(0, abilityConfig.get<Int>(AbilityConfigKeys.ARMOURED_POTION_LEVEL) - 1), false, false, false))
    }

    /**
     * Tries to equip default armor on entity, returning false if it cannot wear armor.
     *
     * @receiver LivingEntity the entity to equip armor onto
     * @return Boolean returns true if entity was successfully equipped with armor
     */
    private fun LivingEntity.equipWithArmor(): Boolean {
        val equip = equipment ?: return false
        EquipmentSlot.values().forEach { equip.setDropChance(it, 0f) }
        val helm = ItemStack(Material.NETHERITE_HELMET, 1).apply { addUnsafeEnchantment(Enchantment.PROTECTION_PROJECTILE, 1) }
        val chest = ItemStack(Material.NETHERITE_CHESTPLATE, 1).apply { addUnsafeEnchantment(Enchantment.PROTECTION_EXPLOSIONS, 1) }
        val pants = ItemStack(Material.NETHERITE_LEGGINGS, 1).apply { addUnsafeEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 1) }
        val boots = ItemStack(Material.NETHERITE_BOOTS, 1).apply { addUnsafeEnchantment(Enchantment.SOUL_SPEED, 1) }
        val sword = ItemStack(Material.NETHERITE_SWORD, 1).apply { addUnsafeEnchantment(Enchantment.DAMAGE_ALL, 4) }
        val bow = ItemStack(Material.BOW).apply {
            addUnsafeEnchantment(Enchantment.ARROW_KNOCKBACK, 2)
            addUnsafeEnchantment(Enchantment.ARROW_DAMAGE, 2)
        }
        equip.apply {
            helmet = helm
            chestplate = chest
            leggings = pants
            setBoots(boots)
            if(itemInMainHand.type == Material.BOW) setItemInMainHand(bow)
            else setItemInMainHand(sword)
        }
        return true
    }

    private fun addHeavyAbility(entity: LivingEntity) {
        entity.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE)?.apply {
            val mod =
                AttributeModifier(knockbackResistUID, Abilities.HEAVY.name, abilityConfig.get(AbilityConfigKeys.HEAVY_RESIST_PERCENTAGE), AttributeModifier.Operation.ADD_SCALAR)
            removeModifier(mod)
            addModifier(mod)
        }
    }

    private fun addInvisibleAbility(entity: LivingEntity) {
        entity.addPotionEffect(PotionEffect(PotionEffectType.INVISIBILITY, Int.MAX_VALUE, 0, abilityConfig.getPotionIsAmbient(Abilities.INVISIBLE), abilityConfig.getPotionEmitParticles(Abilities.INVISIBLE), false))
    }

    private fun addMoltenAbility(entity: LivingEntity) {
        entity.addPotionEffect(PotionEffect(PotionEffectType.FIRE_RESISTANCE, Int.MAX_VALUE, 0, abilityConfig.getPotionIsAmbient(Abilities.INVISIBLE), abilityConfig.getPotionEmitParticles(Abilities.INVISIBLE), false))
    }

    fun startTargetTasks(entity: LivingEntity, target: LivingEntity): List<Job> {
        val abilities = entity.getAbilities() ?: return emptyList()

        val jobList = ArrayList<Job>()
        abilities.forEach {
            when(it) {
                Abilities.ARCHER -> TODO()
                Abilities.CALL_THE_GANG -> TODO()
                Abilities.FIREWORK -> TODO()
                Abilities.GHASTLY -> TODO()
                Abilities.LEVITATE -> TODO()
                Abilities.MOUNTED -> TODO()
                Abilities.NECROMANCER -> TODO()
                Abilities.POISONOUS -> TODO()
                Abilities.POTIONS -> TODO()
                Abilities.QUICKSAND -> TODO()
                Abilities.RESILIENT -> TODO()
                Abilities.RUST -> TODO()
                Abilities.SAPPER -> TODO()
                Abilities.SECOND_WING -> TODO()
                Abilities.SPEEDY -> TODO()
                Abilities.STORM -> TODO()
                Abilities.TELEPORT -> TODO()
                Abilities.THIEF -> TODO()
                Abilities.TOSSER -> TODO()
                Abilities.VENGEANCE -> TODO()
                Abilities.WEAKNESS -> TODO()
                Abilities.WEBBER -> TODO()
                Abilities.WITHERING -> TODO()
                else -> {}
            }
        }
    }

    private val speedyBonus
        get() = abilityConfig.get<Double>(AbilityConfigKeys.SPEEDY_MIN_BONUS) + (abilityConfig.get<Double>(AbilityConfigKeys.SPEEDY_MAX_BONUS) - abilityConfig.get<Double>(AbilityConfigKeys.SPEEDY_MIN_BONUS)) * random.nextDouble()

    private fun LivingEntity.doesFly() = this is Bee || this is Parrot

    private fun LivingEntity.getAbilities(): Set<Abilities>? = pdc.get(keyChain.abilityListKey, PersistentDataType.STRING)?.toAbilitySet()

    private fun String.toAbilitySet() = gson.fromJson<Set<Abilities>>(this, infernalAbilityListToken)

    private companion object {
        val random = Random()
        val gson = Gson()
        val infernalAbilityListToken: Type = object : TypeToken<Set<Abilities>>() {}.type
        private val movSpeedUID = UUID.fromString("57202f4c-2e52-46cb-ad37-77550e99edb2")
        private val knockbackResistUID = UUID.fromString("984e7a8c-188f-444b-82ea-5d02197ea8e4")
    }
}
