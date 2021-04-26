package com.github.secretx33.infernalmobsreloaded.repositories

import com.github.secretx33.infernalmobsreloaded.model.InfernalMobType
import com.github.secretx33.infernalmobsreloaded.utils.YamlManager
import org.bukkit.plugin.Plugin
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.logging.Logger

class InfernalMobTypesRepo(plugin: Plugin, private val log: Logger) {

    private val manager   = YamlManager(plugin, "mobs")
    private val typeCache = ConcurrentHashMap<String, InfernalMobType>()     // lowercase groupName, allInfos
    private val typeList  = CopyOnWriteArrayList<String>()                   // original groupNames

    init { loadMobTypes() }

    fun reload() {
        manager.reload()
        typeCache.clear()
        typeList.clear()
        loadMobTypes()
    }

//    fun getTypeOrNull(group: String): HarvestBlockGroup? = typeCache[group.toLowerCase(Locale.US)]
//
//    fun getGroup(group: String): HarvestBlockGroup = getGroupOrNull(group) ?: throw NoSuchElementException("HarvestBlock for group $group was not found.")
//
//    fun hasGroup(group: String) = typeCache.containsKey(group.toLowerCase(Locale.US))
//
//    fun getAllGroups(): List<String> = typeList

    // used to get "current case" version of a group name, because wands store the group name the way it were, before possible case changes
//    fun getGroupFromName(group: String): String? = typeList.firstOrNull { it.equals(group, ignoreCase = true) }

    private fun loadMobTypes() {
        typeList.apply {
            addAll(manager.getKeys(false))
            sort()
            forEach { group ->
                typeCache[group.toLowerCase(Locale.US)] = makeHarvestBlock(group)
            }
        }
    }

    private fun makeHarvestBlock(group: String): HarvestBlockGroup {
        val breakEffect = manager.getBoolean("$group.enable-break-effect", true)
        val regenTime = max(manager.getDouble("$group.regen-time", 30.0), 0.0)
        val harvestTime = max(manager.getDouble("$group.harvest-time", 3.0), 0.0)
        val item = getHarvestItem(group)
        return HarvestBlockGroup(group,
            breakEffect = breakEffect,
            regenTime = regenTime,
            harvestTime = harvestTime,
            dropItem = item,
            sound  = getSound(group),
            volume = max(manager.getDouble("$group.sound-volume", 1.0), 0.01).toFloat(),
            pitch  = max(manager.getDouble("$group.sound-pitch", 1.0), 0.01).toFloat()
        )
    }

    @Suppress("LiftReturnOrAssignment")
    private fun getHarvestItem(group: String): ItemStack {
        val itemName = manager.getString("$group.item-reward") ?: ""

        // if name is absent or blank
        if(itemName.isBlank()) {
            log.severe("You must provide an item reward for the category ${group}! Defaulting $group reward to Stone.")
            return ItemStack(Material.STONE)
        }

        val words = itemName.split(':').takeIf { it.size in 1..2 } ?: throw IllegalStateException("While getting the item for group $group, itemName $itemName could not be parsed, its format is invalid.")

        // is vanilla item
        if(words.size == 1) {
            return XMaterial.matchXMaterial(words[0]).map { it.parseItem() }.filter { it?.type?.isItem == true }.orElseGet {
                log.severe("Inside harvest group '$group', item of material '${words[0]}' doesn't exist, please fix your item reward configurations. Defaulting $group reward to Stone.")
                ItemStack(Material.STONE)
            }!!
        }

        // is MMOItem
        val mmoItemType = MMOItems.plugin.types[words[0]] ?: run {
            log.severe("Inside harvest group '$group', MMOItem category named '${words[0]}' doesn't exist, please fix your MMOItems configurations or your item reward configurations for group name $group. Defaulting $group reward to Stone.")
            return ItemStack(Material.STONE)
        }

        // ugly catch for non existent item, but there's no better way of handling the item creation for MMOItems
        try {
            return MMOItems.plugin.getItem(mmoItemType, words[1])
        } catch (e: NullPointerException) {
            log.severe("Inside harvest group '$group', could not found an item named '${words[1]}' of mmo category ${mmoItemType.name}, please fix your MMOItems configurations or your item reward configurations. Defaulting $group reward to Stone.")
            return ItemStack(Material.STONE)
        }
    }

    private fun getSound(group: String): Sound? {
        val sound = manager.getString("$group.sound-effect") ?: return null
        return XSound.matchXSound(sound).map { it.parseSound() }.orElse(null) ?: run {
            log.warning("Inside group name $group, could not found sound named '$sound', please fix your harvest group configurations. Defaulting $group sound to nothing.")
            null
        }
    }
}
