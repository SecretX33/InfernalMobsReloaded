//package com.github.secretx33.infernalmobs2.packetlisteners
//
//import com.comphenix.protocol.PacketType
//import com.comphenix.protocol.ProtocolLibrary
//import com.comphenix.protocol.events.ListenerPriority
//import com.comphenix.protocol.events.PacketAdapter
//import com.comphenix.protocol.events.PacketEvent
//import com.comphenix.protocol.wrappers.EnumWrappers
//import com.github.secretx33.infernalmobs2.manager.WorldGuardChecker
//import com.github.secretx33.infernalmobs2.packets.WrapperPlayClientBlockDig
//import org.bukkit.plugin.Plugin
//
//class BlockBreakPacketListener (
//    private val plugin: Plugin,
//    private val wgChecker: WorldGuardChecker,
//) {
//
//    init { setup() }
//
//    private fun setup() {
//        val manager = ProtocolLibrary.getProtocolManager()
//
//        manager.addPacketListener(object : PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Client.BLOCK_DIG) {
//            override fun onPacketReceiving(event: PacketEvent) {
//                if(event.packetType != PacketType.Play.Client.BLOCK_DIG || event.isCancelled) return
//
//                val wrapper = WrapperPlayClientBlockDig(event.packet)
//
//                // if packet is not about a finished block destruction (creative destruction doesn't send this packet either)
//                if(!wrapper.isBlockDestroyPacket()) return
//
//                val player = event.player
//                val tool = player.inventory.itemInMainHand
//                val block = wrapper.location.toLocation(player.world).let { player.world.getBlockAt(it) }
//
//                // TODO("Check if the block is current under modification, and if not return")
//
//                // if player cannot break the block he's trying to break
//                if(!wgChecker.canBreakBlock(block, player)) {
//                    event.isCancelled = true
//                    return
//                }
//
//                // TODO("Drop the block normal reward")
//            }
//        })
//    }
//
//    private fun WrapperPlayClientBlockDig.isBlockDestroyPacket() = status != EnumWrappers.PlayerDigType.STOP_DESTROY_BLOCK
//}
