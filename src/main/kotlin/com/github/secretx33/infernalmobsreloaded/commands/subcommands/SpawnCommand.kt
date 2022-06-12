package com.github.secretx33.infernalmobsreloaded.commands.subcommands

import com.github.secretx33.infernalmobsreloaded.config.MessageKeys
import com.github.secretx33.infernalmobsreloaded.config.Messages
import com.github.secretx33.infernalmobsreloaded.config.replace
import com.github.secretx33.infernalmobsreloaded.config.toComponent
import com.github.secretx33.infernalmobsreloaded.events.InfernalSpawnEvent
import com.github.secretx33.infernalmobsreloaded.manager.BossBarManager
import com.github.secretx33.infernalmobsreloaded.manager.CharmsManager
import com.github.secretx33.infernalmobsreloaded.manager.InfernalMobsManager
import com.github.secretx33.infernalmobsreloaded.model.Ability
import com.github.secretx33.infernalmobsreloaded.model.KeyChain
import com.github.secretx33.infernalmobsreloaded.repositories.InfernalMobTypesRepo
import com.github.secretx33.infernalmobsreloaded.repositories.LootItemsRepo
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason

class SpawnCommand(
    private val messages: Messages,
    private val infernalMobTypesRepo: InfernalMobTypesRepo,
    private val bossBarManager: BossBarManager,
) : SubCommand() {

    override val name: String = "spawn"
    override val permission: String = "spawn"
    override val aliases: List<String> = listOf(name, "summon", "sum", "spaw", "sp", "s")

    override fun onCommandByPlayer(player: Player, alias: String, strings: Array<String>) {
        if(strings.size < 2) {
            player.sendMessage("Usage: /$alias $name <entity_type> [abilities]".toComponent(NamedTextColor.RED))
            return
        }

        // if infernal type doesn't exist
        val infernalType = infernalMobTypesRepo.getInfernalTypeOrNull(strings[1]) ?: run {
            player.sendMessage(messages.get(MessageKeys.INFERNAL_MOB_TYPE_DOESNT_EXIST).replace("<type>", strings[1].toComponent(NamedTextColor.GOLD)))
            return
        }
        val abilities = HashSet<Ability>()

        // if player specified infernal abilities in command
        if(strings.size > 2) {
            for(i in 2..strings.lastIndex) {
                val ability = Ability.getOrNull(strings[i]) ?: run {
                    player.sendMessage(messages.get(MessageKeys.ABILITY_DOESNT_EXIST).replace("<ability>", strings[i].toComponent(NamedTextColor.GOLD)))
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
            bossBarManager.showBossBarForNearbyPlayers(entity)
        }
    }

    override fun onCommandByConsole(sender: CommandSender, alias: String, strings: Array<String>) {
        sender.sendMessage(messages.get(MessageKeys.CONSOLE_CANNOT_USE))
    }

    override fun getCompletor(sender: CommandSender, length: Int, hint: String, strings: Array<String>): List<String> {
        if(sender !is Player || length < 2) return emptyList()

        if(length == 2) return infernalMobTypesRepo.getAllInfernalTypeNames().filter { it.startsWith(hint, ignoreCase = true) }

        return Ability.LOWERCASE_VALUES.filter { it.startsWith(hint, ignoreCase = true) }
    }
}
