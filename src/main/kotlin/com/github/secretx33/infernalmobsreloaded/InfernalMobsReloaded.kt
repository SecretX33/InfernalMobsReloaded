package com.github.secretx33.infernalmobsreloaded

import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.events.PacketAdapter
import com.github.secretx33.infernalmobsreloaded.command.Commands
import com.github.secretx33.infernalmobsreloaded.command.subcommand.SubCommand
import com.github.secretx33.infernalmobsreloaded.config.Config
import com.github.secretx33.infernalmobsreloaded.config.ConfigKeys
import com.github.secretx33.infernalmobsreloaded.eventbus.EventBus
import com.github.secretx33.infernalmobsreloaded.eventbus.internalevent.PluginLoad
import com.github.secretx33.infernalmobsreloaded.eventbus.internalevent.PluginUnload
import com.github.secretx33.infernalmobsreloaded.eventlistener.hook.SpawnerBreakWithSilkSpawnersListener
import com.github.secretx33.infernalmobsreloaded.eventlistener.hook.TownyListener
import com.github.secretx33.infernalmobsreloaded.eventlistener.spawner.SpawnerBreakListener
import com.github.secretx33.infernalmobsreloaded.filter.InfernalDeathConsoleMessageFilter
import com.github.secretx33.infernalmobsreloaded.manager.hook.WorldGuardChecker
import com.github.secretx33.infernalmobsreloaded.manager.hook.WorldGuardCheckerDummy
import com.github.secretx33.infernalmobsreloaded.manager.hook.WorldGuardCheckerImpl
import com.github.secretx33.infernalmobsreloaded.model.PluginMetricsId
import com.github.secretx33.infernalmobsreloaded.util.extension.findRegistrableClasses
import com.github.secretx33.infernalmobsreloaded.util.extension.replace
import com.github.secretx33.infernalmobsreloaded.util.other.Metrics
import me.mattstudios.msg.adventure.AdventureMessage
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.Filter
import org.bukkit.Bukkit
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin
import toothpick.Scope
import toothpick.configuration.Configuration
import toothpick.ktp.KTP
import toothpick.ktp.binding.bind
import toothpick.ktp.binding.module
import toothpick.ktp.extension.getInstance
import java.util.logging.Logger
import kotlin.reflect.KClass

class InfernalMobsReloaded : JavaPlugin() {

    private val mod = module {
        bind<Plugin>().toInstance(this@InfernalMobsReloaded)
        bind<JavaPlugin>().toInstance(this@InfernalMobsReloaded)
        bind<Logger>().toInstance(logger)
        bind<PluginMetricsId>().toInstance(PluginMetricsId(11253))
        bind<Set<KClass<out SubCommand>>>().withName("subcommands").toInstance(findClasspathSubcommands())
        bind<AdventureMessage>().toInstance(AdventureMessage.create())
    }

    override fun onLoad() {
        if (isWorldGuardEnabled) {
            mod.bind<WorldGuardChecker>().toInstance(WorldGuardCheckerImpl())
        } else {
            mod.bind<WorldGuardChecker>().toInstance(WorldGuardCheckerDummy())
        }
    }

    override fun onEnable() {
        KTP.setConfiguration(Configuration.forDevelopment())
        _scope = KTP.openScope(this) {
            it.installModules(mod, module {
                bind<Scope>().toInstance(it)
            })
        }
        findAndRegisterClasspathListeners()
        scope.apply {
            getInstance<Commands>()
            getInstance<Metrics>()
            registerLoggerFilters(getInstance<InfernalDeathConsoleMessageFilter>())
            getInstance<EventBus>().post(PluginLoad())
        }
    }

    override fun onDisable() {
        scope.getInstance<EventBus>().post(PluginUnload())
        KTP.closeScope(this)
        _scope = null
    }

    /**
     * Responsible for finding and registering all event listeners in the classpath, respecting configurations and
     * any other imposed condition for their registral.
     */
    private fun findAndRegisterClasspathListeners() {
        val listeners = findClasspathListeners().toMutableSet()

        if (silkSpawnerHandlesSpawnerDrops) {
            logger.info("Enabling SilkSpawners hook")
            listeners.replace(SpawnerBreakListener::class, SpawnerBreakWithSilkSpawnersListener::class)
        }
        if (isTownyHookEnabled) {
            logger.info("Enabling Towny hook")
            listeners += TownyListener::class
        }

        listeners.map { scope.getInstance(it.java) }
            .forEach { Bukkit.getPluginManager().registerEvents(it, this) }

        if (isProtocolLibEnabled) {
            logger.info("Enabling ProtocolLib hook")
            val packetListeners = findClasspathPacketListeners()

            packetListeners.map { scope.getInstance(it.java) }
                .forEach(ProtocolLibrary.getProtocolManager()::addPacketListener)
        }
    }

    private fun registerLoggerFilters(vararg filters: Filter) {
        val rootLogger = LogManager.getRootLogger() as? org.apache.logging.log4j.core.Logger ?: return
        filters.forEach(rootLogger::addFilter)
    }

    private fun findClasspathListeners(): Set<KClass<out Listener>> = findRegistrableClasses("$PLUGIN_PACKAGE.eventlistener")

    private fun findClasspathPacketListeners(): Set<KClass<out PacketAdapter>> = findRegistrableClasses("$PLUGIN_PACKAGE.packetlistener")

    private fun findClasspathSubcommands(): Set<KClass<out SubCommand>> = findRegistrableClasses("$PLUGIN_PACKAGE.command")

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
