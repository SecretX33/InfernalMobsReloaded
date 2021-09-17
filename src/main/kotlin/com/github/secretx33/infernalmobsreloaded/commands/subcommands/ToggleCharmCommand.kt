package com.github.secretx33.infernalmobsreloaded.commands.subcommands

import com.github.secretx33.infernalmobsreloaded.config.MessageKeys
import com.github.secretx33.infernalmobsreloaded.config.Messages
import com.github.secretx33.infernalmobsreloaded.config.replace
import com.github.secretx33.infernalmobsreloaded.config.toPlainText
import com.github.secretx33.infernalmobsreloaded.manager.CharmsManager
import com.github.secretx33.infernalmobsreloaded.model.KeyChain
import com.github.secretx33.infernalmobsreloaded.repositories.CharmsRepo
import com.github.secretx33.infernalmobsreloaded.utils.extension.displayName
import com.github.secretx33.infernalmobsreloaded.utils.extension.isAir
import com.github.secretx33.infernalmobsreloaded.utils.extension.pdc


import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType
import toothpick.ktp.delegate.inject

class ToggleCharmCommand: SubCommand() {

    override val name: String = "togglecharm"
    override val permission: String = "charms.toggle"
    override val aliases: List<String> = listOf(name, "togglec", "tcharm", "tc")

    private val messages by inject<Messages>()
    private val keyChain by inject<KeyChain>()
    private val charmsRepo by inject<CharmsRepo>()
    private val charmsManager by inject<CharmsManager>()

    override fun onCommandByPlayer(player: Player, alias: String, strings: Array<String>) {
        val intention = player.getIntention()

        // player is not holding a charm
        if(intention == Intention.NOT_A_CHARM) {
            player.sendMessage(messages.get(MessageKeys.NOT_HOLDING_CHARM))
            return
        }

        // player dont have permission to break charms
        if(intention == Intention.BREAK_BUT_NO_PERMISSION) {
            player.sendMessage(messages.get(MessageKeys.YOU_DONT_HAVE_PERMISSION_TO_BREAK_CHARMS))
            return
        }

        // player dont have permission to restore/fix charms
        if(intention == Intention.RESTORE_BUT_NO_PERMISSION) {
            player.sendMessage(messages.get(MessageKeys.YOU_DONT_HAVE_PERMISSION_TO_RESTORE_CHARMS))
            return
        }

        val item = player.inventory.itemInMainHand
        val message: Component
        val itemName = item.displayName
        val newMeta = item.itemMeta

        message = when(intention) {
            Intention.RESTORE -> {
                newMeta.restoreCharm(item)
                messages.get(MessageKeys.YOU_HAVE_RESTORED_YOUR_CHARM).replace("<charm>", newMeta.displayName()
                    ?: throw IllegalStateException("Item ${item.type} doesn't have a display name after being restored by ${player.name}, this should not happen."))
            }
            Intention.BREAK -> {
                newMeta.breakCharm(itemName)
                messages.get(MessageKeys.YOU_HAVE_BROKEN_YOUR_CHARM).replace("<charm>", itemName)
            }
            else -> throw IllegalStateException("Intention $intention should have been treated before entering this 'when' block, but it was not.")
        }

        val newItem = item.clone().apply { itemMeta = newMeta }
        player.inventory.setItemInMainHand(newItem)
        player.updateInventory()
        charmsManager.updateCharmEffects(player)
        player.sendMessage(message)
    }

    private fun Player.getIntention(): Intention {
        val heldItem = inventory.itemInMainHand

        // player not holding a charm
        if(heldItem.isAir() || !charmsRepo.isItemRequiredByCharmEffect(heldItem, acceptBroken = true)) {
            return Intention.NOT_A_CHARM
        }

        val isItemBroken = heldItem.itemMeta?.pdc?.has(keyChain.brokenCharmKey, PersistentDataType.SHORT) == true

        return when {
            !isItemBroken && !hasPermission("imr.charms.toggle.break") -> Intention.BREAK_BUT_NO_PERMISSION
            !isItemBroken -> Intention.BREAK
            isItemBroken && !hasPermission("imr.charms.toggle.restore") -> Intention.RESTORE_BUT_NO_PERMISSION
            isItemBroken -> Intention.RESTORE
            else -> throw IllegalStateException("Unknown Intention for item ${heldItem.displayName.toPlainText()}")
        }
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

    private enum class Intention {
        BREAK, RESTORE, NOT_A_CHARM, BREAK_BUT_NO_PERMISSION, RESTORE_BUT_NO_PERMISSION
    }
}
