package com.github.secretx33.infernalmobsreloaded.model

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockState
import org.bukkit.inventory.InventoryHolder
import java.util.concurrent.atomic.AtomicBoolean

class BlockModification (
    private val blocks: Collection<Block>,
    private val blacklist: MutableSet<Location>,
    private val makeTask: (Collection<Block>) -> Unit,
) {
    private val blockState: List<BlockState> = blocks.map { it.state }
    private val unmade = AtomicBoolean(false)

    fun make() {
        blocks.forEach {
            (blockState as? InventoryHolder)?.inventory?.clear()
            it.type = Material.AIR
        }
        makeTask(blocks)
    }

    fun unmake(){
        if(!unmade.compareAndSet(false, true)) return
        blockState.forEach { it.update(true, true) }
        blacklist.removeAll(blocks.map { it.location })
    }
}
