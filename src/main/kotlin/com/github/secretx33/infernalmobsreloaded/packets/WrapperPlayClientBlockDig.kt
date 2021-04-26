package com.github.secretx33.infernalmobsreloaded.packets

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.events.PacketContainer
import com.comphenix.protocol.wrappers.BlockPosition
import com.comphenix.protocol.wrappers.EnumWrappers
import com.comphenix.protocol.wrappers.EnumWrappers.PlayerDigType


class WrapperPlayClientBlockDig : AbstractPacket {
    constructor() : super(PacketContainer(TYPE), TYPE) {
        handle.modifier.writeDefaults()
    }

    constructor(packet: PacketContainer) : super(packet, TYPE)
    /**
     * Retrieve Location.
     *
     *
     * Notes: block position
     *
     * @return The current Location
     */
    /**
     * Set Location.
     *
     * @param value - new value.
     */
    var location: BlockPosition
        get() = handle.blockPositionModifier.read(0)
        set(value) {
            handle.blockPositionModifier.write(0, value)
        }
    var direction: EnumWrappers.Direction
        get() = handle.directions.read(0)
        set(value) {
            handle.directions.write(0, value)
        }
    /**
     * Retrieve Status.
     *
     *
     * Notes: the action the player is taking against the block (see below)
     *
     * @return The current Status
     */
    /**
     * Set Status.
     *
     * @param value - new value.
     */
    var status: PlayerDigType
        get() = handle.playerDigTypes.read(0)
        set(value) {
            handle.playerDigTypes.write(0, value)
        }

    override fun toString() = "Wrapper:{type = ${TYPE.name()}, location = $location, status = ${status.name}, direction = ${direction.name}}"

    companion object {
        val TYPE: PacketType = PacketType.Play.Client.BLOCK_DIG
    }
}
