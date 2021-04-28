package com.github.secretx33.infernalmobsreloaded.manager

import com.github.secretx33.infernalmobsreloaded.config.AbilityConfig
import com.github.secretx33.infernalmobsreloaded.config.AbilityConfigKeys
import com.github.secretx33.infernalmobsreloaded.config.Config
import com.github.secretx33.infernalmobsreloaded.config.ConfigKeys
import com.github.secretx33.infernalmobsreloaded.model.Abilities
import com.github.secretx33.infernalmobsreloaded.model.KeyChain
import com.github.secretx33.infernalmobsreloaded.repositories.LootItemsRepo
import com.github.secretx33.infernalmobsreloaded.utils.pdc
import com.github.secretx33.infernalmobsreloaded.utils.toUuid
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Job
import org.bukkit.DyeColor
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.*
import org.bukkit.entity.EntityType
import org.bukkit.event.entity.CreatureSpawnEvent.*
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.material.Colorable
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.koin.core.component.KoinApiExtension
import java.lang.reflect.Type
import java.util.*
import kotlin.math.max


@KoinApiExtension
class AbilityHelper (
    private val keyChain: KeyChain,
    private val config: Config,
    private val abilityConfig: AbilityConfig,
    private val lootItemsRepo: LootItemsRepo,
){

    fun removeAbilityEffects(entity: LivingEntity) {
        TODO()
    }

    fun addAbilityEffects(entity: LivingEntity) {
        val abilityList = entity.getAbilities() ?: return
        abilityList.forEach {
            when(it) {
                Abilities.ARMOURED -> addArmouredAbility(entity)
                Abilities.FLYING -> TODO()
                Abilities.GHASTLY -> TODO()
                Abilities.HEAVY -> addHeavyAbility(entity)
                Abilities.INVISIBLE -> addInvisibleAbility(entity)
                Abilities.MOLTEN -> addMoltenAbility(entity)
                Abilities.MORPH -> TODO()
                Abilities.MOUNTED -> addMountedAbility(entity)
                Abilities.POTIONS -> TODO()
                Abilities.SLOWNESS -> TODO()
                Abilities.SPEEDY -> addSpeedyAbility(entity)
                else -> {}
            }
        }
    }

    private fun addMountedAbility(entity: LivingEntity) {
        entity.vehicle?.apply {
            removePassenger(entity)
            if(passengers.isEmpty()) remove()
        }
        val mounts = rideableMounts

        // if player emptied the mount list, do not mount the entity
        if(mounts.isEmpty()) return
        // get the a random mount entity class
        val mountClass = mounts.random().entityClass ?: return
        // spawn it, applying the necessary changes
        val mount = entity.world.spawn(entity.location, mountClass, SpawnReason.CUSTOM) { turnIntoMount(it as LivingEntity) }
        // and add the entity as its passenger
        mount.addPassenger(entity)
    }

    private val rideableMounts
        get() = config.getEnumSet(ConfigKeys.INFERNAL_MOBS_THAT_CAN_BE_RIDED_BY_ANOTHER, EntityType::class.java) { it != null && it.isSpawnable && it.entityClass is LivingEntity && it.entityClass !is ComplexLivingEntity }

    private fun turnIntoMount(entity: LivingEntity) {
        // if entity that will be turned into a mount spawns mounted on something, remove that mount
        entity.vehicle?.remove()
        // if entity that will be turned into a mount spawns with something mounted on it, remove that sneaky passenger
        entity.passengers.forEach {
            entity.removePassenger(it)
            it.remove()
        }
        (entity as? Tameable)?.isTamed = true
        (entity as? Colorable)?.color = DyeColor.values().apply { shuffle() }.first()

        if(entity.type == EntityType.HORSE) {
            (entity as Horse).apply {
                color = Horse.Color.values().random()
                style = Horse.Style.values().random()
            }
            entity.inventory.apply {
                saddle = ItemStack(Material.SADDLE)
                armor = ItemStack(Material.DIAMOND_HORSE_ARMOR)
            }
        }
        addArmouredPotionEffect(entity)
        entity.apply {
            addPotionEffect(PotionEffect(PotionEffectType.SPEED, Int.MAX_VALUE, 0, true, true, false))
            pdc.set(keyChain.infernalMountKey, PersistentDataType.SHORT, 1)
        }
    }

    private fun addSpeedyAbility(entity: LivingEntity) {
        val movSpeed = (if(entity.doesFly()) entity.getAttribute(Attribute.GENERIC_FLYING_SPEED) else entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED))
            ?: return
        val speedBonus = abilityConfig.getDoublePair(AbilityConfigKeys.SPEEDY_BONUS).getRandomBetween()
        val mod = AttributeModifier(movSpeedUID, Abilities.SPEEDY.name, speedBonus, AttributeModifier.Operation.ADD_SCALAR)
        movSpeed.removeModifier(mod)
        movSpeed.addModifier(mod)
    }

    private fun addArmouredAbility(entity: LivingEntity) {
        if(entity.equipWithArmor()) return
        // fallback to potion effect if entity cannot wear armor
        addArmouredPotionEffect(entity)
    }

    private fun addArmouredPotionEffect(entity: LivingEntity) {
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
        equip.apply {
            helmet = lootItemsRepo.getLootItem("armoured_helmet").makeItem()
            chestplate = lootItemsRepo.getLootItem("armoured_chestplate").makeItem()
            leggings = lootItemsRepo.getLootItem("armoured_leggings").makeItem()
            boots =  lootItemsRepo.getLootItem("armoured_boots").makeItem()
            when(itemInMainHand.type) {
                Material.BOW -> lootItemsRepo.getLootItem("armoured_bow").makeItem()
                Material.CROSSBOW -> lootItemsRepo.getLootItem("armoured_crossbow").makeItem()
                else -> lootItemsRepo.getLootItem("armoured_sword").makeItem()
            }
        }
        return true
    }

    private fun addHeavyAbility(entity: LivingEntity) {
        entity.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE)?.apply {
            val resistAmount = abilityConfig.getDoublePair(AbilityConfigKeys.HEAVY_RESIST_PERCENTAGE).getRandomBetween()
            val mod = AttributeModifier(knockbackResistUID, Abilities.HEAVY.name, resistAmount, AttributeModifier.Operation.ADD_SCALAR)
            removeModifier(mod)
            addModifier(mod)
        }
    }

    private fun addInvisibleAbility(entity: LivingEntity) = entity.addPermanentPotion(PotionEffectType.INVISIBILITY, Abilities.INVISIBLE)

    private fun addMoltenAbility(entity: LivingEntity) = entity.addPermanentPotion(PotionEffectType.FIRE_RESISTANCE, Abilities.MOLTEN)

    private fun LivingEntity.addPermanentPotion(effectType: PotionEffectType, ability: Abilities, amplifier: Int = 0) {
        addPotionEffect(PotionEffect(effectType, Int.MAX_VALUE, amplifier, abilityConfig.getPotionIsAmbient(ability), abilityConfig.getPotionEmitParticles(ability), false))
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
                Abilities.SLOWNESS -> TODO()
                Abilities.RUST -> TODO()
                Abilities.SAPPER -> TODO()
                Abilities.SECOND_WING -> TODO()
                Abilities.SPEEDY -> TODO()
                Abilities.LIGHTNING -> TODO()
                Abilities.TELEPORT -> TODO()
                Abilities.THIEF -> TODO()
                Abilities.THORNMAIL -> TODO()
                Abilities.TOSSER -> TODO()
                Abilities.WEAKNESS -> TODO()
                Abilities.WEBBER -> TODO()
                Abilities.WITHERING -> TODO()
                else -> {}
            }
        }
    }

    private fun Pair<Int, Int>.getRandomBetween(): Int {
        val (minValue, maxValue) = this
        return random.nextInt(maxValue - minValue) + minValue
    }

    private fun Pair<Double, Double>.getRandomBetween(): Double {
        val (minValue, maxValue) = this
        return minValue + (maxValue - minValue) * random.nextDouble()
    }

    private fun LivingEntity.doesFly() = this is Bee || this is Parrot

    private fun LivingEntity.getAbilities(): Set<Abilities>? = pdc.get(keyChain.abilityListKey, PersistentDataType.STRING)?.toAbilitySet()

    private fun String.toAbilitySet() = gson.fromJson<Set<Abilities>>(this, infernalAbilityListToken)

    private companion object {
        val random = Random()
        val gson = Gson()
        val infernalAbilityListToken: Type = object : TypeToken<Set<Abilities>>() {}.type
        val movSpeedUID: UUID = "57202f4c-2e52-46cb-ad37-77550e99edb2".toUuid()
        val knockbackResistUID: UUID = "984e7a8c-188f-444b-82ea-5d02197ea8e4".toUuid()
    }
}
