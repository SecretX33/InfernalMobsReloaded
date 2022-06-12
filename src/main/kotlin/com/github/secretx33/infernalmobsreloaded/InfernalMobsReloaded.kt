package com.github.secretx33.infernalmobsreloaded

import com.comphenix.protocol.ProtocolLibrary
import com.github.secretx33.infernalmobsreloaded.annotation.SkipAutoRegistration
import com.github.secretx33.infernalmobsreloaded.commands.Commands
import com.github.secretx33.infernalmobsreloaded.commands.subcommands.SubCommand
import com.github.secretx33.infernalmobsreloaded.config.Config
import com.github.secretx33.infernalmobsreloaded.config.ConfigKeys
import com.github.secretx33.infernalmobsreloaded.eventlisteners.hook.SpawnerBreakWithSilkSpawnersListener
import com.github.secretx33.infernalmobsreloaded.eventlisteners.hook.TownyListener
import com.github.secretx33.infernalmobsreloaded.eventlisteners.spawner.SpawnerBreakListener
import com.github.secretx33.infernalmobsreloaded.filter.InfernalDeathConsoleMessageFilter
import com.github.secretx33.infernalmobsreloaded.manager.AbilityHelper
import com.github.secretx33.infernalmobsreloaded.manager.BossBarManager
import com.github.secretx33.infernalmobsreloaded.manager.CharmsManager
import com.github.secretx33.infernalmobsreloaded.manager.InfernalMobsManager
import com.github.secretx33.infernalmobsreloaded.manager.hook.WorldGuardChecker
import com.github.secretx33.infernalmobsreloaded.manager.hook.WorldGuardCheckerDummy
import com.github.secretx33.infernalmobsreloaded.manager.hook.WorldGuardCheckerImpl
import com.github.secretx33.infernalmobsreloaded.model.Ability.Companion.getOrNull
import com.github.secretx33.infernalmobsreloaded.packetlisteners.InvisibleEntitiesEquipVanisherListener
import com.github.secretx33.infernalmobsreloaded.utils.extension.findClasses
import com.github.secretx33.infernalmobsreloaded.utils.extension.replace
import com.github.secretx33.infernalmobsreloaded.utils.other.Metrics
import com.google.common.reflect.ClassPath
import me.mattstudios.msg.adventure.AdventureMessage
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.Filter
import org.bukkit.Bukkit
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.PluginDescriptionFile
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.plugin.java.JavaPluginLoader
import toothpick.Scope
import toothpick.configuration.Configuration
import toothpick.ktp.KTP
import toothpick.ktp.binding.bind
import toothpick.ktp.binding.module
import toothpick.ktp.extension.getInstance
import java.io.File
import java.util.logging.Logger
import kotlin.reflect.KClass
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.isSuperclassOf

open class InfernalMobsReloaded : JavaPlugin {

    constructor() : super()

    constructor(
        loader: JavaPluginLoader,
        description: PluginDescriptionFile,
        dataFolder: File,
        file: File,
    ) : super(loader, description, dataFolder, file)

    private val mod = module {
        bind<Plugin>().toInstance(this@InfernalMobsReloaded)
        bind<JavaPlugin>().toInstance(this@InfernalMobsReloaded)
        bind<Logger>().toInstance(logger)
        bind<Int>().withName("metricsServiceId").toInstance(11253)
        bind<Set<KClass<out SubCommand>>>().toInstance(findClasspathSubcommands())
        bind<AdventureMessage>().toInstance(AdventureMessage.create())
    }

    override fun onLoad() {
        if(isWorldGuardEnabled) {
            mod.bind<WorldGuardChecker>().toInstance(WorldGuardCheckerImpl())
        } else {
            mod.bind<WorldGuardChecker>().toInstance(WorldGuardCheckerDummy())
        }
    }

