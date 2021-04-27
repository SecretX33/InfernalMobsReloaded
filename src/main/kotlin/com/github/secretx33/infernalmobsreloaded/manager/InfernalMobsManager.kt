package com.github.secretx33.infernalmobsreloaded.manager

import com.github.secretx33.infernalmobsreloaded.events.InfernalMobSpawnEvent
import com.github.secretx33.infernalmobsreloaded.model.InfernalMob
import org.bukkit.plugin.Plugin
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class InfernalMobsManager(private val plugin: Plugin) {

    private val infernalMobs = ConcurrentHashMap<UUID, InfernalMob>()

    fun makeInfernalMob(event: InfernalMobSpawnEvent) {
        val entity = event.entity
        val infernalType = event.infernalType

        entity.apply {
            isPersistent = true
            customName(infernalType.displayName)
            isCustomNameVisible = true
        }

        val infernalMob =  InfernalMob(entity.uniqueId, infernalType)
        infernalMobs[entity.uniqueId] = infernalMob
        // TODO("add to database this mob information")
    }
}
