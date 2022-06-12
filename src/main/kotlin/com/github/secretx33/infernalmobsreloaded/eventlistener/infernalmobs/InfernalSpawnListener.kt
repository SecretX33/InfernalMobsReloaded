package com.github.secretx33.infernalmobsreloaded.eventlistener.infernalmobs

import com.github.secretx33.infernalmobsreloaded.config.Config
import com.github.secretx33.infernalmobsreloaded.config.ConfigKeys
import com.github.secretx33.infernalmobsreloaded.config.MessageKeys
import com.github.secretx33.infernalmobsreloaded.config.Messages
import com.github.secretx33.infernalmobsreloaded.event.InfernalSpawnEvent
import com.github.secretx33.infernalmobsreloaded.manager.InfernalMobsManager
import net.kyori.adventure.text.Component
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import toothpick.InjectConstructor
import kotlin.math.max

@InjectConstructor
class InfernalSpawnListener (
    private val config: Config,
    private val messages: Messages,
    private val mobsManager: InfernalMobsManager,
) : Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun InfernalSpawnEvent.onInfernalSpawn() {
        mobsManager.makeInfernalMob(this)
        mobsManager.loadInfernalMob(entity)
        sendSpawnMessage()
    }

    private fun InfernalSpawnEvent.sendSpawnMessage() {
        if (!spawnMessageEnabled) return
        val msg = spawnMessages.randomOrNull() ?: return
        val range = max(0, messageRange).toDouble()
        entity.getNearbyEntities(range, range, range).forEach {
            it.sendMessage(msg)
        }
    }

    private val spawnMessageEnabled: Boolean get() = config.get(ConfigKeys.ENABLE_INFERNAL_SPAWN_MESSAGE)
    private val spawnMessages: List<Component> get() = messages.getList(MessageKeys.INFERNAL_MOB_SPAWN_MESSAGES)
    private val messageRange: Int get() = config.get(ConfigKeys.INFERNAL_SPAWN_MESSAGE_RADIUS)
}
