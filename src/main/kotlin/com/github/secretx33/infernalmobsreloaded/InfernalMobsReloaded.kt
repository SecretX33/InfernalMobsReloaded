package com.github.secretx33.infernalmobsreloaded

import com.github.secretx33.infernalmobsreloaded.config.AbilityConfig
import com.github.secretx33.infernalmobsreloaded.config.Config
import com.github.secretx33.infernalmobsreloaded.config.Messages
import com.github.secretx33.infernalmobsreloaded.eventlisteners.ChunkLoadListener
import com.github.secretx33.infernalmobsreloaded.eventlisteners.ChunkUnloadListener
import com.github.secretx33.infernalmobsreloaded.eventlisteners.NaturalEntitySpawnListener
import com.github.secretx33.infernalmobsreloaded.eventlisteners.infernalmobs.InfernalDeathListener
import com.github.secretx33.infernalmobsreloaded.eventlisteners.infernalmobs.InfernalSpawnListener
import com.github.secretx33.infernalmobsreloaded.manager.AbilityHelper
import com.github.secretx33.infernalmobsreloaded.manager.InfernalMobsManager
import com.github.secretx33.infernalmobsreloaded.manager.ParticlesHelper
import com.github.secretx33.infernalmobsreloaded.model.KeyChain
import com.github.secretx33.infernalmobsreloaded.repositories.InfernalMobTypesRepo
import com.github.secretx33.infernalmobsreloaded.repositories.LootItemsRepo
import com.github.secretx33.infernalmobsreloaded.utils.*
import me.mattstudios.msg.adventure.AdventureMessage
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin
import org.koin.core.component.KoinApiExtension
import org.koin.core.logger.Level
import org.koin.dsl.bind
import org.koin.dsl.module

@KoinApiExtension
class InfernalMobsReloaded : JavaPlugin(), CustomKoinComponent {

    private val mod = module {
        single<Plugin> { this@InfernalMobsReloaded } bind JavaPlugin::class
        single { get<Plugin>().logger }
        single { AdventureMessage.create() }
        single { Config(get(), get()) }
        single { Messages(get(), get()) }
        single { AbilityConfig(get(), get(), get()) }
        single { KeyChain(get()) }
        single { ParticlesHelper(get(), get()) }
        single { LootItemsRepo(get(), get(), get()) }
        single { InfernalMobTypesRepo(get(), get(), get(), get()) }
        single { InfernalMobsManager(get(), get(), get(), get(), get()) }
        single { InfernalDeathListener(get(), get(), get(), get()) }
        single { InfernalSpawnListener(get(), get(), get(), get()) }
        single { ChunkLoadListener(get(), get()) }
        single { ChunkUnloadListener(get(), get()) }
        single { NaturalEntitySpawnListener(get(), get(), get()) }
    }

//    override fun onLoad() {
//        // if worldguard is enabled, replace dummy module with real one
//        if(isWorldGuardEnabled) {
//            // creation of the WorldGuardChecker happens here because WG is bae and requires hooking to happen on method onLoad
//            mod.single<WorldGuardChecker>(override = true) { WorldGuardCheckerImpl() }
//        }
//    }

    override fun onEnable() {
        startKoin {
            printLogger(Level.ERROR)
            loadKoinModules(mod)
        }
        get<InfernalDeathListener>()
        get<InfernalSpawnListener>()
        get<ChunkLoadListener>()
        get<ChunkUnloadListener>()
        get<NaturalEntitySpawnListener>()
    }

    override fun onDisable() {
        get<AbilityHelper>().revertPendingBlockModifications()
        unloadKoinModules(mod)
        stopKoin()
    }

//    private val isWorldGuardEnabled
//        get() = Bukkit.getPluginManager().getPlugin("WorldGuard") != null
}
