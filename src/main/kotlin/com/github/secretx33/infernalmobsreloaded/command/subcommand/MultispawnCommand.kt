package com.github.secretx33.infernalmobsreloaded.command.subcommand

import com.github.secretx33.infernalmobsreloaded.config.MessageKeys
import com.github.secretx33.infernalmobsreloaded.config.Messages
import com.github.secretx33.infernalmobsreloaded.config.replace
import com.github.secretx33.infernalmobsreloaded.config.toComponent
import com.github.secretx33.infernalmobsreloaded.event.InfernalSpawnEvent
import com.github.secretx33.infernalmobsreloaded.manager.BossBarManager
import com.github.secretx33.infernalmobsreloaded.model.Ability
import com.github.secretx33.infernalmobsreloaded.repository.InfernalMobTypesRepo
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.entity.CreatureSpawnEvent
import kotlin.math.min

class MultispawnCommand(
    private val messages: Messages,
    private val infernalMobTypesRepo: InfernalMobTypesRepo,
    private val bossBarManager: BossBarManager,
) : SubCommand() {

    override val name: String = "multispawn"
    override val permission: String = "multispawn"
    override val aliases: List<String> = listOf(name, "mspawn", "multis", "ms")

    override fun onCommandByPlayer(player: Player, alias: String, strings: Array<String>) {
        if(strings.size < 3) {
            player.sendMessage("Usage: /$alias $name <amount> <entity_type> [abilities]".toComponent(NamedTextColor.RED))
            return
        }

        // if number is invalid
        val amount = strings[1].toIntOrNull() ?: run {
            player.sendMessage(messages.get(MessageKeys.INVALID_NUMBER).replace("<number>", strings[1].toComponent(NamedTextColor.GOLD)))
            return
        }

        // if infernal type doesn't exist
        val infernalType = infernalMobTypesRepo.getInfernalTypeOrNull(strings[2]) ?: run {
            player.sendMessage(messages.get(MessageKeys.INFERNAL_MOB_TYPE_DOESNT_EXIST).replace("<type>", strings[2].toComponent(NamedTextColor.GOLD)))
            return
        }
        val abilities = HashSet<Ability>()

        // if player specified infernal abilities in command
        if(strings.size > 2) {
            for(i in 3..strings.lastIndex) {
                val ability = Ability.getOrNull(strings[i]) ?: run {
                    player.sendMessage(messages.get(MessageKeys.ABILITY_DOESNT_EXIST).replace("<ability>", strings[i].toComponent(NamedTextColor.GOLD)))
                    return
                }
                abilities.add(ability)
            }
        }

        // spawns the new mob as infernal mob
        val block = player.getTargetBlock(null, 5)
        repeat(min(1000, amount)) {
            player.world.spawnEntity(block.location.add(0.5, 1.2, 0.5), infernalType.entityType, CreatureSpawnEvent.SpawnReason.CUSTOM) { entity ->
                if (entity !is LivingEntity) return@spawnEntity
                Bukkit.getPluginManager()
                    .callEvent(InfernalSpawnEvent(entity, infernalType, randomAbilities = abilities.isEmpty(), abilitySet = abilities))
                bossBarManager.showBossBarForNearbyPlayers(entity)
            }
        }
    }

    override fun onCommandByConsole(sender: CommandSender, alias: String, strings: Array<String>) {
        sender.sendMessage(messages.get(MessageKeys.CONSOLE_CANNOT_USE))
    }

    override fun getCompletor(sender: CommandSender, length: Int, hint: String, strings: Array<String>): List<String> {
        if(sender !is Player || length < 2) return emptyList()

        if(length == 2) return if(hint.isBlank()) listOf("<number>") else emptyList()
        if(length == 3) return infernalMobTypesRepo.getAllInfernalTypeNames().filter { it.startsWith(hint, ignoreCase = true) }

        return Ability.LOWERCASE_VALUES.filter { it.startsWith(hint, ignoreCase = true) }
    }
}

