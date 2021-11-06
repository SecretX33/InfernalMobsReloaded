package com.github.secretx33.infernalmobsreloaded.eventlisteners.infernalmobs

import com.github.secretx33.infernalmobsreloaded.config.Config
import com.github.secretx33.infernalmobsreloaded.config.ConfigKeys
import com.github.secretx33.infernalmobsreloaded.config.MessageKeys
import com.github.secretx33.infernalmobsreloaded.config.Messages
import com.github.secretx33.infernalmobsreloaded.events.InfernalSpawnEvent
import com.github.secretx33.infernalmobsreloaded.manager.InfernalMobsManager
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin
import kotlin.math.max

class InfernalSpawnListener (
    plugin: Plugin,
    private val config: Config,
    private val messages: Messages,
    private val mobsManager: InfernalMobsManager,
) : Listener {

    init { Bukkit.getPluginManager().registerEvents(this, plugin) }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun InfernalSpawnEvent.onInfernalSpawn() {
        mobsManager.makeInfernalMob(this)
        mobsManager.loadInfernalMob(entity)
        sendSpawnMessage()
    }

    private fun InfernalSpawnEvent.sendSpawnMessage() {
        if(!spawnMessageEnabled) return
        val msg = spawnMessages.randomOrNull() ?: return
        val range = max(0, messageRange).toDouble()
        entity.getNearbyEntities(range, range, range).forEach {
            it.sendMessage(msg)
        }
    }

    private val spawnMessageEnabled get() = config.get<Boolean>(ConfigKeys.ENABLE_INFERNAL_SPAWN_MESSAGE)
    private val spawnMessages get() = messages.getList(MessageKeys.INFERNAL_MOB_SPAWN_MESSAGES)
    private val messageRange get() = config.get<Int>(ConfigKeys.INFERNAL_SPAWN_MESSAGE_RADIUS)
}
