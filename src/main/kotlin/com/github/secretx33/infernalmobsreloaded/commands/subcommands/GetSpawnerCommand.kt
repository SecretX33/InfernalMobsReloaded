package com.github.secretx33.infernalmobsreloaded.commands.subcommands

import com.cryptomorin.xseries.XItemStack
import com.github.secretx33.infernalmobsreloaded.config.MessageKeys
import com.github.secretx33.infernalmobsreloaded.config.Messages
import com.github.secretx33.infernalmobsreloaded.config.replace
import com.github.secretx33.infernalmobsreloaded.config.toComponent
import com.github.secretx33.infernalmobsreloaded.repositories.InfernalMobTypesRepo
import com.github.secretx33.infernalmobsreloaded.utils.extension.displayName
import com.github.secretx33.infernalmobsreloaded.utils.extension.turnIntoSpawner


import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import toothpick.ktp.delegate.inject

class GetSpawnerCommand: SubCommand() {

    override val name: String = "getspawner"
    override val permission: String = "getspawner"
    override val aliases: List<String> = listOf(name, "spawner", "gs")

    private val messages by inject<Messages>()
    private val infernalMobTypesRepo by inject<InfernalMobTypesRepo>()

    override fun onCommandByPlayer(player: Player, alias: String, strings: Array<String>) {
        if(strings.size < 2) {
            player.sendMessage("Usage: /$alias $name <entity_type> [amount]".toComponent(NamedTextColor.RED))
            return
        }

        // if infernal type doesn't exist
        val infernalType = infernalMobTypesRepo.getInfernalTypeOrNull(strings[1]) ?: run {
            player.sendMessage(messages.get(MessageKeys.INFERNAL_MOB_TYPE_DOESNT_EXIST).replace("<type>", strings[1].toComponent(NamedTextColor.GOLD)))
            return
        }
        val spawner = ItemStack(Material.SPAWNER).turnIntoSpawner(infernalType)

        // Material.maxStackSize * 36 is the upper limit here because https://github.com/CryptoMorin/XSeries/issues/119
        val amount = if(strings.size < 3) spawner.amount else strings[2].toIntOrNull()?.coerceIn(1, spawner.type.maxStackSize * 36) ?: run {
            player.sendMessage(messages.get(MessageKeys.INVALID_NUMBER).replace("<number>", strings[2]))
            return
        }
        spawner.amount = amount

        // notify the player about the items
        player.sendMessage(messages.get(MessageKeys.RECEIVED_LOOT_ITEM).replace("<amount>", spawner.amount.toString())
            .replace("<item>", spawner.displayName))
        // and give the item to him
        XItemStack.giveOrDrop(player, true, spawner)
    }

    override fun onCommandByConsole(sender: CommandSender, alias: String, strings: Array<String>) {
        sender.sendMessage(messages.get(MessageKeys.CONSOLE_CANNOT_USE))
    }

    override fun getCompletor(sender: CommandSender, length: Int, hint: String, strings: Array<String>): List<String> {
        if(sender !is Player || length < 2) return emptyList()

        return when {
            length == 2 -> infernalMobTypesRepo.getAllInfernalTypeNames().filter { it.startsWith(hint, ignoreCase = true) }
            length == 3 && hint.isBlank() -> listOf("<number>")
            else -> emptyList()
        }
    }
}
