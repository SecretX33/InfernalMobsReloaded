package com.github.secretx33.infernalmobsreloaded.command.subcommand

import com.github.secretx33.infernalmobsreloaded.config.AbilityConfig
import com.github.secretx33.infernalmobsreloaded.config.Config
import com.github.secretx33.infernalmobsreloaded.config.MessageKeys
import com.github.secretx33.infernalmobsreloaded.config.Messages
import com.github.secretx33.infernalmobsreloaded.manager.BossBarManager
import com.github.secretx33.infernalmobsreloaded.manager.CharmsManager
import com.github.secretx33.infernalmobsreloaded.manager.InfernalMobsManager
import com.github.secretx33.infernalmobsreloaded.repository.CharmsRepo
import com.github.secretx33.infernalmobsreloaded.repository.GlobalDropsRepo
import com.github.secretx33.infernalmobsreloaded.repository.InfernalMobTypesRepo
import com.github.secretx33.infernalmobsreloaded.repository.LootItemsRepo
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class ReloadCommand(
    private val config: Config,
    private val messages: Messages,
    private val abilityConfig: AbilityConfig,
    private val lootItemsRepo: LootItemsRepo,
    private val globalDropsRepo: GlobalDropsRepo,
    private val infernalMobTypesRepo: InfernalMobTypesRepo,
    private val charmsRepo: CharmsRepo,
    private val infernalMobsManager: InfernalMobsManager,
    private val bossBarManager: BossBarManager,
    private val charmsManager: CharmsManager,
) : SubCommand() {

    override val name: String = "reload"
    override val permission: String = "reload"
    override val aliases: List<String> = listOf(name, "rel", "r")

    override fun onCommandByPlayer(player: Player, alias: String, strings: Array<String>) =
        onCommandByConsole(player, alias, strings)

    override fun onCommandByConsole(sender: CommandSender, alias: String, strings: Array<String>) {
        infernalMobsManager.unloadAllInfernals()
        bossBarManager.hideAllBarsFromAllPlayers()
        config.reload()
        messages.reload()
        abilityConfig.reload()
        lootItemsRepo.reload()
        globalDropsRepo.reload()
        infernalMobTypesRepo.reload()
        charmsRepo.reload()
        infernalMobsManager.loadAllInfernals()
        bossBarManager.showBarsOfNearbyInfernalsForAllPlayers()
        charmsManager.reload()
        sender.sendMessage(messages.get(MessageKeys.CONFIGS_RELOADED))
    }
}
