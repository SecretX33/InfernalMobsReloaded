package com.github.secretx33.infernalmobsreloaded.commands

import com.github.secretx33.infernalmobsreloaded.commands.subcommands.InspectCommand
import com.github.secretx33.infernalmobsreloaded.commands.subcommands.ReloadCommand
import com.github.secretx33.infernalmobsreloaded.commands.subcommands.SpawnCommand
import com.github.secretx33.infernalmobsreloaded.commands.subcommands.SubCommand
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.koin.core.component.KoinApiExtension

@KoinApiExtension
class Commands(plugin: JavaPlugin) : CommandExecutor, TabCompleter {

    private val subcommands: List<SubCommand> = listOf(
        InspectCommand(),
        ReloadCommand(),
        SpawnCommand(),
    )

    init {
        plugin.getCommand("infernalmobsreloaded")?.let { cmd ->
            cmd.setExecutor(this)
            cmd.tabCompleter = this
        }
    }

    override fun onCommand(sender: CommandSender, command: Command, alias: String, strings: Array<String>): Boolean {
        if (strings.isEmpty()) return true

        val sub = strings[0]
        subcommands.firstOrNull { it.hasPermission(sender) && (it.name == sub || it.aliases.contains(sub)) }?.let { cmd ->
            if(sender is Player) {
                cmd.onCommandByPlayer(sender, alias, strings)
            } else {
                cmd.onCommandByConsole(sender, alias, strings)
            }
        }
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, strings: Array<String>): List<String> {
        // mobs <subcommand> <args>
        if(strings.size == 1) {
            return subcommands.asSequence()
                .filter { cmd -> cmd.hasPermission(sender) && cmd.name.startsWith(strings[0], ignoreCase = true)}
                .map { it.name }
                .toList()
        }
        if(strings.size > 1) {
            return subcommands
                .firstOrNull { it.hasPermission(sender) && it.aliases.contains(strings[0].toLowerCase()) }
                ?.getCompletor(sender, strings.size, strings[strings.size - 1], strings)
                ?: emptyList()
        }
        return emptyList()
    }
}
