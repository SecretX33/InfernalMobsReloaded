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
import com.github.secretx33.infernalmobsreloaded.utils.other.WrappedInjector
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class ReloadCommand(injector: WrappedInjector) : SubCommand() {

    override val name: String = "reload"
    override val permission: String = "reload"
    override val aliases: List<String> = listOf(name, "rel", "r")

    private val config = injector.getInstance<Config>()
    private val messages = injector.getInstance<Messages>()
    private val abilityConfig = injector.getInstance<AbilityConfig>()
    private val lootItemsRepo = injector.getInstance<LootItemsRepo>()
    private val globalDropsRepo = injector.getInstance<GlobalDropsRepo>()
    private val infernalMobTypesRepo = injector.getInstance<InfernalMobTypesRepo>()
    private val charmsRepo = injector.getInstance<CharmsRepo>()
    private val infernalMobsManager = injector.getInstance<InfernalMobsManager>()
    private val bossBarManager = injector.getInstance<BossBarManager>()
    private val charmsManager = injector.getInstance<CharmsManager>()

    override fun onCommandByPlayer(player: Player, alias: String, strings: Array<String>) {
        onCommandByConsole(player, alias, strings)
    }

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

    override fun getCompletor(sender: CommandSender, length: Int, hint: String, strings: Array<String>): List<String> {
        return emptyList()
    }
}
