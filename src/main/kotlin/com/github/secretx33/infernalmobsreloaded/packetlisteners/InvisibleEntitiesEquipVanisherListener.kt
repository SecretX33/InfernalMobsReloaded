package com.github.secretx33.infernalmobsreloaded.packetlisteners

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.events.ListenerPriority
import com.comphenix.protocol.events.PacketAdapter
import com.comphenix.protocol.events.PacketEvent
import com.comphenix.protocol.wrappers.Pair
import com.github.secretx33.infernalmobsreloaded.config.AbilityConfig
import com.github.secretx33.infernalmobsreloaded.config.AbilityConfigKeys
import com.github.secretx33.infernalmobsreloaded.manager.InfernalMobsManager
import com.github.secretx33.infernalmobsreloaded.model.Ability
import com.github.secretx33.infernalmobsreloaded.packets.WrapperPlayServerEntityEquipment
import org.bukkit.Material
import org.bukkit.entity.LivingEntity
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import org.koin.core.component.KoinApiExtension

@KoinApiExtension
class InvisibleEntitiesEquipVanisherListener (
    private val plugin: Plugin,
    private val abilityConfig: AbilityConfig,
    private val mobsManager: InfernalMobsManager,
) {

    init { setup() }

    private fun setup() {
        val manager = ProtocolLibrary.getProtocolManager()

        manager.addPacketListener(object : PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.ENTITY_EQUIPMENT) {
            override fun onPacketSending(event: PacketEvent) {
                if(event.packetType != PacketType.Play.Server.ENTITY_EQUIPMENT || event.isCancelled || !disableEquipVisibility) return

                val wrapper = WrapperPlayServerEntityEquipment(event.packet)
                val entity = wrapper.getEntity(event) as? LivingEntity ?: return
                // if entity is not invisible inferno, no need to alter its equipments
                if(!entity.isInvisibleInfernal()) return

                // setting air as item in all lists pairs of slot <-> item for the invisible infernal
                wrapper.handle.slotStackPairLists.apply {
                    (0 until values.size).forEach { i -> modify(i) { list -> list?.map { Pair(it.first, ItemStack(Material.AIR)) } } }
                }
            }
        })
    }

    private fun LivingEntity.isInvisibleInfernal() = mobsManager.isValidInfernalMob(this) && mobsManager.hasAbility(this, Ability.INVISIBLE)

    private val disableEquipVisibility
        get() = abilityConfig.get<Boolean>(AbilityConfigKeys.INVISIBLE_DISABLE_EQUIPMENT_VISIBLITY)

    private val disableEntitySounds
        get() = abilityConfig.get<Boolean>(AbilityConfigKeys.INVISIBLE_DISABLE_ENTITY_SOUNDS)
}
