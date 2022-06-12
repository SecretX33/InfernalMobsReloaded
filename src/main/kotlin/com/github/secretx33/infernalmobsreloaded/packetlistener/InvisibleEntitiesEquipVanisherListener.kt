package com.github.secretx33.infernalmobsreloaded.packetlistener

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.events.ListenerPriority
import com.comphenix.protocol.events.PacketAdapter
import com.comphenix.protocol.events.PacketEvent
import com.comphenix.protocol.wrappers.Pair
import com.github.secretx33.infernalmobsreloaded.config.AbilityConfig
import com.github.secretx33.infernalmobsreloaded.config.AbilityConfigKeys
import com.github.secretx33.infernalmobsreloaded.manager.InfernalMobsManager
import com.github.secretx33.infernalmobsreloaded.model.Ability
import com.github.secretx33.infernalmobsreloaded.packet.WrapperPlayServerEntityEquipment
import org.bukkit.Material
import org.bukkit.entity.LivingEntity
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import toothpick.InjectConstructor

@InjectConstructor
class InvisibleEntitiesEquipVanisherListener (
    plugin: Plugin,
    private val abilityConfig: AbilityConfig,
    private val mobsManager: InfernalMobsManager,
) : PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.ENTITY_EQUIPMENT) {

    override fun onPacketSending(event: PacketEvent) {
        if (event.packetType != PacketType.Play.Server.ENTITY_EQUIPMENT || event.isCancelled || !disableEquipVisibility) return

        val wrapper = WrapperPlayServerEntityEquipment(event.packet)
        val entity = wrapper.getEntity(event) as? LivingEntity ?: return
        // if entity is not invisible inferno, no need to alter its equipments
        if (!entity.isInvisibleInfernal()) return

        // setting air as item in all lists pairs of slot <-> item for the invisible infernal
        wrapper.handle.slotStackPairLists.apply {
            (0 until values.size).forEach { i -> modify(i) { list -> list?.map { Pair(it.first, ItemStack(Material.AIR)) } } }
        }
    }

    private fun LivingEntity.isInvisibleInfernal() = mobsManager.isValidInfernalMob(this) && mobsManager.hasAbility(this, Ability.INVISIBLE)

    private val disableEquipVisibility
        get() = abilityConfig.get<Boolean>(AbilityConfigKeys.INVISIBLE_DISABLE_EQUIPMENT_VISIBLITY)
}
