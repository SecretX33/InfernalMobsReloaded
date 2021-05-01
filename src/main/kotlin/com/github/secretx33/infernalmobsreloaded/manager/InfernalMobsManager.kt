package com.github.secretx33.infernalmobsreloaded.manager

import com.github.secretx33.infernalmobsreloaded.config.Config
import com.github.secretx33.infernalmobsreloaded.config.ConfigKeys
import com.github.secretx33.infernalmobsreloaded.events.InfernalDamageDoneEvent
import com.github.secretx33.infernalmobsreloaded.events.InfernalDamageTakenEvent
import com.github.secretx33.infernalmobsreloaded.events.InfernalSpawnEvent
import com.github.secretx33.infernalmobsreloaded.model.Abilities
import com.github.secretx33.infernalmobsreloaded.model.DisplayCustomNameMode
import com.github.secretx33.infernalmobsreloaded.model.InfernalMobType
import com.github.secretx33.infernalmobsreloaded.model.KeyChain
import com.github.secretx33.infernalmobsreloaded.repositories.InfernalMobTypesRepo
import com.github.secretx33.infernalmobsreloaded.utils.pdc
import com.google.common.collect.MultimapBuilder
import com.google.common.collect.Multimaps
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import org.bukkit.Bukkit
import org.bukkit.Particle
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
    private val infernalMobTypesRepo: InfernalMobTypesRepo,
    private val particlesHelper: ParticlesHelper,
    private val abilityHelper: AbilityHelper,
) {

    private val infernalMobPeriodicTasks = Multimaps.synchronizedListMultimap(MultimapBuilder.hashKeys().arrayListValues().build<UUID, Job>())
    private val infernalMobTargetTasks = Multimaps.synchronizedListMultimap(MultimapBuilder.hashKeys().arrayListValues().build<UUID, Job>())

    fun isValidInfernalMob(entity: LivingEntity) = infernalMobTypesRepo.canTypeBecomeInfernal(entity.type) && entity.pdc.get(keyChain.infernalCategoryKey, PersistentDataType.STRING)?.let { infernalMobTypesRepo.isValidInfernalType(it) } == true

    fun isPossibleInfernalMob(entity: LivingEntity) = entity.pdc.has(keyChain.infernalCategoryKey, PersistentDataType.STRING)

    fun getInfernalGroupNameOrNull(entity: LivingEntity) = entity.pdc.get(keyChain.infernalCategoryKey, PersistentDataType.STRING)

    fun getInfernalAbilities(entity: LivingEntity) = entity.getAbilities() ?: throw IllegalStateException("Queried for infernal mob abilities but it doesn't have any")

    fun isMountOfAnotherInfernal(entity: Entity) = entity.pdc.has(keyChain.infernalMountKey, PersistentDataType.SHORT) || entity.pdc.has(keyChain.infernalBatMountKey, PersistentDataType.SHORT)

    fun getInfernalTypeOrNull(entity: LivingEntity) = entity.pdc.get(keyChain.infernalCategoryKey, PersistentDataType.STRING)
        ?.let { infernalMobTypesRepo.getInfernalTypeOrNull(it) }

    fun getLives(entity: LivingEntity): Int = entity.pdc.get(keyChain.livesKey, PersistentDataType.INTEGER) ?: throw IllegalStateException("Entity ${entity.type.name} doesn't have a ${keyChain.livesKey.key} key on its pdc, but tried to query its lives")

    fun setLives(entity: LivingEntity, lives: Int) = entity.pdc.set(keyChain.livesKey, PersistentDataType.INTEGER, lives)

    fun makeInfernalMob(event: InfernalSpawnEvent) {
        val entity = event.entity
        val infernalType = event.infernalType

        addCustomNameToInfernal(entity, infernalType)
        addPdcKeysToInfernal(entity, infernalType, event)
        abilityHelper.addAbilityEffects(entity, infernalType)
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
        val abilitySet = event.abilitySet?.toMutableSet()?.filterConflicts() ?: Abilities.random(infernalType.getAbilityNumber()).filterConflicts()
        val livesNumber = if(abilitySet.contains(Abilities.SECOND_WING)) 2 else 1

        entity.pdc.apply {
            set(keyChain.infernalCategoryKey, PersistentDataType.STRING, infernalType.name)
            set(keyChain.abilityListKey, PersistentDataType.STRING, abilitySet.toJson())
            set(keyChain.livesKey, PersistentDataType.INTEGER, livesNumber)
        }
    }

    private fun MutableSet<Abilities>.filterConflicts(): MutableSet<Abilities> {
        if(contains(Abilities.FLYING) && contains(Abilities.MOUNTED)) {
            if(random.nextInt(2) == 0) remove(Abilities.FLYING)
            else remove(Abilities.MOUNTED)
            add(Abilities.values.filter { it != Abilities.FLYING && it != Abilities.MOUNTED }.random())
        }
        return this
    }

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
        val infernalType = infernalMobTypesRepo.getInfernalTypeOrNull(savedType) ?: run {
            unmakeInfernalMob(entity)
            return
        }
        startParticleEmissionTask(entity, infernalType)
    }

    private fun startParticleEmissionTask(entity: LivingEntity, infernalType: InfernalMobType) {
        val delay = delayBetweenParticleEmission
        val job = CoroutineScope(Dispatchers.Default).launch {
            println("particleSpread = $particleSpread, delayBetweenParticleEmission = $delayBetweenParticleEmission, particleAmount = ${config.get<Int>(ConfigKeys.INFERNAL_PARTICLES_AMOUNT)}")
            while(isActive) {
                particlesHelper.sendParticle(entity, Particle.LAVA, particleSpread)
                delay(delay)
            }
        }
        infernalMobPeriodicTasks.put(entity.uniqueId, job)
    }

    private val delayBetweenParticleEmission
        get() = (max(0.01, config.get(ConfigKeys.DELAY_BETWEEN_INFERNAL_PARTICLES)) * 1000.0).toLong()

    private val particleSpread
        get() = config.get<Double>(ConfigKeys.INFERNAL_PARTICLES_SPREAD)

    fun loadAllInfernals() {
        Bukkit.getWorlds().forEach { world ->
            world.livingEntities.filter { isValidInfernalMob(it) }.forEach {
                loadInfernalMob(it)
                (it as? Mob)?.target?.let { target -> startTargetTasks(it, target) }
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

    fun triggerOnDamageDoneAbilities(event: InfernalDamageDoneEvent) = abilityHelper.triggerOnDamageDoneAbilities(event)

    fun triggerOnDamageTakenAbilities(event: InfernalDamageTakenEvent) = abilityHelper.triggerOnDamageTakenAbilities(event)

    fun triggerOnDeathAbilities(entity: LivingEntity) = abilityHelper.triggerOnDeathAbilities(entity)

    private fun cancelAllInfernalTasks(entity: LivingEntity) {
        infernalMobPeriodicTasks[entity.uniqueId].forEach { it.cancel() }
        infernalMobPeriodicTasks.removeAll(entity.uniqueId)
        infernalMobTargetTasks[entity.uniqueId].forEach { it.cancel() }
        infernalMobTargetTasks.removeAll(entity.uniqueId)
    }

    fun startTargetTasks(entity: LivingEntity, target: LivingEntity) = abilityHelper.startTargetTasks(entity, target, infernalMobTargetTasks)

    fun cancelTargetTasks(entity: LivingEntity) {
        infernalMobTargetTasks[entity.uniqueId].forEach { it.cancel() }
        infernalMobTargetTasks.removeAll(entity.uniqueId)
    }

    private fun LivingEntity.getAbilities(): Set<Abilities>? = pdc.get(keyChain.abilityListKey, PersistentDataType.STRING)?.toAbilitySet()

    private fun Set<Abilities>.toJson() = gson.toJson(this, infernalAbilitySetToken)

    private fun String.toAbilitySet() = gson.fromJson<Set<Abilities>>(this, infernalAbilitySetToken)

    private companion object {
        val gson = Gson()
        val random = Random()
        val infernalAbilitySetToken: Type = object : TypeToken<Set<Abilities>>() {}.type
    }
}
