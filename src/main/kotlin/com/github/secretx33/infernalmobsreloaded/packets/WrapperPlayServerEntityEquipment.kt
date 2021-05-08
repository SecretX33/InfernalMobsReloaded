package com.github.secretx33.infernalmobsreloaded.packets

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.events.PacketContainer
import com.comphenix.protocol.events.PacketEvent
import com.comphenix.protocol.wrappers.EnumWrappers.ItemSlot
import org.bukkit.World
import org.bukkit.entity.Entity
import org.bukkit.inventory.ItemStack

class WrapperPlayServerEntityEquipment : AbstractPacket {

    constructor() : super(PacketContainer(TYPE), TYPE) {
        handle.modifier.writeDefaults()
    }

    constructor(packet: PacketContainer) : super(packet, TYPE) {}

    var entityID: Int
        /**
         * Retrieve Entity ID.
         *
         * Notes: entity's ID
         *
         * @return The current Entity ID
         */
        get() = handle.integers.read(0)
        /**
         * Set Entity ID.
         *
         * @param value - new value.
         */
        set(value) {
            handle.integers.write(0, value)
        }

    /**
     * Retrieve the entity of the painting that will be spawned.
     *
     * @param world - the current world of the entity.
     * @return The spawned entity.
     */
    fun getEntity(world: World): Entity? = handle.getEntityModifier(world).read(0)

    /**
     * Retrieve the entity of the painting that will be spawned.
     *
     * @param event - the packet event.
     * @return The spawned entity.
     */
    fun getEntity(event: PacketEvent): Entity? = getEntity(event.player.world)

    var slot: ItemSlot
        get() = handle.itemSlots.read(0)
        set(value) {
            handle.itemSlots.write(0, value)
        }


    var item: ItemStack?
        /**
         * Retrieve Item.
         *
         * Notes: item in slot format
         *
         * @return The current Item
         */
        get() = handle.itemModifier.read(0)
        /**
         * Set Item.
         *
         * @param value - new value.
         */
        set(value) {
            handle.itemModifier.write(0, value)
        }

    companion object {
        val TYPE: PacketType = PacketType.Play.Server.ENTITY_EQUIPMENT
    }
}
