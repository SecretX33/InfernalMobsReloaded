package com.github.secretx33.infernalmobsreloaded.manager.hook

import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldguard.WorldGuard
import com.sk89q.worldguard.protection.flags.Flags
import org.bukkit.Bukkit
import org.bukkit.block.Block
import toothpick.InjectConstructor

interface WorldGuardChecker {
    fun canMobGriefBlock(block: Block): Boolean
}

// Used when WorldGuard is absent
@InjectConstructor
class WorldGuardCheckerDummy : WorldGuardChecker {
    override fun canMobGriefBlock(block: Block): Boolean = true
}

// Used when WorldGuard is present
@InjectConstructor
class WorldGuardCheckerImpl : WorldGuardChecker {

    override fun canMobGriefBlock(block: Block): Boolean {
        if (!isWorldGuardEnabled) return true

        val loc = BukkitAdapter.adapt(block.location)
        val container = WorldGuard.getInstance().platform.regionContainer
        val query = container.createQuery()
        return query.testState(loc, null, Flags.MOB_DAMAGE, Flags.CREEPER_EXPLOSION, Flags.ENDERDRAGON_BLOCK_DAMAGE, Flags.GHAST_FIREBALL, Flags.OTHER_EXPLOSION, Flags.WITHER_DAMAGE, Flags.ENDER_BUILD, Flags.SNOWMAN_TRAILS, Flags.RAVAGER_RAVAGE, Flags.ENTITY_PAINTING_DESTROY, Flags.ENTITY_ITEM_FRAME_DESTROY)
    }
    private val isWorldGuardEnabled
        get() = Bukkit.getPluginManager().isPluginEnabled("WorldGuard")
}
