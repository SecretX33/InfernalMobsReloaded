package com.github.secretx33.infernalmobsreloaded.manager

import com.github.secretx33.infernalmobsreloaded.config.Config
import com.github.secretx33.infernalmobsreloaded.config.ConfigKeys
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.entity.LivingEntity
import org.koin.core.component.KoinApiExtension
import java.util.logging.Logger

@KoinApiExtension
class ParticlesHelper(private val config: Config, private val log: Logger) {

    fun sendParticle(loc: Location, particle: Particle, range: Double) {
        if(!globalEffectsEnabled) return
        val world = loc.world ?: return

        // World#spawnParticle(Particle particle, double x, double y, double z, int count, double offsetX, double offsetY, double offsetZ)
        world.spawnParticle(particle, loc.x, loc.y, loc.z, 10, range, 1.0, range)
    }

    fun sendParticle(entity: LivingEntity, particle: Particle, range: Double) {
        if(!globalEffectsEnabled) return
        val loc = entity.eyeLocation

        // World#spawnParticle(Particle particle, double x, double y, double z, int count, double offsetX, double offsetY, double offsetZ)
        entity.world.spawnParticle(particle, loc.x, loc.y, loc.z, 10, range, 1.0, range)
    }

    private val globalEffectsEnabled
        get() = config.get<Boolean>(ConfigKeys.ENABLE_PARTICLE_EFFECTS)
}
