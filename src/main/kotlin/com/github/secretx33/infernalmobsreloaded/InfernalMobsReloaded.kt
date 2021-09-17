package com.github.secretx33.infernalmobsreloaded

import com.github.secretx33.infernalmobsreloaded.annotations.PluginId
import com.github.secretx33.infernalmobsreloaded.config.Config
import com.github.secretx33.infernalmobsreloaded.config.ConfigKeys
import com.github.secretx33.infernalmobsreloaded.eventlisteners.integration.TownyListener
import com.github.secretx33.infernalmobsreloaded.manager.AbilityHelper
import com.github.secretx33.infernalmobsreloaded.manager.BossBarManager
import com.github.secretx33.infernalmobsreloaded.manager.CharmsManager
import com.github.secretx33.infernalmobsreloaded.manager.InfernalMobsManager
import com.github.secretx33.infernalmobsreloaded.manager.WorldGuardChecker
import com.github.secretx33.infernalmobsreloaded.manager.WorldGuardCheckerDummy
import com.github.secretx33.infernalmobsreloaded.manager.WorldGuardCheckerImpl
import com.github.secretx33.infernalmobsreloaded.packetlisteners.InvisibleEntitiesEquipVanisherListener
import com.github.secretx33.infernalmobsreloaded.scanning.Rules
import com.github.secretx33.infernalmobsreloaded.scanning.implementations.ZISScanner
import com.github.secretx33.infernalmobsreloaded.utils.other.Metrics
import me.mattstudios.msg.adventure.AdventureMessage
import org.bukkit.Bukkit
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.PluginDescriptionFile
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.plugin.java.JavaPluginLoader
import toothpick.Scope
import toothpick.ktp.KTP
import toothpick.ktp.binding.bind
import toothpick.ktp.binding.module
import toothpick.ktp.extension.getInstance
import java.io.File
import java.util.logging.Logger

open class InfernalMobsReloaded: JavaPlugin {

    constructor() : super()

    constructor(
        loader: JavaPluginLoader,
        description: PluginDescriptionFile,
        dataFolder: File,
        file: File
    ) : super(loader, description, dataFolder, file)

    private val mod = module {
        bind<Plugin>().toInstance(this@InfernalMobsReloaded)
        bind<JavaPlugin>().toInstance(this@InfernalMobsReloaded)
        bind<Logger>().toInstance(this@InfernalMobsReloaded.logger)
        bind<AdventureMessage>().toInstance(AdventureMessage.create())
        bind<Int>().withName(PluginId::class).toInstance(11253)
    }

    override fun onLoad() {
        // if worldguard is enabled, replace dummy module with real one
        if(isWorldGuardEnabled) {
            // creation of the WorldGuardChecker happens here because WG is bae and requires hooking to happen on method onLoad
            mod.bind<WorldGuardChecker>().toClass<WorldGuardCheckerImpl>()
        } else {
            mod.bind<WorldGuardChecker>().toClass<WorldGuardCheckerDummy>()
        }
    }

    private lateinit var scope: Scope

    override fun onEnable() {
        scope = KTP.openScope("InfernalMobsReloaded").installModules(mod)
        if(isProtocolLibEnabled)
            scope.getInstance<InvisibleEntitiesEquipVanisherListener>()
        if(isTownyHookEnabled) {
            logger.info("Enabling Towny hook.")
            scope.getInstance<TownyListener>()
        }
        scope.getInstance<Metrics>()
        scope.getInstance<InfernalMobsManager>().loadAllInfernals()
        scope.getInstance<BossBarManager>().showBarsOfNearbyInfernalsForAllPlayers()
        scope.getInstance<CharmsManager>().startAllCharmTasks()
        val scanner = ZISScanner.create(this::class.java, "com.github.secretx33.infernalmobsreloaded")
        val rules = Rules.builder<Any>().typeExtends(Listener::class.java).disallowMutableClasses().build()
        scanner.classes(rules)
            .map { scope.getInstance(it) }
            .forEach { Bukkit.getPluginManager().registerEvents(it, this) }
    }

    override fun onDisable() {
        if(!KTP.isScopeOpen("InfernalMobsReloaded")) return
        scope.apply {
            getInstance<InfernalMobsManager>().unloadAllInfernals()
            getInstance<BossBarManager>().hideAllBarsFromAllPlayers()
            getInstance<CharmsManager>().stopAllCharmTasks()
            getInstance<AbilityHelper>().revertPendingBlockModifications()
            if(isTownyHookEnabled) getInstance<TownyListener>().cancelRemovalTasks()
        }
        KTP.closeScope("InfernalMobsReloaded")
    }

    private val isWorldGuardEnabled
        get() = Bukkit.getPluginManager().getPlugin("WorldGuard") != null

    private val isProtocolLibEnabled
        get() = Bukkit.getPluginManager().isPluginEnabled("ProtocolLib")

    private val isTownyHookEnabled
        get() = Bukkit.getPluginManager().isPluginEnabled("Towny") && scope.getInstance<Config>().get(ConfigKeys.TOWNY_REMOVE_INFERNAL_IN_TOWNS)
}
