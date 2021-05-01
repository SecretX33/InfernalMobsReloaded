package com.github.secretx33.infernalmobsreloaded.eventlisteners.infernalmobs

import com.github.secretx33.infernalmobsreloaded.events.InfernalDamageTakenEvent
import com.github.secretx33.infernalmobsreloaded.manager.BossBarManager
import com.github.secretx33.infernalmobsreloaded.manager.InfernalMobsManager
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
        println("${attacker.name} attacked infernal ${entity.name} (${infernalType.name}), was event cancelled = $isCancelled")
        if(!isCancelled) bossBarManager.updateBossBar(entity)
    }
}
