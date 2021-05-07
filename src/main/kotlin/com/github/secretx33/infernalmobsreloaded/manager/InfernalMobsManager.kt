package com.github.secretx33.infernalmobsreloaded.manager

import com.github.secretx33.infernalmobsreloaded.config.AbilityConfig
import com.github.secretx33.infernalmobsreloaded.config.AbilityConfigKeys
import com.github.secretx33.infernalmobsreloaded.config.Config
import com.github.secretx33.infernalmobsreloaded.config.ConfigKeys
import com.github.secretx33.infernalmobsreloaded.events.InfernalDamageDoneEvent
import com.github.secretx33.infernalmobsreloaded.events.InfernalDamageTakenEvent
import com.github.secretx33.infernalmobsreloaded.events.InfernalSpawnEvent
import com.github.secretx33.infernalmobsreloaded.model.Ability
import com.github.secretx33.infernalmobsreloaded.model.DisplayCustomNameMode
import com.github.secretx33.infernalmobsreloaded.model.InfernalMobType
import com.github.secretx33.infernalmobsreloaded.model.KeyChain
import com.github.secretx33.infernalmobsreloaded.repositories.InfernalMobTypesRepo
import com.github.secretx33.infernalmobsreloaded.utils.formattedTypeName
import com.github.secretx33.infernalmobsreloaded.utils.pdc
import com.github.secretx33.infernalmobsreloaded.utils.toUuid
import com.google.common.collect.MultimapBuilder
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import org.bukkit.Bukkit
import org.bukkit.Particle
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Mob
import org.bukkit.entity.Monster
import org.bukkit.persistence.PersistentDataType
import org.koin.core.component.KoinApiExtension
import java.lang.reflect.Type
import java.util.*
import kotlin.math.max


