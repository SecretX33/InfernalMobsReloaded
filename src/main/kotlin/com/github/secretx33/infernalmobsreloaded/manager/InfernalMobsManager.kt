package com.github.secretx33.infernalmobsreloaded.manager

import com.github.secretx33.infernalmobsreloaded.events.InfernalSpawnEvent
import com.github.secretx33.infernalmobsreloaded.model.InfernalAbility
import com.github.secretx33.infernalmobsreloaded.model.InfernalMobType
import com.github.secretx33.infernalmobsreloaded.model.KeyChain
import com.github.secretx33.infernalmobsreloaded.repositories.InfernalMobTypesRepo
import com.github.secretx33.infernalmobsreloaded.utils.pdc
import com.google.common.collect.MultimapBuilder
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Job
import org.bukkit.entity.LivingEntity
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin
import java.util.*

class InfernalMobsManager (
    private val plugin: Plugin,
    private val keyChain: KeyChain,
    private val infernalMobTypesRepo: InfernalMobTypesRepo,
) {

    private val infernalMobTasks = MultimapBuilder.hashKeys().arrayListValues().build<UUID, Job>()

    fun isInfernalMob(entity: LivingEntity) = entity.pdc.get(keyChain.infernalCategoryKey, PersistentDataType.STRING)?.let { infernalMobTypesRepo.isValidInfernoType(it) } == true

    fun makeInfernalMob(event: InfernalSpawnEvent) {
        val entity = event.entity
        val infernalType = event.infernalType

        addCustomNameToInfernal(entity, infernalType)
        addPdcKeysToInfernal(entity, infernalType)
    }

    private fun addCustomNameToInfernal(entity: LivingEntity, infernalType: InfernalMobType) {
        entity.apply {
            isPersistent = true
            customName(infernalType.displayName)
            isCustomNameVisible = true
        }
    }

    private fun addPdcKeysToInfernal(entity: LivingEntity, infernalType: InfernalMobType) {
        val abilityList = InfernalAbility.random(infernalType.getAbilityNumber())
        val livesNumber = if(abilityList.contains(InfernalAbility.SECOND_WING)) 2 else 1

        entity.pdc.apply {
            set(keyChain.infernalCategoryKey, PersistentDataType.STRING, infernalType.name)
            set(keyChain.abilityListKey, PersistentDataType.STRING, abilityList.toJson())
            set(keyChain.livesKey, PersistentDataType.INTEGER, livesNumber)
        }
    }

    fun loadInfernalMob(entity: LivingEntity) {
        startAllTasks(entity)
    }

    private fun startAllTasks(entity: LivingEntity) {
        // TODO("make tasks")
    }

    fun unloadInfernalMob(entity: LivingEntity) {
        cancelAllTasks(entity)
    }

    private fun cancelAllTasks(entity: LivingEntity) {
        // TODO("cancel tasks")
    }

    private fun List<InfernalAbility>.toJson() = gson.toJson(this, infernalAbilityListToken)

    private fun String.toAbilityList() = gson.fromJson<List<InfernalAbility>>(this, infernalAbilityListToken)

    private companion object {
        val random = Random()
        val gson = Gson()
        val infernalAbilityListToken = object : TypeToken<List<InfernalAbility>>() {}.type
    }
}
