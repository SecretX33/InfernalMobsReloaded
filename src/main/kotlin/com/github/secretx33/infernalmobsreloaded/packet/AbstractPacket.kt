package com.github.secretx33.infernalmobsreloaded.packet

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.events.PacketContainer
import org.bukkit.entity.Player
import java.lang.reflect.InvocationTargetException

abstract class AbstractPacket protected constructor(handle: PacketContainer, type: PacketType) {
    /**
     * Retrieve a handle to the raw packet data.
     *
     * @return Raw packet data.
     */
    // The packet we will be modifying
    var handle: PacketContainer
        protected set

    /**
     * Constructs a new strongly typed wrapper for the given packet.
     *
     * @param handle - handle to the raw packet data.
     * @param type - the packet type.
     */
    init {
        // Make sure we're given a valid packet
        require(handle.type == type) {
            handle.handle.toString() + " is not a packet of type " + type
        }
        this.handle = handle
    }

    /**
     * Send the current packet to the given receiver.
     *
     * @param receiver - the receiver.
     * @throws RuntimeException If the packet cannot be sent.
     */
    fun sendPacket(receiver: Player) {
        try {
            ProtocolLibrary.getProtocolManager().sendServerPacket(receiver, handle)
        } catch (e: InvocationTargetException) {
            throw RuntimeException("Cannot send packet.", e)
        }
    }

    /**
     * Send the current packet to all online players.
     */
    fun broadcastPacket() {
        ProtocolLibrary.getProtocolManager().broadcastServerPacket(handle)
    }

    /**
     * Simulate receiving the current packet from the given sender.
     *
     * @param sender - the sender.
     * @throws RuntimeException If the packet cannot be received.
     * @see .receivePacket
     */
    @Deprecated("""Misspelled. recieve to receive""")
    fun recievePacket(sender: Player) {
        try {
            ProtocolLibrary.getProtocolManager().recieveClientPacket(sender, handle)
        } catch (e: Exception) {
            throw RuntimeException("Cannot recieve packet.", e)
        }
    }

    /**
     * Simulate receiving the current packet from the given sender.
     *
     * @param sender - the sender.
     * @throws RuntimeException if the packet cannot be received.
     */
    fun receivePacket(sender: Player) {
        try {
            ProtocolLibrary.getProtocolManager().recieveClientPacket(sender, handle)
        } catch (e: Exception) {
            throw RuntimeException("Cannot receive packet.", e)
        }
    }
}

