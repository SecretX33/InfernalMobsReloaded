package com.github.secretx33.infernalmobsreloaded.manager

import com.cryptomorin.xseries.particles.ParticleDisplay
import com.cryptomorin.xseries.particles.XParticle
import com.github.secretx33.infernalmobsreloaded.config.Config
import com.github.secretx33.infernalmobsreloaded.config.ConfigKeys
import com.github.secretx33.infernalmobsreloaded.model.Ability
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.entity.LivingEntity
import org.bukkit.util.Vector
import toothpick.InjectConstructor
import javax.inject.Singleton

@Singleton
@InjectConstructor
class ParticlesHelper(private val config: Config) {

    fun sendParticle(loc: Location, particle: Particle, spread: Double, amount: Int = particleAmount) {
        if(!globalEffectsEnabled) return
        val world = loc.world ?: return

        // World#spawnParticle(Particle particle, double x, double y, double z, int count, double offsetX, double offsetY, double offsetZ)
        world.spawnParticle(particle, loc.x, loc.y, loc.z, amount, spread, 1.0, spread)
    }

    fun sendParticle(entity: LivingEntity, particle: Particle, spread: Double, amount: Int = particleAmount) {
        if(!globalEffectsEnabled) return
        val loc = entity.eyeLocation

        // World#spawnParticle(Particle particle, double x, double y, double z, int count, double offsetX, double offsetY, double offsetZ)
        entity.world.spawnParticle(particle, loc.x, loc.y, loc.z, amount, spread, 1.0, spread)
    }

    fun sendParticle(entity: LivingEntity, ability: Ability) {
        CoroutineScope(Dispatchers.Default).launch {
            when(ability) {
                Ability.SECOND_WIND -> XParticle.circle(entity.width * 1.2, entity.width * 1.5, 0.5, 1.0, 2.0, particleDisplay(Particle.TOTEM, entity.location, 20, defaultOffsetVector))
                Ability.THORNMAIL -> XParticle.filledCircle(1.0, 0.5, 0.5, particleDisplay(Particle.WARPED_SPORE, entity.location.add(0.0, entity.height * 0.5, 0.0), 100, defaultOffsetVector))
                else -> {}
            }
        }
    }

    private fun particleDisplay(particle: Particle, location: Location?, count: Int, offset: Vector): ParticleDisplay =
        ParticleDisplay.simple(location, particle).withCount(count).offset(offset)

    private val globalEffectsEnabled
        get() = config.get<Boolean>(ConfigKeys.ENABLE_GLOBAL_PARTICLE_EFFECTS)

    private val particleAmount
        get() = config.get<Int>(ConfigKeys.INFERNAL_PARTICLES_AMOUNT)

    private companion object {
        val defaultOffsetVector = Vector(0.25, 0.25, 0.25)
    }
}
