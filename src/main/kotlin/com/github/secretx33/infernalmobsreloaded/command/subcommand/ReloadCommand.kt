package com.github.secretx33.infernalmobsreloaded.command.subcommand

import com.github.secretx33.infernalmobsreloaded.config.MessageKeys
import com.github.secretx33.infernalmobsreloaded.config.Messages
import com.github.secretx33.infernalmobsreloaded.eventbus.EventBus
import com.github.secretx33.infernalmobsreloaded.eventbus.internalevent.PluginReload
import com.github.secretx33.infernalmobsreloaded.eventbus.internalevent.PluginReloaded
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import toothpick.InjectConstructor

@InjectConstructor
class ReloadCommand(
    private val messages: Messages,
    private val eventBus: EventBus,
) : SubCommand() {

    override val name: String = "reload"
    override val permission: String = "reload"
    override val aliases: List<String> = listOf(name, "rel", "r")

    override fun onCommandByPlayer(player: Player, alias: String, strings: Array<String>) =
        onCommandByConsole(player, alias, strings)

    override fun onCommandByConsole(sender: CommandSender, alias: String, strings: Array<String>) {
        eventBus.post(PluginReload())
        eventBus.post(PluginReloaded())
        sender.sendMessage(messages.get(MessageKeys.CONFIGS_RELOADED))
    }
}
