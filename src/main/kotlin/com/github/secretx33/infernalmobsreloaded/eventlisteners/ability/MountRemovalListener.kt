package com.github.secretx33.infernalmobsreloaded.eventlisteners.ability

import com.github.secretx33.infernalmobsreloaded.model.KeyChain
import com.github.secretx33.infernalmobsreloaded.utils.extension.pdc
import org.bukkit.Bukkit
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin

class MountRemovalListener(plugin: Plugin, private val keyChain: KeyChain) : Listener {

    init { Bukkit.getPluginManager().registerEvents(this, plugin) }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun EntityDeathEvent.onMountedMobDeath() {
        val mount = entity.vehicle as? LivingEntity ?: return
        if (!mount.isBatMount()) return
        mount.remove()  // despawn bat mount (flying ability) if the rider dies
    }

    private fun LivingEntity.isBatMount() = pdc.has(keyChain.infernalBatMountKey, PersistentDataType.SHORT)
}
