package com.github.secretx33.infernalmobsreloaded.eventlisteners.infernalmobs

import com.github.secretx33.infernalmobsreloaded.config.Config
import com.github.secretx33.infernalmobsreloaded.config.ConfigKeys
import com.github.secretx33.infernalmobsreloaded.config.MessageKeys
import com.github.secretx33.infernalmobsreloaded.config.Messages
import com.github.secretx33.infernalmobsreloaded.events.InfernalDeathEvent
import com.github.secretx33.infernalmobsreloaded.manager.BossBarManager
import com.github.secretx33.infernalmobsreloaded.manager.InfernalMobsManager
import com.github.secretx33.infernalmobsreloaded.manager.ParticlesHelper
import com.github.secretx33.infernalmobsreloaded.model.Abilities
import org.bukkit.Bukkit
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin
import org.koin.core.component.KoinApiExtension
import kotlin.math.max

@KoinApiExtension
class InfernalDeathListener (
    plugin: Plugin,
    private val config: Config,
    private val messages: Messages,
    private val particlesHelper: ParticlesHelper,
    private val mobsManager: InfernalMobsManager,
    private val bossBarManager: BossBarManager,
): Listener {

    init { Bukkit.getPluginManager().registerEvents(this, plugin) }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    private fun InfernalDeathEvent.onInfernalDeath() {
        val lives = entity.getLives()
        // if entity got more than one life, cancel its death and subtract one life
        if(lives > 1) {
            isCancelled = true
            particlesHelper.sendParticle(entity, Abilities.SECOND_WING)
            mobsManager.setLives(entity, lives - 1)
            return
        }
        mobsManager.triggerOnDeathAbilities(entity)
        mobsManager.unloadInfernalMob(entity)
        bossBarManager.removeBossBar(entity)
        // drop infernal rewards on infernal mob's body (along with normal mob loot)
        infernalType.getLoots().forEach {
            world.dropItemNaturally(entity.location, it)
        }
        sendDeathMessage()
    }

    private fun InfernalDeathEvent.sendDeathMessage() {
        if(!deathMessageEnabled) return
        val msg = deathMessages.shuffled().firstOrNull() ?: return
        val range = max(0, messageRange).toDouble()
        entity.getNearbyEntities(range, range, range).forEach {
            it.sendMessage(msg)
        }
    }

    private fun LivingEntity.getLives() = mobsManager.getLives(this)

    private val deathMessageEnabled get() = config.get<Boolean>(ConfigKeys.ENABLE_INFERNAL_DEATH_MESSAGE)
    private val deathMessages get() = messages.getList(MessageKeys.INFERNAL_MOB_DEATH_MESSAGES)
    private val messageRange get() = config.getInt(ConfigKeys.INFERNAL_DEATH_MESSAGE_RADIUS)
}
