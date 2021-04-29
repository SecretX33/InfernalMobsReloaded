package com.github.secretx33.infernalmobsreloaded.model

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockState
import org.bukkit.inventory.InventoryHolder
import java.util.concurrent.atomic.AtomicBoolean

class BlockModification (
    private val blocks: List<Block>,
    private val blockModificationList: MutableSet<BlockModification>,
    private val blockBlacklist: MutableSet<Location>,
    private val makeTask: (List<Block>) -> Unit,
) {
    val blockLocations = blocks.map { it.location }
    private val blockState: List<BlockState> = blocks.map { it.state }
    private val unmade = AtomicBoolean(false)

    // blockModificationList and blockBlacklist additions are ALL responsibility of the caller,
    // this class will only be responsible for removing them, when the changes are reverted
    fun make() {
        // don't try to modify the blocks if they already got "reverted" back
        if(unmade.get()) return
        blocks.forEach {
            (blockState as? InventoryHolder)?.inventory?.clear()
            it.type = Material.AIR
        }
        makeTask(blocks)
    }

    fun unmake(){
        // don't try to revert the block states back if they got reverted already
        if(!unmade.compareAndSet(false, true)) return
        blockState.forEach { it.update(true, true) }
        blockBlacklist.removeAll(blockLocations)
        blockModificationList.remove(this)
    }
}
