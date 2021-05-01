package com.github.secretx33.infernalmobsreloaded.eventlisteners.infernalmobs

import com.github.secretx33.infernalmobsreloaded.config.Config
import com.github.secretx33.infernalmobsreloaded.config.Messages
import com.github.secretx33.infernalmobsreloaded.events.InfernalDamageTakenEvent
import com.github.secretx33.infernalmobsreloaded.manager.BossBarManager
import com.github.secretx33.infernalmobsreloaded.manager.InfernalMobsManager
import com.github.secretx33.infernalmobsreloaded.manager.ParticlesHelper
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin
import org.koin.core.component.KoinApiExtension

@KoinApiExtension
class InfernalDamageTakenListener (
    plugin: Plugin,
    private val mobsManager: InfernalMobsManager,
    private val bossBarManager: BossBarManager,
): Listener {

    init { Bukkit.getPluginManager().registerEvents(this, plugin) }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    private fun InfernalDamageTakenEvent.onInfernalDamageTaken() {
        mobsManager.triggerOnDamageTakenAbilities(this)
        if(!isCancelled) bossBarManager.updateBossBar(entity)
    }
}
