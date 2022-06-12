package com.github.secretx33.infernalmobsreloaded.eventlistener.infernalmobs

import com.github.secretx33.infernalmobsreloaded.event.InfernalDamageDoneEvent
import com.github.secretx33.infernalmobsreloaded.manager.InfernalMobsManager
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import toothpick.InjectConstructor

@InjectConstructor
class InfernalDamageDoneListener(private val mobsManager: InfernalMobsManager) : Listener {

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    private fun InfernalDamageDoneEvent.onInfernalDamageDone() {
        mobsManager.triggerOnDamageDoneAbilities(this)
        damageMulti *= infernalType.getDamageMulti()
    }
}
