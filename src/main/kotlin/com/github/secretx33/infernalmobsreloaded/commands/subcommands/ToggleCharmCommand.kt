package com.github.secretx33.infernalmobsreloaded.commands.subcommands

import com.github.secretx33.infernalmobsreloaded.config.MessageKeys
import com.github.secretx33.infernalmobsreloaded.config.Messages
import com.github.secretx33.infernalmobsreloaded.config.replace
import com.github.secretx33.infernalmobsreloaded.manager.CharmsManager
import com.github.secretx33.infernalmobsreloaded.model.KeyChain
import com.github.secretx33.infernalmobsreloaded.repositories.CharmsRepo
import com.github.secretx33.infernalmobsreloaded.utils.extension.displayName
import com.github.secretx33.infernalmobsreloaded.utils.extension.isAir
import com.github.secretx33.infernalmobsreloaded.utils.extension.pdc
import com.github.secretx33.infernalmobsreloaded.utils.other.CustomKoinComponent
import com.github.secretx33.infernalmobsreloaded.utils.other.inject
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType

class ToggleCharmCommand: SubCommand(), CustomKoinComponent {

    override val name: String = "togglecharm"
    override val permission: String = "charms.toggle"
    override val aliases: List<String> = listOf(name, "togglec", "tcharm", "tc")

    private val messages by inject<Messages>()
    private val keyChain by inject<KeyChain>()
    private val charmsRepo by inject<CharmsRepo>()
    private val charmsManager by inject<CharmsManager>()

    override fun onCommandByPlayer(player: Player, alias: String, strings: Array<String>) {
        val item = player.inventory.itemInMainHand

        // player not holding a charm
        if(item.isAir() || !charmsRepo.isItemRequiredByCharmEffect(item, acceptBroken = true)) {
            player.sendMessage(messages.get(MessageKeys.NOT_HOLDING_CHARM))
            return
        }
        val message: Component
        val itemName = item.displayName

        val newItem = item.clone()
        newItem.itemMeta.apply {
            if(pdc.has(keyChain.brokenCharmKey, PersistentDataType.SHORT)) {
                restoreCharm(item)
                message = messages.get(MessageKeys.YOU_HAVE_RESTORED_YOUR_CHARM).replace("<charm>", displayName()
                    ?: throw IllegalStateException("Item ${item.type} doesn't have a display name after being restored by ${player.name}, this should not happen."))
            } else {
                breakCharm(itemName)
                message = messages.get(MessageKeys.YOU_HAVE_BROKEN_YOUR_CHARM).replace("<charm>", itemName)
            }
            newItem.itemMeta = this
        }

        player.inventory.setItemInMainHand(newItem)
        player.updateInventory()
        charmsManager.updateCharmEffects(player)
        player.sendMessage(message)
    }

    private fun ItemMeta.restoreCharm(item: ItemStack) {
        val originalName = pdc.get(keyChain.brokenCharmOriginalNameKey, PersistentDataType.STRING)
            ?.let { GsonComponentSerializer.gson().deserialize(it) }
            ?: item.displayName()

        pdc.remove(keyChain.brokenCharmKey)
        pdc.remove(keyChain.brokenCharmOriginalNameKey)
        displayName(originalName)
    }

    private fun ItemMeta.breakCharm(itemName: Component) {
        val originalName = GsonComponentSerializer.gson().serialize(itemName)
        pdc.set(keyChain.brokenCharmKey, PersistentDataType.SHORT, 1)
        pdc.set(keyChain.brokenCharmOriginalNameKey, PersistentDataType.STRING, originalName)
        displayName(itemName.append(messages.get(MessageKeys.BROKEN_CHARM_SUFFIX)))
    }

    override fun onCommandByConsole(sender: CommandSender, alias: String, strings: Array<String>) {
        sender.sendMessage(messages.get(MessageKeys.CONSOLE_CANNOT_USE))
    }

    override fun getCompletor(sender: CommandSender, length: Int, hint: String, strings: Array<String>): List<String>
        = emptyList()
}
