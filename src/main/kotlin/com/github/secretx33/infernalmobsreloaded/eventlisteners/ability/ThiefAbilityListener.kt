package com.github.secretx33.infernalmobsreloaded.eventlisteners.ability

import com.github.secretx33.infernalmobsreloaded.config.AbilityConfig
import com.github.secretx33.infernalmobsreloaded.config.AbilityConfigKeys
import com.github.secretx33.infernalmobsreloaded.model.KeyChain
import com.github.secretx33.infernalmobsreloaded.utils.extension.pdc
import com.github.secretx33.infernalmobsreloaded.utils.extension.runSync
import org.bukkit.Bukkit
import org.bukkit.entity.EntityType
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin

class ThiefAbilityListener(
    private val plugin: Plugin,
    private val abilityConfig: AbilityConfig,
    private val keyChain: KeyChain,
) : Listener {

    init { Bukkit.getPluginManager().registerEvents(this, plugin) }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun EntityPickupItemEvent.onStolenItemPickup() {
        val item = item.itemStack.takeIf { it.isStolenItem() }
            ?: return

        // entity picking up the item is human, no further action necessary
        if (entityType == EntityType.PLAYER) {
            item.removeStolenTag()
            return
        }
        // set the drop rate of the stolen item to the configured value
        runSync(plugin, 50L) {
            entity.equipment?.let { equip ->
                EquipmentSlot.values().filter { equip.getItem(it).isStolenItem() }
                    .forEach { equip.setDropChance(it, stolenItemDropChance) }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private fun EntityDeathEvent.onEntityDeathEvent() {
        // players don't need to have theirs drops parsed, since they'll never have stolen items
        if (entityType == EntityType.PLAYER) return
        // restore the durability that the items had before they were stolen
        drops.asSequence().withIndex()
            .filter { it.value?.isStolenItem() == true }
            .forEach { (index, item) -> drops[index] = item.restoreDurability().removeStolenTag() }
    }

    private fun ItemStack?.isStolenItem() = this != null && itemMeta?.pdc?.has(keyChain.stolenItemByThiefKey, PersistentDataType.SHORT) == true

    private fun ItemStack?.removeStolenTag(): ItemStack? {
        this?.itemMeta?.let { meta ->
            meta.pdc.apply {
                remove(keyChain.stolenItemByThiefKey)
                remove(keyChain.thiefItemDurabilityKey)
            }
            itemMeta = meta
        }
        return this
    }

    private fun ItemStack.restoreDurability(): ItemStack {
        val meta = itemMeta ?: return this
        (meta as? Damageable)?.damage = meta.pdc.get(keyChain.thiefItemDurabilityKey, PersistentDataType.INTEGER) ?: 0
        itemMeta = meta
        return this
    }

    private val stolenItemDropChance: Float
        get() = abilityConfig.getDouble(AbilityConfigKeys.THIEF_DROP_STOLEN_ITEM_CHANCE, maxValue = 1.0).toFloat()
}
