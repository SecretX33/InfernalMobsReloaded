package com.github.secretx33.infernalmobsreloaded.commands.subcommands

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.permissions.Permissible

abstract class SubCommand {

    abstract val name: String
    abstract val permission: String
    abstract val aliases: List<String>

    abstract fun onCommandByPlayer(player: Player, alias: String, strings: Array<String>)
    abstract fun onCommandByConsole(sender: CommandSender, alias: String, strings: Array<String>)
    abstract fun getCompletor(sender: CommandSender, length: Int, hint: String, strings: Array<String>): List<String>

    fun hasPermission(sender: Permissible): Boolean = sender.hasPermission("magicwands.$permission")
}

