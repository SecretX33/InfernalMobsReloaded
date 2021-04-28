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
import org.koin.core.component.KoinApiExtension
import kotlin.math.max
import kotlin.math.min

@KoinApiExtension
class InfernalSpawnListener (
    plugin: Plugin,
    private val config: Config,
    private val messages: Messages,
    private val mobsManager: InfernalMobsManager,
): Listener {

    init { Bukkit.getPluginManager().registerEvents(this, plugin) }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun InfernalSpawnEvent.onInfernalSpawn() {
        mobsManager.makeInfernalMob(this)
        // TODO("Apply the right effects to this mob and start all its scheduled tasks")
        sendSpawnMessage()
    }

    private fun InfernalSpawnEvent.sendSpawnMessage() {
        if(!spawnMessageEnabled) return
        val msg = spawnMessages.shuffled().firstOrNull() ?: return
        val range = max(0, messageRange).toDouble()
        entity.getNearbyEntities(range, range, range).forEach {
            it.sendMessage(msg)
        }
    }

    private val spawnMessageEnabled
        get() = config.get<Boolean>(ConfigKeys.ENABLE_INFERNO_SPAWN_MESSAGE)

    private val spawnMessages
        get() = messages.getList(MessageKeys.INFERNAL_MOB_SPAWN_MESSAGES)

    private val messageRange
        get() = config.get<Int>(ConfigKeys.INFERNO_SPAWN_MESSAGE_RADIUS)
}
