package com.github.secretx33.infernalmobsreloaded.eventlistener.infernalmobs

import com.github.secretx33.infernalmobsreloaded.config.Config
import com.github.secretx33.infernalmobsreloaded.config.ConfigKeys
import com.github.secretx33.infernalmobsreloaded.config.MessageKeys
import com.github.secretx33.infernalmobsreloaded.config.Messages
import com.github.secretx33.infernalmobsreloaded.config.replace
import com.github.secretx33.infernalmobsreloaded.event.InfernalDeathEvent
import com.github.secretx33.infernalmobsreloaded.manager.BossBarManager
import com.github.secretx33.infernalmobsreloaded.manager.InfernalMobsManager
import com.github.secretx33.infernalmobsreloaded.manager.ParticlesHelper
import com.github.secretx33.infernalmobsreloaded.model.Ability
import com.github.secretx33.infernalmobsreloaded.util.extension.random
import com.github.secretx33.infernalmobsreloaded.util.extension.turnIntoSpawner
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack
import toothpick.InjectConstructor
import kotlin.math.max

@InjectConstructor
class InfernalDeathListener(
    private val config: Config,
    private val messages: Messages,
    private val particlesHelper: ParticlesHelper,
    private val mobsManager: InfernalMobsManager,
    private val bossBarManager: BossBarManager,
) : Listener {

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    private fun InfernalDeathEvent.onInfernalPossibleDeath() {
        val lives = entity.getLives()
        if (lives <= 1) return
        // if entity got more than one life, cancel its death and subtract one life
        isCancelled = true
        triggerSecondWind(lives)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun InfernalDeathEvent.onInfernalDeath() {
        mobsManager.triggerOnDeathAbilities(entity)
        mobsManager.unloadInfernalMob(entity)
        bossBarManager.removeBossBar(entity)
        // drop infernal rewards on infernal mob's body (along with normal mob loot)
        dropMobLoots()
        dropMobSpawner()
        sendDeathMessage()
        consoleCommmands()
    }

    private fun InfernalDeathEvent.triggerSecondWind(lives: Int) {
        particlesHelper.sendParticle(entity, Ability.SECOND_WIND)
        mobsManager.setLives(entity, lives - 1)
        bossBarManager.updateBossBar(entity, 1f)
    }

    private fun InfernalDeathEvent.dropMobLoots() {
        infernalType.getLoots().forEach {
            world.dropItemNaturally(entity.location, it)
        }
    }

    private fun InfernalDeathEvent.dropMobSpawner() {
        if (!dropSpawners || random.nextDouble() > infernalType.mobSpawnerDropChance) return
        val spawner = ItemStack(Material.SPAWNER).turnIntoSpawner(infernalType)
        world.dropItemNaturally(entity.location, spawner)
    }

    private fun InfernalDeathEvent.sendDeathMessage() {
        if (!deathMessageEnabled) return
        val player = entity.killer ?: return
        val msg = deathMessages.randomOrNull()?.replace("<player>", player.displayName()) ?: return
        val range = max(0, messageRange).toDouble()

        entity.getNearbyEntities(range, range, range).forEach {
            it.sendMessage(msg)
        }
    }

    private fun InfernalDeathEvent.consoleCommmands() {
        val player = entity.killer ?: return
        infernalType.consoleCommands.forEach {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), it.replace("<player>", player.name))
        }
    }

    private fun LivingEntity.getLives() = mobsManager.getLives(this)

    private val dropSpawners
        get() = config.get<Boolean>(ConfigKeys.ENABLE_SPAWNER_DROPS)

    private val deathMessageEnabled get() = config.get<Boolean>(ConfigKeys.ENABLE_INFERNAL_DEATH_MESSAGE)
    private val deathMessages get() = messages.getList(MessageKeys.INFERNAL_MOB_DEATH_MESSAGES)
    private val messageRange get() = config.getInt(ConfigKeys.INFERNAL_DEATH_MESSAGE_RADIUS)
}
