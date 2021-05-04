package com.github.secretx33.infernalmobsreloaded.model

import org.bukkit.NamespacedKey
import org.bukkit.plugin.Plugin

/**
 * This class contains all namespace keys of this plugin
 */
class KeyChain(plugin: Plugin) {
    val infernalCategoryKey   = NamespacedKey(plugin, "infernal_category")       // String
    val abilityListKey        = NamespacedKey(plugin, "infernal_ability_list")   // String
    val livesKey              = NamespacedKey(plugin, "infernal_lives_count")    // Int > 0
    val infernalMountKey      = NamespacedKey(plugin, "mob_spawned_as_mount_for_infernal")    // Short = 1
    val infernalBatMountKey   = NamespacedKey(plugin, "bat_spawned_to_make_infernal_fly")     // Short = 1
    val fireworkOwnerUuidKey  = NamespacedKey(plugin, "infernal_uuid_which_spawned_the_firework")   // String containing the infernal UUID that spawned the firework
    val spawnerCategoryKey    = NamespacedKey(plugin, "spawner_infernal_category")  // String
}
