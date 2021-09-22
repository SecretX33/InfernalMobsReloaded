package com.github.secretx33.infernalmobsreloaded.eventlisteners.charm

import com.github.secretx33.infernalmobsreloaded.manager.CharmsManager
import org.bukkit.Bukkit
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.plugin.Plugin

class PlayerDamageCharmListener (
    plugin: Plugin,
    private val charmsManager: CharmsManager,
) : Listener {

    init { Bukkit.getPluginManager().registerEvents(this, plugin) }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun EntityDamageByEntityEvent.onPlayerHitEntity() {
        val player = damager as? Player ?: return
        val target = entity as? LivingEntity ?: return

        if(!player.isOnCharmEnabledWorld()) return
        charmsManager.triggerOnHitCharms(player, target)
    }

    private fun Player.isOnCharmEnabledWorld() = charmsManager.areCharmsAllowedOnWorld(world)
}
