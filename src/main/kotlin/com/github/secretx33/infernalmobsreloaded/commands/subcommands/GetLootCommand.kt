package com.github.secretx33.infernalmobsreloaded.commands.subcommands

import com.cryptomorin.xseries.XItemStack
import com.github.secretx33.infernalmobsreloaded.config.MessageKeys
import com.github.secretx33.infernalmobsreloaded.config.Messages
import com.github.secretx33.infernalmobsreloaded.config.replace
import com.github.secretx33.infernalmobsreloaded.config.toComponent
import com.github.secretx33.infernalmobsreloaded.manager.CharmsManager
import com.github.secretx33.infernalmobsreloaded.repositories.LootItemsRepo
import com.github.secretx33.infernalmobsreloaded.utils.extension.displayName
import com.github.secretx33.infernalmobsreloaded.utils.other.CustomKoinComponent
import com.github.secretx33.infernalmobsreloaded.utils.other.inject
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class GetLootCommand: SubCommand(), CustomKoinComponent {

    override val name: String = "getloot"
    override val permission: String = "getloot"
    override val aliases: List<String> = listOf(name, "loot", "gl", "l")

    private val messages by inject<Messages>()
    private val lootItemsRepo by inject<LootItemsRepo>()
    private val charmsManager by inject<CharmsManager>()

    override fun onCommandByPlayer(player: Player, alias: String, strings: Array<String>) {
        if(strings.size < 2) {
            player.sendMessage("Usage: /$alias $name <loot_item> [amount]".toComponent(NamedTextColor.RED))
            return
        }

        val lootItem = lootItemsRepo.getLootItemOrNull(strings[1]) ?: run {
            player.sendMessage(messages.get(MessageKeys.LOOT_ITEM_DOESNT_EXIST).replace("<item>", strings[1]))
            return
        }
        val item = lootItem.makeItem()

        // Material.maxStackSize * 36 is the upper limit here because https://github.com/CryptoMorin/XSeries/issues/119
        val amount = if(strings.size < 3) item.amount else strings[2].toIntOrNull()?.coerceIn(1, item.type.maxStackSize * 36) ?: run {
            player.sendMessage(messages.get(MessageKeys.INVALID_NUMBER).replace("<number>", strings[2]))
            return
        }
        item.amount = amount

        // notify the player about the items
        player.sendMessage(messages.get(MessageKeys.RECEIVED_LOOT_ITEM).replace("<amount>", item.amount.toString())
            .replace("<item>", item.displayName))
        // and give the item to him
        if(item.type.maxStackSize == 1)  {
            repeat(amount.coerceAtMost(250)) {
                XItemStack.giveOrDrop(player, true, lootItem.makeItem())
            }
        } else {
            XItemStack.giveOrDrop(player, true, item)
        }
        charmsManager.updateCharmEffects(player)
    }

    override fun onCommandByConsole(sender: CommandSender, alias: String, strings: Array<String>) {
        sender.sendMessage(messages.get(MessageKeys.CONSOLE_CANNOT_USE))
    }

    override fun getCompletor(sender: CommandSender, length: Int, hint: String, strings: Array<String>): List<String> {
        if(sender !is Player || length < 2) return emptyList()

        if(length == 2) return lootItemsRepo.getAllLootItems().filter { it.startsWith(hint, ignoreCase = true) }
        if(length == 3 && hint.isBlank()) return listOf("<number>")

        return emptyList()
    }
}
