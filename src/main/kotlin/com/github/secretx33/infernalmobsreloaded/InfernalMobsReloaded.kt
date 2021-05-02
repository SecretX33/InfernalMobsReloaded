package com.github.secretx33.infernalmobsreloaded

import com.github.secretx33.infernalmobsreloaded.commands.Commands
import com.github.secretx33.infernalmobsreloaded.config.AbilityConfig
import com.github.secretx33.infernalmobsreloaded.config.Config
import com.github.secretx33.infernalmobsreloaded.config.Messages
import com.github.secretx33.infernalmobsreloaded.eventlisteners.ChunkLoadListener
import com.github.secretx33.infernalmobsreloaded.eventlisteners.ChunkUnloadListener
import com.github.secretx33.infernalmobsreloaded.eventlisteners.ability.FireworkDamageIncreaseListener
import com.github.secretx33.infernalmobsreloaded.eventlisteners.entity.EntityDamageEntityListener
import com.github.secretx33.infernalmobsreloaded.eventlisteners.entity.EntityDeathListener
import com.github.secretx33.infernalmobsreloaded.eventlisteners.entity.EntitySpawnListener
import com.github.secretx33.infernalmobsreloaded.eventlisteners.infernalmobs.*
import com.github.secretx33.infernalmobsreloaded.eventlisteners.player.PlayerJoinListener
import com.github.secretx33.infernalmobsreloaded.eventlisteners.player.PlayerLeaveListener
import com.github.secretx33.infernalmobsreloaded.eventlisteners.player.PlayerMoveListener
import com.github.secretx33.infernalmobsreloaded.eventlisteners.sideeffectsmitigation.FireworkDamageWorkaroundListener
import com.github.secretx33.infernalmobsreloaded.eventlisteners.sideeffectsmitigation.LightningDamageWorkaroundListener
import com.github.secretx33.infernalmobsreloaded.eventlisteners.sideeffectsmitigation.MountRemovalListener
import com.github.secretx33.infernalmobsreloaded.manager.*
import com.github.secretx33.infernalmobsreloaded.model.KeyChain
import com.github.secretx33.infernalmobsreloaded.packetlisteners.InvisibleEntitiesEquipVanisherListener
import com.github.secretx33.infernalmobsreloaded.repositories.InfernalMobTypesRepo
import com.github.secretx33.infernalmobsreloaded.repositories.LootItemsRepo
import com.github.secretx33.infernalmobsreloaded.utils.*
import me.mattstudios.msg.adventure.AdventureMessage
import org.bukkit.Bukkit
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
        single { AbilityConfig(get(), get()) }
        single { KeyChain(get()) }
        single { BossBarManager(get(), get()) }
        single { ParticlesHelper(get(), get()) }
        single { LootItemsRepo(get(), get(), get()) }
        single { AbilityHelper(get(),get(), get(), get(), get(), get(), get(), get(), get()) }
        single { InfernalMobTypesRepo(get(), get(), get(), get(), get()) }
        single { InfernalMobsManager(get(), get(), get(), get(), get(), get()) }
        single { FireworkDamageIncreaseListener(get(), get(), get()) }
        single { EntityDamageEntityListener(get(), get(), get()) }
        single { EntityDeathListener(get(), get()) }
        single { EntitySpawnListener(get(), get(), get(), get()) }
        single { InfernalDamageDoneListener(get(), get()) }
        single { InfernalDamageTakenListener(get(), get(), get()) }
        single { InfernalDeathListener(get(), get(), get(), get(), get(), get()) }
        single { InfernalSpawnListener(get(), get(), get(), get()) }
        single { InfernalTargetListener(get(), get()) }
        single { PlayerJoinListener(get(), get()) }
        single { PlayerLeaveListener(get(), get()) }
        single { PlayerMoveListener(get(), get(), get(), get()) }
        single { FireworkDamageWorkaroundListener(get(), get()) }
        single { LightningDamageWorkaroundListener(get(), get()) }
        single { MountRemovalListener(get(), get()) }
        single { ChunkUnloadListener(get(), get()) }
        single { ChunkLoadListener(get(), get()) }
        single { InvisibleEntitiesEquipVanisherListener(get(), get(), get()) }
        single { Commands(get()) }
        single<WorldGuardChecker> { WorldGuardCheckerDummy() }
    }

    override fun onLoad() {
        // if worldguard is enabled, replace dummy module with real one
        if(isWorldGuardEnabled) {
            // creation of the WorldGuardChecker happens here because WG is bae and requires hooking to happen on method onLoad
            mod.single<WorldGuardChecker>(override = true) { WorldGuardCheckerImpl() }
        }
    }

    override fun onEnable() {
        startKoin {
            printLogger(Level.ERROR)
            loadKoinModules(mod)
        }
        get<FireworkDamageIncreaseListener>()
        get<EntityDamageEntityListener>()
        get<EntityDeathListener>()
        get<EntitySpawnListener>()
        get<InfernalDamageDoneListener>()
        get<InfernalDamageTakenListener>()
        get<InfernalDeathListener>()
        get<InfernalSpawnListener>()
        get<InfernalTargetListener>()
        get<PlayerJoinListener>()
        get<PlayerLeaveListener>()
        get<PlayerMoveListener>()
        get<FireworkDamageWorkaroundListener>()
        get<LightningDamageWorkaroundListener>()
        get<MountRemovalListener>()
        get<ChunkUnloadListener>()
        get<ChunkLoadListener>()
        get<Commands>()
        get<InfernalMobsManager>().loadAllInfernals()
        get<BossBarManager>().showBarsOfNearbyInfernalsForAllPlayers()
        if(isProtocolLibEnabled)
            get<InvisibleEntitiesEquipVanisherListener>()
    }

    override fun onDisable() {
        get<AbilityHelper>().revertPendingBlockModifications()
        get<InfernalMobsManager>().unloadAllInfernals()
        get<BossBarManager>().hideAllBarsFromAllPlayers()
        unloadKoinModules(mod)
        stopKoin()
    }

    private val isWorldGuardEnabled
        get() = Bukkit.getPluginManager().getPlugin("WorldGuard") != null

    private val isProtocolLibEnabled
        get() = Bukkit.getPluginManager().isPluginEnabled("ProtocolLib")
}
