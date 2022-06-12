package com.github.secretx33.infernalmobsreloaded.eventlistener.charm

import com.github.secretx33.infernalmobsreloaded.manager.CharmsManager
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import toothpick.InjectConstructor

@InjectConstructor
class PlayerDamageCharmListener(private val charmsManager: CharmsManager) : Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun EntityDamageByEntityEvent.onPlayerHitEntity() {
        val player = damager as? Player ?: return
        val target = entity as? LivingEntity ?: return

        if (!player.isOnCharmEnabledWorld()) return
        charmsManager.triggerOnHitCharms(player, target)
    }

    private fun Player.isOnCharmEnabledWorld(): Boolean = charmsManager.areCharmsAllowedOnWorld(world)
}
