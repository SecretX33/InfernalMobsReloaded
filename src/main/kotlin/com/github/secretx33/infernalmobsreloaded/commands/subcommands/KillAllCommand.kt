package com.github.secretx33.infernalmobsreloaded.commands.subcommands

import com.github.secretx33.infernalmobsreloaded.config.MessageKeys
import com.github.secretx33.infernalmobsreloaded.config.Messages
import com.github.secretx33.infernalmobsreloaded.config.replace
import com.github.secretx33.infernalmobsreloaded.manager.InfernalMobsManager
import com.github.secretx33.infernalmobsreloaded.model.KeyChain
import com.github.secretx33.infernalmobsreloaded.utils.other.CustomKoinComponent
import com.github.secretx33.infernalmobsreloaded.utils.other.inject
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class KillAllCommand : SubCommand(), CustomKoinComponent {

    override val name: String = "killall"
    override val permission: String = "killall"
    override val aliases: List<String> = listOf(name, "ka")

    private val messages by inject<Messages>()
    private val mobsManager by inject<InfernalMobsManager>()
    private val keyChain by inject<KeyChain>()

    override fun onCommandByPlayer(player: Player, alias: String, strings: Array<String>) =
        onCommandByConsole(player, alias, strings)

    override fun onCommandByConsole(sender: CommandSender, alias: String, strings: Array<String>) {
        // killall [world]
        if(strings.size < 2) {
            killAllInfernals(sender)
            return
        }
        killAllInfernalsOnWorld(sender, strings[1])
    }

    private fun killAllInfernalsOnWorld(sender: CommandSender, worldName: String) {
        val world = Bukkit.getWorld(worldName) ?: run {
            sender.sendMessage(messages.get(MessageKeys.WORLD_NOT_FOUND).replace("<world>", worldName))
            return
        }
        world.killInfernals()
        sender.sendMessage(messages.get(MessageKeys.KILLED_ALL_INFERNALS_ON_WORLD).replace("<world>", world.name))
    }

    private fun killAllInfernals(sender: CommandSender) {
        Bukkit.getWorlds().forEach { it.killInfernals() }

        // notify sender that all infernals got killed
        sender.sendMessage(messages.get(MessageKeys.KILLED_ALL_INFERNALS))
    }

    private fun World.killInfernals() = livingEntities
        .filter { mobsManager.isPossibleInfernalMob(it) || keyChain.hasMountKey(it) }
        .forEach { mobsManager.removeAndDropStolenItems(it) }

    override fun getCompletor(sender: CommandSender, length: Int, hint: String, strings: Array<String>): List<String> {
        if(length != 2) return emptyList()
        return Bukkit.getWorlds()
            .filter { it.name.startsWith(hint, ignoreCase = true) }
            .map { it.name }
    }
}

