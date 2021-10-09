package com.github.secretx33.infernalmobsreloaded.commands.subcommands

import com.github.secretx33.infernalmobsreloaded.config.AbilityConfig
import com.github.secretx33.infernalmobsreloaded.config.Config
import com.github.secretx33.infernalmobsreloaded.config.MessageKeys
import com.github.secretx33.infernalmobsreloaded.config.Messages
import com.github.secretx33.infernalmobsreloaded.manager.BossBarManager
import com.github.secretx33.infernalmobsreloaded.manager.CharmsManager
import com.github.secretx33.infernalmobsreloaded.manager.InfernalMobsManager
import com.github.secretx33.infernalmobsreloaded.repositories.CharmsRepo
import com.github.secretx33.infernalmobsreloaded.repositories.GlobalDropsRepo
import com.github.secretx33.infernalmobsreloaded.repositories.InfernalMobTypesRepo
import com.github.secretx33.infernalmobsreloaded.repositories.LootItemsRepo
import com.github.secretx33.infernalmobsreloaded.utils.other.CustomKoinComponent
import com.github.secretx33.infernalmobsreloaded.utils.other.inject
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class ReloadCommand : SubCommand(), CustomKoinComponent {

    override val name: String = "reload"
    override val permission: String = "reload"
    override val aliases: List<String> = listOf(name, "rel", "r")

    private val config by inject<Config>()
    private val messages by inject<Messages>()
    private val abilityConfig by inject<AbilityConfig>()
    private val lootItemsRepo by inject<LootItemsRepo>()
    private val globalDropsRepo by inject<GlobalDropsRepo>()
    private val infernalMobTypesRepo by inject<InfernalMobTypesRepo>()
    private val charmsRepo by inject<CharmsRepo>()
    private val infernalMobsManager by inject<InfernalMobsManager>()
    private val bossBarManager by inject<BossBarManager>()
    private val charmsManager by inject<CharmsManager>()

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
