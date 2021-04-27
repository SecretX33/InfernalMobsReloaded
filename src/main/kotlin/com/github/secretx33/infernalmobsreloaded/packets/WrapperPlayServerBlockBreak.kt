//package com.github.secretx33.infernalmobsreloaded.packets
//
//import com.comphenix.protocol.PacketType
//import com.comphenix.protocol.events.PacketContainer
//import com.comphenix.protocol.wrappers.BlockPosition
//import com.comphenix.protocol.wrappers.EnumWrappers.PlayerDigType
//import com.comphenix.protocol.wrappers.WrappedBlockData
//
//class WrapperPlayServerBlockBreak : AbstractPacket {
//
//    constructor() : super(PacketContainer(TYPE), TYPE) {
//        handle.modifier.writeDefaults()
//    }
//
//    constructor(packet: PacketContainer) : super(packet, TYPE) {}
//
//    var location: BlockPosition
//        /**
//         * Retrieve Location.
//         *
//         * Notes: position where the digging was happening
//         * @return The current Location
//         */
//        get() = handle.blockPositionModifier.read(0)
//        /**
//         * Set Location.
//         * @param value - new value.
//         */
//        set(value) {
//            handle.blockPositionModifier.write(0, value)
//        }
//
//    var blockData: WrappedBlockData
//        /**
//         * Retrieve Block.
//         *
//         * Notes: block state ID of the block that should be at that position now.
//         * @return The current Block
//         */
//        get() = handle.blockData.read(0)
//        /**
//         * Set Block.
//         * @param value - new value.
//         */
//        set(value) {
//            handle.blockData.write(0, value)
//        }
//
//    var status: PlayerDigType
//        /**
//         * Retrieve Status.
//         *
//         * Notes: same as Player Digging. Only Started digging (0), Cancelled digging (1), and Finished digging (2) are used.
//         * @return The current Status
//         */
//        get() = handle.playerDigTypes.read(0)
//        /**
//         * Set Status.
//         * @param value - new value.
//         */
//        set(value) {
//            handle.playerDigTypes.write(0, value)
//        }
//
//    var successful: Boolean
//        /**
//         * Retrieve Successful.
//         *
//         * Notes: true if the digging succeeded; false if the client should undo any changes it made locally. (How does this work?)
//         * @return The current Successful
//         */
//        get() = handle.booleans.read(0)
//        /**
//         * Set Successful.
//         * @param value - new value.
//         */
//        set(value) {
//            handle.booleans.write(0, value)
//        }
//
//    override fun toString() = "Wrapper:{type = ${TYPE.name()}, location = $location, blockType = ${blockData.type}, status = ${status.name}, successful = $successful}"
//
//    companion object {
//        val TYPE: PacketType = PacketType.Play.Server.BLOCK_BREAK
//    }
//}
