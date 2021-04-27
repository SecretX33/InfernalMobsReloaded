//package com.github.secretx33.infernalmobsreloaded.packets
//
//import com.comphenix.protocol.PacketType
//import com.comphenix.protocol.events.PacketContainer
//import com.comphenix.protocol.wrappers.BlockPosition
//import com.comphenix.protocol.wrappers.WrappedBlockData
//import org.bukkit.Location
//import org.bukkit.World
//
//
//class WrapperPlayServerBlockChange : AbstractPacket {
//
//    constructor() : super(PacketContainer(TYPE), TYPE) {
//        handle.modifier.writeDefaults()
//    }
//
//    constructor(packet: PacketContainer) : super(packet, TYPE)
//
//
//    var location: BlockPosition
//        /**
//         * Retrieve Location.
//         *
//         * Notes: block Coordinates
//         *
//         * @return The current Location
//         */
//        get() = handle.blockPositionModifier.read(0)
//        /**
//         * Set Location.
//         *
//         * @param value - new value.
//         */
//        set(value) {
//            handle.blockPositionModifier.write(0, value)
//        }
//
//    /**
//     * Retrieve the Bukkit Location.
//     *
//     * @param world World for the location
//     * @return Bukkit Location
//     */
//    fun getBukkitLocation(world: World): Location {
//        return location.toVector().toLocation(world)
//    }
//
//    var blockData: WrappedBlockData
//        /**
//         * Retrieve Block Data.
//         *
//         * @return The current Block Data
//         */
//        get() = handle.blockData.read(0)
//        /**
//         * Set Block Data.
//         *
//         * @param value - new value.
//         */
//        set(value) {
//            handle.blockData.write(0, value)
//        }
//
//    override fun toString() = "Wrapper:{type = ${TYPE.name()}, location = $location, blockType = ${blockData.type}}"
//
//    companion object {
//        val TYPE: PacketType = PacketType.Play.Server.BLOCK_CHANGE
//    }
//}
