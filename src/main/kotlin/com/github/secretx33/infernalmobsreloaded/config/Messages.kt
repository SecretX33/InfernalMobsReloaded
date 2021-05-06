package com.github.secretx33.infernalmobsreloaded.config

import com.github.secretx33.infernalmobsreloaded.utils.YamlManager
import me.mattstudios.msg.adventure.AdventureMessage
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentLike
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
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
            manager.getString(key.configEntry)?.parse() ?: default?.parse() ?: (key.default as? Component) ?: (key.default as String).parse()
        }
    }

    fun getList(key: MessageKeys): List<Component> {
        return listCache.getOrPut(key) {
            manager.getStringList(key.configEntry).map { it.parse() }
        }
    }

    fun reload() {
        stringCache.clear()
        listCache.clear()
        manager.reload()
    }

    private fun String.parse(): Component = adventureMessage.parse(this)
}

enum class MessageKeys(val default: Any) {
    RUST_CORRODE_TOOLS_MESSAGE("<#c27c21>You feel your tools corroding at your hands."),
    THIEF_MESSAGE_TO_TARGET("<#55FFFF>Woah, beware! <entity> <#55FFFF>stole your <#FFFFFF><item>."),
    THIEF_MESSAGE_TO_TARGET_ITEM_BROKE("<#55FFFF>Woah, beware! <entity> <#55FFFF>stole your <#FFFFFF><item><#55FFFF>, and unfortunately it broke in the process."),
    ABILITY_DOESNT_EXIST("Sorry, there is no ability named <ability>, please type a valid ability.".toComponent(NamedTextColor.RED)),
    INFERNAL_MOB_TYPE_DOESNT_EXIST("Sorry, the infernal type <type> doesn't exist in your mobs.yml file, please type a valid name.".toComponent(NamedTextColor.RED)),
    NOT_TARGETING_LIVING_ENTITY("You are not targeting an entity, please aim to an entity and try again.".toComponent(NamedTextColor.RED)),
    NOT_TARGETING_INFERNAl("The entity you're currently targeting is not an Infernal Mob, please target an Infernal Mob and try again.".toComponent(NamedTextColor.RED)),
    NOT_TARGETING_VALID_INFERNAl("The entity you're currently targeting was an Infernal Mob from mob category <group>, but this mob category is not present on your mobs.yml file, so it's not currently considered an Infernal Mob.".toComponent(NamedTextColor.RED)),
    TARGETING_INFERNAL("<#55FF55>The <#00AA00><entity> <#55FF55>you're currently targeting has the following abilities: <#ffb319><abilities>."),
    INFERNAL_MOB_SPAWN_MESSAGES(""),
    INFERNAL_MOB_DEATH_MESSAGES(""),
    CONFIGS_RELOADED("${ChatColor.GREEN}Reloaded configs."),
    CONSOLE_CANNOT_USE("${ChatColor.RED}Sorry, the console cannot use this command.");

    val configEntry = name.lowercase(Locale.US).replace('_','-')
}

fun String.toComponent(r: Int, g: Int, b: Int) = Component.text(this, TextColor.color(r, g, b))

fun String.toComponent(color: NamedTextColor? = null) = if(color == null) Component.text(this) else Component.text(this, color)

fun Component.replace(oldText: String, newText: String) = replaceText { it.match(oldText).replacement(newText) }

fun Component.replace(oldText: String, newText: String, color: NamedTextColor) = replaceText { it.match(oldText).replacement(newText.toComponent(color)) }

fun Component.replace(oldText: String, newText: ComponentLike) = replaceText { it.match(oldText).replacement(newText) }
