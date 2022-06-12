package com.github.secretx33.infernalmobsreloaded.eventlisteners.infernalmobs

import com.github.secretx33.infernalmobsreloaded.events.InfernalDamageTakenEvent
import com.github.secretx33.infernalmobsreloaded.manager.InfernalMobsManager
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import toothpick.InjectConstructor

@InjectConstructor
class InfernalDamageTakenListener(private val mobsManager: InfernalMobsManager) : Listener {

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    private fun InfernalDamageTakenEvent.onInfernalDamageTaken() {
        mobsManager.triggerOnDamageTakenAbilities(this)
    }
}