@KoinApiExtension
class InfernalMobsManager (
    private val config: Config,
    private val keyChain: KeyChain,
    private val abilityConfig: AbilityConfig,
    private val infernalMobTypesRepo: InfernalMobTypesRepo,
    private val particlesHelper: ParticlesHelper,
    private val abilityHelper: AbilityHelper,
) {

    private val infernalMobParticleTasks = HashMap<UUID, Job>()   // stores the job currently emitting infernal particles
    private val infernalMobAbilityTasks = MultimapBuilder.hashKeys().arrayListValues().build<UUID, Job>()  // for abilities that require a target

    fun isValidInfernalMob(entity: LivingEntity) = infernalMobTypesRepo.canTypeBecomeInfernal(entity.type) && entity.pdc.get(keyChain.infernalCategoryKey, PersistentDataType.STRING)?.let { infernalMobTypesRepo.isValidInfernalType(it) } == true

    fun isPossibleInfernalMob(entity: LivingEntity) = entity.pdc.has(keyChain.infernalCategoryKey, PersistentDataType.STRING)

    fun getInfernalGroupNameOrNull(entity: LivingEntity) = entity.pdc.get(keyChain.infernalCategoryKey, PersistentDataType.STRING)

    fun getInfernalAbilities(entity: LivingEntity) = entity.getAbilities() ?: throw IllegalStateException("Queried for infernal mob abilities but it doesn't have any")

    fun isMountOfAnotherInfernal(entity: Entity) = entity.pdc.has(keyChain.infernalMountKey, PersistentDataType.SHORT) || entity.pdc.has(keyChain.infernalBatMountKey, PersistentDataType.SHORT)

    fun getInfernalTypeOrNull(entity: LivingEntity) = entity.pdc.get(keyChain.infernalCategoryKey, PersistentDataType.STRING)
        ?.let { infernalMobTypesRepo.getInfernalTypeOrNull(it) }

    fun getLives(entity: LivingEntity): Int = entity.pdc.get(keyChain.livesKey, PersistentDataType.INTEGER) ?: throw IllegalStateException("Entity ${entity.type.name} doesn't have a ${keyChain.livesKey.key} key on its pdc, but tried to query its lives")

    fun setLives(entity: LivingEntity, lives: Int) = entity.pdc.set(keyChain.livesKey, PersistentDataType.INTEGER, lives)

    fun hasAbility(entity: LivingEntity, ability: Ability) = entity.getAbilities()?.contains(ability) ?: false

    fun makeInfernalMob(event: InfernalSpawnEvent) {
        val entity = event.entity
        val infernalType = event.infernalType

        addCustomNameToInfernal(entity, infernalType)
        addPdcKeysToInfernal(entity, infernalType, event)
        entity.addInfernalCustomAttribs(infernalType)
        abilityHelper.addAbilityEffects(entity)
    }

    private fun LivingEntity.addInfernalCustomAttribs(infernalType: InfernalMobType) {
        // follow range attribute
        getAttribute(Attribute.GENERIC_FOLLOW_RANGE)?.let { hp ->
            val mod = AttributeModifier(followRangeUID, "infernal_follow_range_multi", max(0.0, infernalType.getFollowRangeMulti() - 1), AttributeModifier.Operation.ADD_SCALAR)
            hp.removeModifier(mod)
            hp.addModifier(mod)
        }
        // attack knockback modifier
        getAttribute(Attribute.GENERIC_ATTACK_KNOCKBACK)?.let { atkKnockback ->
            val mod = AttributeModifier(atkKnockbackUID, "infernal_atk_knockback_multi", max(0.0, infernalType.getAtkKnockbackMod()), AttributeModifier.Operation.ADD_NUMBER)
            atkKnockback.removeModifier(mod)
            atkKnockback.addModifier(mod)
        }
        // health attribute
        getAttribute(Attribute.GENERIC_MAX_HEALTH)?.let { hp ->
            val mod = AttributeModifier(healthUID, "infernal_hp_multi", max(0.01, infernalType.getHealthMulti() - 1), AttributeModifier.Operation.ADD_SCALAR)
            val percentHP = health / hp.value
            hp.removeModifier(mod)
            hp.addModifier(mod)
            health = hp.value * percentHP // Preserve the entity HP percentage when modifying HP
        }
        // speed attribute
        getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)?.let { moveSpeed ->
            val mod = AttributeModifier(moveSpeedUID, "infernal_speed_multi", max(0.0, infernalType.getSpeedMulti() - 1), AttributeModifier.Operation.ADD_SCALAR)
            moveSpeed.removeModifier(mod)
            moveSpeed.addModifier(mod)
        }
    }

    private fun addCustomNameToInfernal(entity: LivingEntity, infernalType: InfernalMobType) {
        val displayMode = config.getEnum<DisplayCustomNameMode>(ConfigKeys.DISPLAY_INFERNAL_NAME_MODE)
        entity.apply {
            isPersistent = true
            if(displayMode.addCustomName) customName(infernalType.displayName)
            isCustomNameVisible = displayMode.customNameVisible
        }
    }

    private fun addPdcKeysToInfernal(entity: LivingEntity, infernalType: InfernalMobType, event: InfernalSpawnEvent) {
        val abilitySet = (event.abilitySet.takeIf { !event.randomAbilities }
            ?: Ability.random(infernalType.getAbilityNumber(), disabledAbilities)).filterInvalid()
        val livesNumber = if(abilitySet.contains(Ability.SECOND_WIND)) 2 else 1

        entity.pdc.apply {
            set(keyChain.infernalCategoryKey, PersistentDataType.STRING, infernalType.name)
            set(keyChain.abilityListKey, PersistentDataType.STRING, abilitySet.toJson())
            set(keyChain.livesKey, PersistentDataType.INTEGER, livesNumber)
        }
    }

    private fun Set<Ability>.filterInvalid(): Set<Ability> {
        val newSet = HashSet(this)
        // filter conflicts
        if(contains(Ability.FLYING) && contains(Ability.MOUNTED)) {
            if(random.nextInt(2) == 0) newSet.remove(Ability.FLYING)
            else newSet.remove(Ability.MOUNTED)
            newSet += Ability.random(1, disabledAbilities + Ability.FLYING + Ability.MOUNTED)
        }
        return newSet
    }

    private val disabledAbilities: Set<Ability>
        get() = config.getEnumSet(ConfigKeys.DISABLED_ABILITIES, Ability::class.java)

    private fun unmakeInfernalMob(entity: LivingEntity) {
        removeCustomNameOfInfernal(entity)
        removePdcKeysOfInfernal(entity)
        cancelAllInfernalTasks(entity)
    }

    private fun removePdcKeysOfInfernal(entity: LivingEntity) {
        entity.pdc.apply {
            remove(keyChain.infernalCategoryKey)
            remove(keyChain.abilityListKey)
            remove(keyChain.livesKey)
        }
    }

    private fun removeCustomNameOfInfernal(entity: LivingEntity) {
        entity.apply {
            isPersistent = entity !is Monster
            isCustomNameVisible = false
            customName(null)
        }
    }

    fun loadInfernalMob(entity: LivingEntity) {
        val savedType = entity.pdc.get(keyChain.infernalCategoryKey, PersistentDataType.STRING) ?: return

        // is type of that infernal is missing, convert it back to a normal entity
        if(!infernalMobTypesRepo.isValidInfernalType(savedType)) {
            unmakeInfernalMob(entity)
            return
        }
        startParticleEmissionTask(entity)
        val target = (entity as? Mob)?.target ?: return
        startTargetAbilityTasks(entity, target)
    }

    private fun startParticleEmissionTask(entity: LivingEntity) {
        cancelParticleTask(entity) // for safety
        if(!infernalParticlesEnabled || entity.disableInvisibleParticles()) return
        val particleType = particleType
        val particleSpread = particleSpread
        val delay = delayBetweenParticleEmission

        val job = CoroutineScope(Dispatchers.Default).launch {
            while(isActive) {
                particlesHelper.sendParticle(entity, particleType, particleSpread)
                delay(delay)
            }
        }
        infernalMobParticleTasks[entity.uniqueId] = job
    }

    private val infernalParticlesEnabled
        get() = config.get<Boolean>(ConfigKeys.ENABLE_INFERNAL_PARTICLES)

    private fun LivingEntity.disableInvisibleParticles() = abilityConfig.get(AbilityConfigKeys.INVISIBLE_DISABLE_INFERNAL_PARTICLES) && hasAbility(this, Ability.INVISIBLE)

    private val particleType
        get() = config.getEnum<Particle>(ConfigKeys.INFERNAL_PARTICLE_TYPE)

    private val delayBetweenParticleEmission
        get() = (max(0.01, config.get(ConfigKeys.DELAY_BETWEEN_INFERNAL_PARTICLES)) * 1000.0).toLong()

    private val particleSpread
        get() = config.get<Double>(ConfigKeys.INFERNAL_PARTICLES_SPREAD)

    fun loadAllInfernals() {
        Bukkit.getWorlds().forEach { world ->
            world.livingEntities.filter { isValidInfernalMob(it) }.forEach {
                loadInfernalMob(it)
            }
        }
    }

    fun unloadAllInfernals() {
        Bukkit.getWorlds().forEach { world ->
            world.livingEntities.filter { isValidInfernalMob(it) }.forEach { unloadInfernalMob(it) }
        }
    }

    fun unloadInfernalMob(entity: LivingEntity) {
        cancelAllInfernalTasks(entity)
    }

    fun startTargetAbilityTasks(entity: LivingEntity, target: LivingEntity) = infernalMobAbilityTasks.putAll(entity.uniqueId, abilityHelper.startTargetAbilityTasks(entity, target))

    fun triggerOnDamageDoneAbilities(event: InfernalDamageDoneEvent) = abilityHelper.triggerOnDamageDoneAbilities(event)

    fun triggerOnDamageTakenAbilities(event: InfernalDamageTakenEvent) = abilityHelper.triggerOnDamageTakenAbilities(event)

    fun triggerOnDeathAbilities(entity: LivingEntity) = abilityHelper.triggerOnDeathAbilities(entity)

    private fun cancelAllInfernalTasks(entity: LivingEntity) {
        cancelParticleTask(entity) // cancel particle task
        cancelAbilityTasks(entity) // cancel all currently running ability tasks
    }

    private fun cancelParticleTask(entity: LivingEntity) = infernalMobParticleTasks.remove(entity.uniqueId)?.cancel()

    fun cancelAbilityTasks(entity: LivingEntity) {
        infernalMobAbilityTasks[entity.uniqueId].forEach { it.cancel() }
        infernalMobAbilityTasks.removeAll(entity.uniqueId)
    }

    private fun LivingEntity.getAbilities(): Set<Ability>? = pdc.get(keyChain.abilityListKey, PersistentDataType.STRING)?.toAbilitySet()

    private fun Set<Ability>.toJson() = gson.toJson(this, infernalAbilitySetToken)

    private fun String.toAbilitySet() = gson.fromJson<Set<Ability>>(this, infernalAbilitySetToken)

    private companion object {
        val gson = Gson()
        val random = Random()
        val infernalAbilitySetToken: Type = object : TypeToken<Set<Ability>>() {}.type

        val followRangeUID: UUID = "ff6d1ee3-8c7e-4826-b795-945689b5dc76".toUuid()
        val atkKnockbackUID: UUID = "0c9a9cf0-4507-47b7-b4db-77be78e7d55e".toUuid()
        val healthUID: UUID = "23a4f497-15c5-4fe1-9f2b-4c2b1249ba42".toUuid()
        val moveSpeedUID: UUID = "913c083e-9b68-4677-bf27-510bc9aea94e".toUuid()
    }
}
