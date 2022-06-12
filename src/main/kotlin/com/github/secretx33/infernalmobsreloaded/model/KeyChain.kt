package com.github.secretx33.infernalmobsreloaded.model

import com.github.secretx33.infernalmobsreloaded.utils.extension.pdc
import org.bukkit.NamespacedKey
import org.bukkit.entity.Entity
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin
import toothpick.InjectConstructor
import javax.inject.Singleton

/**
 * This class contains all namespace keys of this plugin.
 */
@Singleton
@InjectConstructor
class KeyChain(plugin: Plugin) {
    val infernalCategoryKey        = NamespacedKey(plugin, "infernal_category")       // String
    val abilityListKey             = NamespacedKey(plugin, "infernal_ability_list")   // String
    val livesKey                   = NamespacedKey(plugin, "infernal_lives_count")    // Int > 0
    val infernalMountKey           = NamespacedKey(plugin, "mob_spawned_as_mount_for_infernal")    // Short = 1
    val infernalBatMountKey        = NamespacedKey(plugin, "bat_spawned_to_make_infernal_fly")     // Short = 1
    val fireworkOwnerUuidKey       = NamespacedKey(plugin, "infernal_uuid_which_spawned_the_firework")   // String containing the infernal UUID that spawned the firework
    val spawnerCategoryKey         = NamespacedKey(plugin, "spawner_infernal_category")     // String
    val infernalItemNameKey        = NamespacedKey(plugin, "infernal_mobs_loot_item_name")  // String
    val stolenItemByThiefKey       = NamespacedKey(plugin, "stolen_item_by_thief")          // Short = 1
    val thiefItemDurabilityKey     = NamespacedKey(plugin, "thief_item_durability")         // Int >= 0
    val brokenCharmKey             = NamespacedKey(plugin, "broken_charm_marker")           // Short = 1
    val brokenCharmOriginalNameKey = NamespacedKey(plugin, "broken_charm_original_name")    // String

    fun hasMountKey(entity: Entity) = entity.pdc.has(infernalMountKey, PersistentDataType.SHORT) || entity.pdc.has(infernalBatMountKey, PersistentDataType.SHORT)
}
