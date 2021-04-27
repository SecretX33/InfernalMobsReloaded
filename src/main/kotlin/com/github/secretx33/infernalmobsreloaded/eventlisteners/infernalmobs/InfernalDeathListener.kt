package com.github.secretx33.infernalmobsreloaded.eventlisteners.infernalmobs

import com.github.secretx33.infernalmobsreloaded.config.Config
import com.github.secretx33.infernalmobsreloaded.config.ConfigKeys
import com.github.secretx33.infernalmobsreloaded.config.MessageKeys
import com.github.secretx33.infernalmobsreloaded.config.Messages
import com.github.secretx33.infernalmobsreloaded.manager.InfernalMobsManager
import org.bukkit.Bukkit
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.plugin.Plugin
import org.koin.core.component.KoinApiExtension
import kotlin.math.max

@KoinApiExtension
class InfernalDeathListener (
    plugin: Plugin,
    private val config: Config,
    private val messages: Messages,
    private val mobsManager: InfernalMobsManager,
): Listener {

    init { Bukkit.getPluginManager().registerEvents(this, plugin) }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun EntityDeathEvent.onInfernalMobDeath() {
        if(!entity.isInfernalMob()) return
        // TODO("Manage second live & add custom loot")
        mobsManager.unloadInfernalMob(entity)
        sendDeathMessage()
    }

    private fun EntityDeathEvent.sendDeathMessage() {
        if(!deathMessageEnabled) return
        val msg = deathMessages.shuffled().firstOrNull() ?: return
        val range = max(0, messageRange).toDouble()
        entity.getNearbyEntities(range, range, range).forEach {
            it.sendMessage(msg)
        }
    }

    private fun LivingEntity.isInfernalMob() = mobsManager.isInfernalMob(this)

    private val deathMessageEnabled
        get() = config.get<Boolean>(ConfigKeys.ENABLE_INFERNO_DEATH_MESSAGE)

    private val deathMessages
        get() = messages.getList(MessageKeys.INFERNAL_MOB_DEATH_MESSAGES)

    private val messageRange
        get() = config.get<Int>(ConfigKeys.INFERNO_DEATH_MESSAGE_RADIUS)
}
