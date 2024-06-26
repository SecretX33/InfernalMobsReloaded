package com.github.secretx33.infernalmobsreloaded.command.subcommand

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.permissions.Permissible

abstract class SubCommand {

    abstract val name: String
    abstract val permission: String
    abstract val aliases: List<String>

    abstract fun onCommandByPlayer(player: Player, alias: String, strings: Array<String>)
    abstract fun onCommandByConsole(sender: CommandSender, alias: String, strings: Array<String>)
    open fun getCompletor(sender: CommandSender, length: Int, hint: String, strings: Array<String>): List<String> = emptyList()

    fun hasPermission(sender: Permissible): Boolean = sender.hasPermission("imr.$permission")
}

