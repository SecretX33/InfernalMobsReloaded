package com.github.secretx33.infernalmobsreloaded.commands.subcommands

import com.github.secretx33.infernalmobsreloaded.config.MessageKeys
import com.github.secretx33.infernalmobsreloaded.config.Messages
import com.github.secretx33.infernalmobsreloaded.manager.InfernalMobsManager
import com.github.secretx33.infernalmobsreloaded.model.KeyChain
import com.github.secretx33.infernalmobsreloaded.utils.other.WrappedInjector
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class KillAllCommand(injector: WrappedInjector) : SubCommand() {

    override val name: String = "killall"
    override val permission: String = "killall"
    override val aliases: List<String> = listOf(name, "ka")

    private val messages = injector.getInstance<Messages>()
    private val mobsManager = injector.getInstance<InfernalMobsManager>()
    private val keyChain = injector.getInstance<KeyChain>()

    override fun onCommandByPlayer(player: Player, alias: String, strings: Array<String>) {
        Bukkit.getWorlds().flatMap { it.livingEntities }
            .filter { mobsManager.isPossibleInfernalMob(it) || keyChain.hasMountKey(it) }
            .forEach { mobsManager.removeAndDropStolenItems(it) }

        // notify player that all infernals got killed
        player.sendMessage(messages.get(MessageKeys.KILLED_ALL_INFERNALS))
    }

    override fun onCommandByConsole(sender: CommandSender, alias: String, strings: Array<String>) {
        sender.sendMessage(messages.get(MessageKeys.CONSOLE_CANNOT_USE))
    }

    override fun getCompletor(sender: CommandSender, length: Int, hint: String, strings: Array<String>): List<String> {
        return emptyList()
    }
}

