package com.github.secretx33.infernalmobsreloaded

import com.github.secretx33.infernalmobsreloaded.eventlisteners.InfernalMobSpawnListener
import com.github.secretx33.infernalmobsreloaded.eventlisteners.NaturalEntitySpawnListener
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
        single { LootItemsRepo(get(), get(), get()) }
        single { InfernalMobTypesRepo(get(), get(), get(), get()) }
        single { NaturalEntitySpawnListener(get(), get(), get()) }
        single { InfernalMobSpawnListener(get()) }
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
        get<NaturalEntitySpawnListener>()
        get<InfernalMobSpawnListener>()
    }

    override fun onDisable() {
        unloadKoinModules(mod)
        stopKoin()
    }

//    private val isWorldGuardEnabled
//        get() = Bukkit.getPluginManager().getPlugin("WorldGuard") != null
}
