package com.github.secretx33.infernalmobsreloaded.command

import com.github.secretx33.infernalmobsreloaded.command.subcommand.SubCommand
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import toothpick.InjectConstructor
import toothpick.Scope
import java.util.Locale
import javax.inject.Named
import kotlin.reflect.KClass

@InjectConstructor
class Commands(
    plugin: JavaPlugin,
    scope: Scope,
    @Named("subcommands") subcommandClasses: Set<KClass<out SubCommand>>,
) : CommandExecutor, TabCompleter {

    private val subcommands = subcommandClasses
        .mapTo(mutableSetOf()) { scope.getInstance(it.java) }

    init {
        plugin.getCommand("imr")?.let { cmd ->
            cmd.setExecutor(this)
            cmd.tabCompleter = this
        }
    }

    override fun onCommand(sender: CommandSender, command: Command, alias: String, strings: Array<String>): Boolean {
        if (strings.isEmpty()) return true

        val sub = strings[0].lowercase(Locale.US)
        subcommands.firstOrNull { it.hasPermission(sender) && (sub == it.name || sub in it.aliases) }?.let { cmd ->
            if(sender is Player) {
                cmd.onCommandByPlayer(sender, alias, strings)
            } else {
                cmd.onCommandByConsole(sender, alias, strings)
            }
        }
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, strings: Array<String>): List<String> {
        if(strings.isEmpty()) return emptyList()
        // mobs <subcommand> <args>
        if(strings.size == 1) {
            return subcommands.asSequence()
                .filter { cmd -> cmd.hasPermission(sender) && cmd.name.startsWith(strings[0], ignoreCase = true)}
                .map { it.name }
                .toList()
        }
        return subcommands
            .firstOrNull { it.hasPermission(sender) && it.aliases.contains(strings[0].lowercase()) }
            ?.getCompletor(sender, strings.size, strings[strings.size - 1], strings)
            ?: emptyList()
    }
}
