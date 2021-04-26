//package com.github.secretx33.almostasyncworldedit.manager
//
//import com.sk89q.worldedit.bukkit.BukkitAdapter
//import com.sk89q.worldguard.WorldGuard
//import com.sk89q.worldguard.bukkit.WorldGuardPlugin
//import com.sk89q.worldguard.protection.flags.BooleanFlag
//import com.sk89q.worldguard.protection.flags.Flag
//import com.sk89q.worldguard.protection.flags.Flags
//import org.bukkit.Bukkit
//import org.bukkit.block.Block
//import org.bukkit.entity.Player
//import java.util.logging.Logger
//
//interface WorldGuardChecker {
//    fun canBreakBlock(block: Block, player: Player): Boolean
//}
//
//// Used when WorldGuard is absent
//class WorldGuardCheckerDummy : WorldGuardChecker {
//    override fun canBreakBlock(block: Block, player: Player): Boolean = true
//}
//
//// Used when WorldGuard is present
//class WorldGuardCheckerImpl : WorldGuardChecker {
//
//    override fun canBreakBlock(block: Block, player: Player): Boolean {
//        if(!isWorldGuardEnabled) return true
//
//        val wg = WorldGuard.getInstance()
//        val loc = BukkitAdapter.adapt(block.location)
//        val container = wg.platform.regionContainer
//        val query = container.createQuery()
//        val localPlayer = WorldGuardPlugin.inst().wrapPlayer(player)
//        return query.testBuild(loc, localPlayer, Flags.BUILD) || wg.platform.sessionManager.hasBypass(localPlayer, localPlayer.world)
//    }
//    private val isWorldGuardEnabled
//        get() = Bukkit.getPluginManager().isPluginEnabled("WorldGuard")
//}