    override fun onEnable() {
        KTP.setConfiguration(Configuration.forDevelopment())
        _scope = KTP.openScope(this) {
            it.installModules(module {
                bind<Scope>().toInstance(it)
            })
        }
        findAndRegisterClasspathListeners()
        scope.apply {
            registerLoggerFilters(getInstance<InfernalDeathConsoleMessageFilter>())
            getInstance<Commands>()
            getInstance<Metrics>()
            getInstance<InfernalMobsManager>().loadAllInfernals()
            getInstance<BossBarManager>().showBarsOfNearbyInfernalsForAllPlayers()
            getInstance<CharmsManager>().startAllCharmTasks()
        }
    }

    override fun onDisable() {
        scope.apply {
            getInstance<InfernalMobsManager>().unloadAllInfernals()
            getInstance<BossBarManager>().hideAllBarsFromAllPlayers()
            getInstance<CharmsManager>().stopAllCharmTasks()
            getInstance<AbilityHelper>().revertPendingBlockModifications()
        }
        getOrNull<TownyListener>()?.cancelRemovalTasks()
        KTP.closeScope(this)
        _scope = null
    }

    /**
     * Responsible for finding and registering all event listeners in the classpath, respecting configurations and
     * any other imposed condition for their registral.
     */
    private fun findAndRegisterClasspathListeners() {
        val listeners = findClasspathListeners()
            .filterNotTo(mutableSetOf()) { it.hasAnnotation<SkipAutoRegistration>() }

        if (silkSpawnerHandlesSpawnerDrops) {
            logger.info("Enabling SilkSpawners hook.")
            listeners.replace(SpawnerBreakListener::class, SpawnerBreakWithSilkSpawnersListener::class)
        }
        if (isTownyHookEnabled) {
            logger.info("Enabling Towny hook.")
            listeners += TownyListener::class
        }

        listeners.map { scope.getInstance(it.java) }
            .forEach { Bukkit.getPluginManager().registerEvents(it, this) }

        if (isProtocolLibEnabled) {
            logger.info("Enabling ProtocolLib hook.")
            val packetListeners = setOf(InvisibleEntitiesEquipVanisherListener::class)

            packetListeners.map { scope.getInstance(it.java) }
                .forEach(ProtocolLibrary.getProtocolManager()::addPacketListener)
        }
    }

    private fun registerLoggerFilters(vararg filters: Filter) {
        val rootLogger = LogManager.getRootLogger() as? org.apache.logging.log4j.core.Logger ?: return
        filters.forEach(rootLogger::addFilter)
    }

    private fun findClasspathListeners(): Set<KClass<out Listener>> =
        findClasses("$PLUGIN_PACKAGE.eventlisteners") { !it.isAbstract && Listener::class.isSuperclassOf(it) }

    private fun findClasspathSubcommands(): Set<KClass<out SubCommand>> =
        findClasses("$PLUGIN_PACKAGE.commands") { !it.isAbstract && SubCommand::class.isSuperclassOf(it) }

    private val isWorldGuardEnabled
        get() = Bukkit.getPluginManager().getPlugin("WorldGuard") != null

    private val isProtocolLibEnabled
        get() = Bukkit.getPluginManager().isPluginEnabled("ProtocolLib")

    private val isTownyHookEnabled
        get() = Bukkit.getPluginManager().isPluginEnabled("Towny")
                && scope.getInstance<Config>().get(ConfigKeys.TOWNY_REMOVE_INFERNAL_IN_TOWNS)

    private val silkSpawnerHandlesSpawnerDrops
        get() = Bukkit.getPluginManager().isPluginEnabled("SilkSpawners")
                && scope.getInstance<Config>().get(ConfigKeys.SILKSPAWNERS_HANDLES_SPAWNER_DROP)

    companion object {
        private const val PLUGIN_PACKAGE = "com.github.secretx33.infernalmobsreloaded"
        private var _scope: Scope? = null
        val scope: Scope get() = _scope!!
    }
}
