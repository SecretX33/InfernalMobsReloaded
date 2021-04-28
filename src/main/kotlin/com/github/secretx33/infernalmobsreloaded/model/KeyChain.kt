package com.github.secretx33.infernalmobsreloaded.model

import org.bukkit.NamespacedKey
import org.bukkit.plugin.Plugin

/**
 * This class contains all namespace keys of this plugin
 */
class KeyChain(plugin: Plugin) {
    val infernalCategoryKey = NamespacedKey(plugin, "infernal_category")   // String
    val abilityListKey      = NamespacedKey(plugin, "ability_list")        // String
    val livesKey            = NamespacedKey(plugin, "lives_count")         // Int > 0
    val infernalMountKey    = NamespacedKey(plugin, "mob_spawned_as_mount_for_infernal")  // Short = 1
    val infernalBatMountKey = NamespacedKey(plugin, "bat_spawned_to_make_infernal_fly")  // Short = 1
}
