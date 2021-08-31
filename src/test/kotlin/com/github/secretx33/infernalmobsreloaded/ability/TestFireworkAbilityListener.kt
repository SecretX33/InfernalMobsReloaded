package com.github.secretx33.infernalmobsreloaded.ability

import be.seeseemelk.mockbukkit.MockBukkit
import com.github.secretx33.infernalmobsreloaded.config.AbilityConfig
import com.github.secretx33.infernalmobsreloaded.config.AbilityConfigKeys
import com.github.secretx33.infernalmobsreloaded.config.Config
import com.github.secretx33.infernalmobsreloaded.config.ConfigKeys
import com.github.secretx33.infernalmobsreloaded.eventlisteners.ability.FireworkAbilityListener
import com.github.secretx33.infernalmobsreloaded.model.KeyChain
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.Firework
import org.bukkit.entity.LivingEntity
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import utils.callEventMethods
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue


class TestFireworkAbilityListener {

    private val server = MockBukkit.mock()
    private val plugin = MockBukkit.createMockPlugin()

    private val keyChain = mock<KeyChain>()

    @AfterAll
    fun unload() {
        MockBukkit.unmock()
    }

    private companion object {
        const val EVENT_DEFAULT_DAMAGE = 10.0
    }

    private fun createListener(
        config: Config = mock(),
        abilityConfig: AbilityConfig = mock(),
        damageMulti: Double = 1.0,
    ): FireworkAbilityListener {
        val pairDmgMulti = Pair(damageMulti, damageMulti)
        whenever(config.get<Boolean>(ConfigKeys.INFERNALS_CANNOT_DAMAGE_THEMSELVES)).thenReturn(true)
        whenever(abilityConfig.getDoublePair(AbilityConfigKeys.FIREWORK_DAMAGE_MULTIPLIER)).thenReturn(pairDmgMulti)
        return FireworkAbilityListener(plugin, config, abilityConfig, keyChain)
    }

    private fun createEvent(
        attacker: Entity = mockedLivingEntity(),
        defender: Entity = mockedLivingEntity(),
        cause: EntityDamageEvent.DamageCause = EntityDamageEvent.DamageCause.ENTITY_EXPLOSION,
        damage: Double = EVENT_DEFAULT_DAMAGE,
    ) = EntityDamageByEntityEvent(attacker, defender, cause, damage)

    private fun mockFirework(owner: UUID? = null): Firework {
        val pdc = mock<PersistentDataContainer> {
            on { has(keyChain.fireworkOwnerUuidKey, PersistentDataType.STRING) } doReturn (owner != null)
            on { get(keyChain.fireworkOwnerUuidKey, PersistentDataType.STRING) } doReturn owner?.toString()
        }
        val firework = mock<Firework> {
            on { type } doReturn EntityType.FIREWORK
            on { persistentDataContainer } doReturn pdc
        }
        return firework
    }

    private fun mockedEntity(): Entity = mock {
        on { uniqueId } doReturn UUID.randomUUID()
    }

    private fun mockedLivingEntity(): LivingEntity = mock {
        on { uniqueId } doReturn UUID.randomUUID()
    }

    @Nested
    inner class EventCancel {

        @Test
        fun `Firework should not be able to damage its owner`(){
            val infernal = mock<LivingEntity> {
                on { uniqueId } doReturn UUID.randomUUID()
            }
            val firework = mockFirework(infernal.uniqueId)
            val event = createEvent(attacker = firework, defender = infernal)
            val listener = createListener()

            listener.callEventMethods(event)
            assertTrue(event.isCancelled, "Event should be cancelled")
            assertEquals(event.damage, 0.0)
        }

        @Test
        fun `Firework should still be able to damage its anyone that is not the owner or its mount`(){
            val firework = mockFirework(UUID.randomUUID())
            val event = createEvent(attacker = firework)
            val listener = createListener()

            listener.callEventMethods(event)
            assertFalse(event.isCancelled, "Event should NOT be cancelled")
            assertEquals(EVENT_DEFAULT_DAMAGE, event.damage, "Event damage should not be modified")
        }
    }

    @Nested
    inner class DamageMultiplier {

        @Test
        fun `Given damage multiplier of 2, event damage should be doubled`() {
            val firework = mockFirework(UUID.randomUUID())
            val event = createEvent(attacker = firework)
            val listener = createListener(damageMulti = 2.0)

            assertEquals(EVENT_DEFAULT_DAMAGE, event.damage)
            listener.callEventMethods(event)
            assertEquals(EVENT_DEFAULT_DAMAGE * 2, event.damage, "Event damage should be doubled after calling listener method 'onFireworkExplosion'")
        }

        @Test
        fun `Given defender is not a living entity, event damage should stay the same`() {
            val firework = mockFirework(UUID.randomUUID())
            val entity = mockedEntity()
            val event = createEvent(attacker = firework, defender = entity)
            val listener = createListener(damageMulti = 2.0)

            assertTrue(entity !is LivingEntity)
            assertEquals(EVENT_DEFAULT_DAMAGE, event.damage)
            listener.callEventMethods(event)
            assertEquals(EVENT_DEFAULT_DAMAGE, event.damage, "Event damage should remain the same since 'defender' is not a living entity")
        }

        @Test
        fun `Given attacker is not a firework, event damage should stay the same`() {
            val event = createEvent()
            val listener = createListener(damageMulti = 5.0)

            assertEquals(EVENT_DEFAULT_DAMAGE, event.damage)
            listener.callEventMethods(event)
            assertEquals(EVENT_DEFAULT_DAMAGE, event.damage, "Event damage should remain the same since 'attacker' is not a firework")
        }

        @Test
        fun `Given attacker is a firework that doesn't have any infernal owner tag, event damage should stay the same`() {
            val firework = mockFirework()
            val event = createEvent(attacker = firework)
            val listener = createListener(damageMulti = 4.0)

            assertEquals(EVENT_DEFAULT_DAMAGE, event.damage)
            listener.callEventMethods(event)
            assertEquals(EVENT_DEFAULT_DAMAGE, event.damage, "Event damage should remain the same since 'attacker' is a firework without infernal owner tag")
        }
    }
}
