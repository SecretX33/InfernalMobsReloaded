package com.github.secretx33.infernalmobsreloaded.events

import com.github.secretx33.infernalmobsreloaded.model.Abilities
import com.github.secretx33.infernalmobsreloaded.model.InfernalMobType
import org.bukkit.entity.LivingEntity
import org.bukkit.event.Cancellable
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason

class InfernalSpawnEvent (
    entity: LivingEntity,
    infernalType: InfernalMobType,
    val spawnReason: SpawnReason = SpawnReason.CUSTOM,
    val randomAbilities: Boolean = true,
    val abilitySet: Set<Abilities> = emptySet(),
) : InfernalEntityEvent(entity, infernalType), Cancellable {

    private var isCancelled = false

    override fun isCancelled(): Boolean = isCancelled

    override fun setCancelled(isCancelled: Boolean) {
        this.isCancelled = isCancelled
    }
}
