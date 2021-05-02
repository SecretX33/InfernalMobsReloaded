//package com.github.secretx33.infernalmobsreloaded.packets
//
//import com.comphenix.protocol.PacketType
//import com.comphenix.protocol.events.PacketContainer
//import com.comphenix.protocol.events.PacketEvent
//import com.comphenix.protocol.wrappers.EnumWrappers
//import org.bukkit.Sound
//import org.bukkit.World
//import org.bukkit.entity.Entity
//
//class WrapperPlayServerEntitySound : AbstractPacket {
//
//    constructor() : super(PacketContainer(TYPE), TYPE) {
//        handle.modifier.writeDefaults()
//    }
//
//    constructor(packet: PacketContainer) : super(packet, TYPE) {}
//
//    var sound: Sound
//        /**
//         * Retrieve Sound ID.
//         *
//         * Notes: iD of hardcoded sound event (events as of 1.15.2)
//         * @return The current Sound ID
//         */
//        get() = handle.soundEffects.read(0)
//        /**
//         * Set Sound ID.
//         * @param value - new value.
//         */
//        set(value) {
//            handle.soundEffects.write(0, value)
//        }
//
//    var soundCategory: EnumWrappers.SoundCategory
//        /**
//         * Retrieve Sound Category.
//         *
//         *
//         * Notes: the category that this sound will be played from (current categories)
//         * @return The current Sound Category
//         */
//        get() = handle.soundCategories.read(0)
//        /**
//         * Set Sound Category.
//         * @param value - new value.
//         */
//        set(value) {
//            handle.soundCategories.write(0, value)
//        }
//
//    var entityID: Int
//        /**
//         * Retrieve Entity ID.
//         * @return The current Entity ID
//         */
//        get() = handle.integers.read(0)
//        /**
//         * Set Entity ID.
//         * @param value - new value.
//         */
//        set(value) {
//            handle.integers.write(0, value)
//        }
//
//    /**
//     * Retrieve the entity involved in this event.
//     * @param world - the current world of the entity.
//     * @return The involved entity.
//     */
//    fun getEntity(world: World): Entity = handle.getEntityModifier(world).read(2)
//
//    /**
//     * Retrieve the entity involved in this event.
//     * @param event - the packet event.
//     * @return The involved entity.
//     */
//    fun getEntity(event: PacketEvent): Entity {
//        return getEntity(event.player.world)
//    }
//
//    var volume: Float
//        /**
//         * Retrieve Volume.
//         *
//         * Notes: 1.0 is 100%, capped between 0.0 and 1.0 by Notchian clients
//         * @return The current Volume
//         */
//        get() = handle.float.read(0)
//        /**
//         * Set Volume.
//         * @param value - new value.
//         */
//        set(value) {
//            handle.float.write(0, value)
//        }
//
//    var pitch: Float
//        /**
//         * Retrieve Pitch.
//         *
//         * Notes: float between 0.5 and 2.0 by Notchian clients
//         * @return The current Pitch
//         */
//        get() = handle.float.read(1)
//        /**
//         * Set Pitch.
//         * @param value - new value.
//         */
//        set(value) {
//            handle.float.write(1, value)
//        }
//
//    companion object {
//        val TYPE: PacketType = PacketType.Play.Server.ENTITY_SOUND
//    }
//}
//
