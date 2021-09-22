package com.github.secretx33.infernalmobsreloaded

import com.github.secretx33.infernalmobsreloaded.annotations.OptionalListener
import com.github.secretx33.infernalmobsreloaded.config.Config
import com.github.secretx33.infernalmobsreloaded.config.ConfigKeys
import com.github.secretx33.infernalmobsreloaded.eventlisteners.integration.TownyListener
import com.github.secretx33.infernalmobsreloaded.manager.AbilityHelper
import com.github.secretx33.infernalmobsreloaded.manager.BossBarManager
import com.github.secretx33.infernalmobsreloaded.manager.CharmsManager
import com.github.secretx33.infernalmobsreloaded.manager.InfernalMobsManager
import com.github.secretx33.infernalmobsreloaded.module.GuiceModule
import com.github.secretx33.infernalmobsreloaded.packetlisteners.InvisibleEntitiesEquipVanisherListener
import com.github.secretx33.infernalmobsreloaded.scanning.Rules
import com.github.secretx33.infernalmobsreloaded.scanning.implementations.ZISScanner
import com.github.secretx33.infernalmobsreloaded.utils.extension.Utils
import com.github.secretx33.infernalmobsreloaded.utils.other.Metrics
import com.github.secretx33.infernalmobsreloaded.utils.other.WrappedInjector
import org.bukkit.Bukkit
import org.bukkit.event.Listener
import org.bukkit.plugin.PluginDescriptionFile
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.plugin.java.JavaPluginLoader
import java.io.File

open class InfernalMobsReloaded: JavaPlugin {

    constructor() : super()

    constructor(
        loader: JavaPluginLoader,
        description: PluginDescriptionFile,
        dataFolder: File,
        file: File
    ) : super(loader, description, dataFolder, file)

    override fun onLoad() {
        // if worldguard is enabled, replace dummy module with real one
        if(isWorldGuardEnabled) {
            // creation of the WorldGuardChecker happens here because WG is bae and requires hooking to happen on method onLoad
//            mod.bind<WorldGuardChecker>().toClass<WorldGuardCheckerImpl>()
        } else {
//            mod.bind<WorldGuardChecker>().toClass<WorldGuardCheckerDummy>()
        }
    }

    private lateinit var injector: WrappedInjector

    override fun onEnable() {
        injector = WrappedInjector.getGuiceInjector(GuiceModule(this)).apply {
            Utils.keyChain = getInstance()
            if(isProtocolLibEnabled)
                getInstance<InvisibleEntitiesEquipVanisherListener>()
            if(isTownyHookEnabled) {
                logger.info("Enabling Towny hook.")
                getInstance<TownyListener>()
            }
            getInstance<Metrics>()
            getInstance<InfernalMobsManager>().loadAllInfernals()
            getInstance<BossBarManager>().showBarsOfNearbyInfernalsForAllPlayers()
            getInstance<CharmsManager>().startAllCharmTasks()
        }
        val scanner = ZISScanner.create(this::class.java, "com.github.secretx33.infernalmobsreloaded")
        val rules = Rules.builder<Any>().typeExtends(Listener::class.java)
            .doesntHaveAnnotation(OptionalListener::class.java)
            .disallowMutableClasses()
            .build()
        scanner.classes(rules)
            .map { injector.getInstance(it) }
            .forEach { Bukkit.getPluginManager().registerEvents(it, this) }
    }

    override fun onDisable() {
        injector.apply {
            getInstance<InfernalMobsManager>().unloadAllInfernals()
            getInstance<BossBarManager>().hideAllBarsFromAllPlayers()
            getInstance<CharmsManager>().stopAllCharmTasks()
            getInstance<AbilityHelper>().revertPendingBlockModifications()
            if(isTownyHookEnabled) getInstance<TownyListener>().cancelRemovalTasks()
        }
    }

    private val isWorldGuardEnabled
        get() = Bukkit.getPluginManager().getPlugin("WorldGuard") != null

    private val isProtocolLibEnabled
        get() = Bukkit.getPluginManager().isPluginEnabled("ProtocolLib")

    private val isTownyHookEnabled
        get() = Bukkit.getPluginManager().isPluginEnabled("Towny") && injector.getInstance(Config::class.java).get(ConfigKeys.TOWNY_REMOVE_INFERNAL_IN_TOWNS)
}
