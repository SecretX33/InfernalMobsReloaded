package com.github.secretx33.infernalmobsreloaded.commands.subcommands

import com.github.secretx33.infernalmobsreloaded.config.MessageKeys
import com.github.secretx33.infernalmobsreloaded.config.Messages
import com.github.secretx33.infernalmobsreloaded.config.replace
import com.github.secretx33.infernalmobsreloaded.config.toComponent
import com.github.secretx33.infernalmobsreloaded.manager.InfernalMobsManager
import com.github.secretx33.infernalmobsreloaded.utils.CustomKoinComponent
import com.github.secretx33.infernalmobsreloaded.utils.formattedTypeName
import com.github.secretx33.infernalmobsreloaded.utils.getTarget
import com.github.secretx33.infernalmobsreloaded.utils.inject
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class InspectCommand : SubCommand(), CustomKoinComponent {

    override val name: String = "inspect"
    override val permission: String = "inspect"
    override val aliases: List<String> = listOf(name, "insp", "ins", "in", "i")

    private val messages by inject<Messages>()
    private val mobsManager by inject<InfernalMobsManager>()

    override fun onCommandByPlayer(player: Player, alias: String, strings: Array<String>) {
        // if player is not targeting an entity
        val target = player.getTarget(25)
            ?: player.getTargetBlock(null, 25).let { block -> block.world.getNearbyLivingEntities(block.location, 6.0, 6.0) { it.isValid && !it.isDead && mobsManager.isPossibleInfernalMob(it) }.firstOrNull() } ?: run {
            player.sendMessage(messages.get(MessageKeys.NOT_TARGETING_LIVING_ENTITY))
            return
        }

        // if targeted entity is not an infernal entity
        if(!mobsManager.isPossibleInfernalMob(target)) {
            player.sendMessage(messages.get(MessageKeys.NOT_TARGETING_INFERNAL))
            return
        }
        val group = mobsManager.getInfernalGroupNameOrNull(target) ?: return

        // if targeted entity is not an infernal entity
        if(!mobsManager.isValidInfernalMob(target)) {
            player.sendMessage(messages.get(MessageKeys.NOT_TARGETING_VALID_INFERNAL).replace("<group>", group))
            return
        }
        val abilitiesString = mobsManager.getInfernalAbilities(target).joinToString { it.displayName }.takeIf { it.isNotBlank() }?.toComponent() ?: "<none>".toComponent(NamedTextColor.GRAY)

        // send the list of abilities the mob he's targeting has
        player.sendMessage(messages.get(MessageKeys.TARGETING_INFERNAL)
            .replace("<entity>", target.formattedTypeName())
            .replace("<abilities>", abilitiesString))
    }

    override fun onCommandByConsole(sender: CommandSender, alias: String, strings: Array<String>) {
        sender.sendMessage(messages.get(MessageKeys.CONSOLE_CANNOT_USE))
    }

    override fun getCompletor(sender: CommandSender, length: Int, hint: String, strings: Array<String>): List<String> {
        return emptyList()
    }
}
