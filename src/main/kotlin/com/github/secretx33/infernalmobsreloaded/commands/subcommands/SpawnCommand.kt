package com.github.secretx33.infernalmobsreloaded.commands.subcommands

import com.github.secretx33.infernalmobsreloaded.config.MessageKeys
import com.github.secretx33.infernalmobsreloaded.config.Messages
import com.github.secretx33.infernalmobsreloaded.config.toComponent
import com.github.secretx33.infernalmobsreloaded.events.InfernalSpawnEvent
import com.github.secretx33.infernalmobsreloaded.model.Abilities
import com.github.secretx33.infernalmobsreloaded.repositories.InfernalMobTypesRepo
import com.github.secretx33.infernalmobsreloaded.utils.CustomKoinComponent
import com.github.secretx33.infernalmobsreloaded.utils.inject
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason
import org.koin.core.component.KoinApiExtension

@KoinApiExtension
class SpawnCommand: SubCommand(), CustomKoinComponent {

    override val name: String = "spawn"
    override val permission: String = "spawn"
    override val aliases: List<String> = listOf(name, "spaw", "sp", "s")

    private val messages by inject<Messages>()
    private val infernalMobTypesRepo by inject<InfernalMobTypesRepo>()

    override fun onCommandByPlayer(player: Player, alias: String, strings: Array<String>) {
        if(strings.size < 2) {
            player.sendMessage("Usage: /$alias $name <entity_type> [abilities]".toComponent(NamedTextColor.RED))
            return
        }

        // if infernal type doesn't exist
        val infernalType = infernalMobTypesRepo.getInfernalTypeOrNull(strings[1]) ?: run {
            player.sendMessage(messages.get(MessageKeys.INFERNAL_MOB_TYPE_DOESNT_EXIST).replaceText { it.match("<type>").replacement(strings[1]) })
            return
        }
        val abilities = HashSet<Abilities>()

        // if player specified infernal abilities in command
        if(strings.size > 2) {
            for(i in 2..strings.lastIndex) {
                val ability = Abilities.getOrNull(strings[i]) ?: run {
                    player.sendMessage(messages.get(MessageKeys.ABILITY_DOESNT_EXIST)
                        .replaceText { it.match("<ability>").replacement(strings[i]) })
                    return
                }
                abilities.add(ability)
            }
        }

        // spawns the new mob as infernal mob
        val block = player.getTargetBlock(null, 5)
        player.world.spawnEntity(block.location.add(0.5, 1.2, 0.5), infernalType.entityType, SpawnReason.CUSTOM) { entity ->
            if(entity !is LivingEntity) return@spawnEntity
            Bukkit.getPluginManager().callEvent(InfernalSpawnEvent(entity, infernalType, randomAbilities = abilities.isEmpty(), abilitySet = abilities))
        }
    }

    override fun onCommandByConsole(sender: CommandSender, alias: String, strings: Array<String>) {
        sender.sendMessage(messages.get(MessageKeys.CONSOLE_CANNOT_USE))
    }

    override fun getCompletor(sender: CommandSender, length: Int, hint: String, strings: Array<String>): List<String> {
        if(sender !is Player || length < 2) return emptyList()

        if(length == 2) return infernalMobTypesRepo.getAllInfernalTypeNames().filter { it.startsWith(hint, ignoreCase = true) }

        return Abilities.lowercasedValues.filter { it.startsWith(hint, ignoreCase = true) }
    }
}
