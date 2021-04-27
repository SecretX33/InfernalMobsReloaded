package com.github.secretx33.infernalmobsreloaded.config

import com.github.secretx33.infernalmobsreloaded.utils.YamlManager
import me.mattstudios.msg.adventure.AdventureMessage
import net.kyori.adventure.text.Component
import org.bukkit.ChatColor
import org.bukkit.plugin.Plugin
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class Messages(plugin: Plugin, private val adventureMessage: AdventureMessage) {
    private val manager = YamlManager(plugin, "messages")
    private val stringCache = ConcurrentHashMap<MessageKeys, Component>()
    private val listCache = ConcurrentHashMap<MessageKeys, List<Component>>()

    fun get(key: MessageKeys, default: String? = null): Component {
        return stringCache.getOrPut(key) {
            (manager.getString(key.configEntry) ?: default ?: key.default).toComponent()
        }
    }

    fun getList(key: MessageKeys): List<Component> {
        return listCache.getOrPut(key) {
            manager.getStringList(key.configEntry).map { it.toComponent() }
        }
    }

    fun reload() {
        stringCache.clear()
        listCache.clear()
        manager.reload()
    }

    private fun String.toComponent(): Component = adventureMessage.parse(this)

    private fun String.correctColorCodes(): String = ChatColor.translateAlternateColorCodes('&', this)
}

enum class MessageKeys(val default: String) {
    INFERNAL_MOB_SPAWN_MESSAGES(""),
    INFERNAL_MOB_DEATH_MESSAGES(""),
    CONFIGS_RELOADED("${ChatColor.GREEN}Reloaded configs."),
    CONSOLE_CANNOT_USE("${ChatColor.RED}Sorry, the console cannot use this command."),
    HARVEST_BAR_TEXT("${ChatColor.DARK_GREEN}Harvest: ${ChatColor.DARK_GRAY}[<bar>${ChatColor.DARK_GRAY}]"),
    HARVEST_CANCELLED_ANOTHER_PLAYER_FINISHED_FIRST("${ChatColor.RED}Oops, seems like <player> was faster this time."),
    HARVEST_CANCELLED_BLOCK_REMOVED("${ChatColor.RED}The block you were harvesting was removed."),
    HARVEST_CANCELLED_DUE_TO_MOVEMENT("${ChatColor.RED}The action was cancelled due to movement."),
    HARVEST_FINISHED("${ChatColor.GREEN}You just harvested a <item>."),
    MARKED_HARVEST_BLOCK("${ChatColor.GREEN}Added block <type> to harvest group <group>."),
    PLACED_BLOCK_BROKE_HARVEST_BLOCK("${ChatColor.RED}The block you placed broke the harvest block that was there."),
    RECEIVED_WAND("${ChatColor.GREEN}You received a selection wand."),
    REGEN_ALL_BLOCKS("${ChatColor.GREEN}Regenerated all blocks."),
    REGEN_BLOCKS_OF_TYPE("${ChatColor.GREEN}Regenerated all <group> blocks."),
    TYPED_GROUP_IS_INVALID("${ChatColor.RED}Sorry, there is no group named <group>, please correct the group name and try again."),
    UNMARKED_HARVEST_BLOCK("${ChatColor.YELLOW}Removed block <type> from harvest group <group>, it is a normal block now."),
    UPDATED_HARVEST_BLOCK("${ChatColor.LIGHT_PURPLE}Block <type> now belongs to harvest group <group>."),
    YOU_BROKE_A_HARVEST_BLOCK("${ChatColor.RED}You broke a harvest block.");

    val configEntry = name.toLowerCase(Locale.US).replace('_','-')
}
