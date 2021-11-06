package com.github.secretx33.infernalmobsreloaded

import com.github.secretx33.infernalmobsreloaded.commands.Commands
import com.github.secretx33.infernalmobsreloaded.config.AbilityConfig
import com.github.secretx33.infernalmobsreloaded.config.Config
import com.github.secretx33.infernalmobsreloaded.config.ConfigKeys
import com.github.secretx33.infernalmobsreloaded.config.Messages
import com.github.secretx33.infernalmobsreloaded.eventlisteners.ability.FireworkAbilityListener
import com.github.secretx33.infernalmobsreloaded.eventlisteners.ability.LightningAbilityListener
import com.github.secretx33.infernalmobsreloaded.eventlisteners.ability.MountRemovalListener
import com.github.secretx33.infernalmobsreloaded.eventlisteners.ability.ThiefAbilityListener
import com.github.secretx33.infernalmobsreloaded.eventlisteners.charm.CancelCharmEffectsListener
import com.github.secretx33.infernalmobsreloaded.eventlisteners.charm.PlayerDamageCharmListener
import com.github.secretx33.infernalmobsreloaded.eventlisteners.charm.PlayerItemMoveListener
import com.github.secretx33.infernalmobsreloaded.eventlisteners.entity.EntityDamageEntityListener
import com.github.secretx33.infernalmobsreloaded.eventlisteners.entity.EntityDeathListener
import com.github.secretx33.infernalmobsreloaded.eventlisteners.entity.EntitySpawnListener
import com.github.secretx33.infernalmobsreloaded.eventlisteners.hook.SpawnerBreakWithSilkSpawnersListener
import com.github.secretx33.infernalmobsreloaded.eventlisteners.hook.TownyListener
import com.github.secretx33.infernalmobsreloaded.eventlisteners.infernalmobs.BossBarListener
import com.github.secretx33.infernalmobsreloaded.eventlisteners.infernalmobs.InfernalDamageDoneListener
import com.github.secretx33.infernalmobsreloaded.eventlisteners.infernalmobs.InfernalDamageTakenListener
import com.github.secretx33.infernalmobsreloaded.eventlisteners.infernalmobs.InfernalDeathListener
import com.github.secretx33.infernalmobsreloaded.eventlisteners.infernalmobs.InfernalSpawnListener
import com.github.secretx33.infernalmobsreloaded.eventlisteners.infernalmobs.InfernalTargetListener
import com.github.secretx33.infernalmobsreloaded.eventlisteners.player.LethalPoisonListener
import com.github.secretx33.infernalmobsreloaded.eventlisteners.player.PlayerMoveListener
import com.github.secretx33.infernalmobsreloaded.eventlisteners.spawner.SpawnerBreakListener
import com.github.secretx33.infernalmobsreloaded.eventlisteners.spawner.SpawnerInteractListener
import com.github.secretx33.infernalmobsreloaded.eventlisteners.spawner.SpawnerPlaceListener
import com.github.secretx33.infernalmobsreloaded.eventlisteners.spawner.SpawnerSpawnListener
import com.github.secretx33.infernalmobsreloaded.eventlisteners.world.EntityLoadListener
import com.github.secretx33.infernalmobsreloaded.eventlisteners.world.EntityUnloadListener
import com.github.secretx33.infernalmobsreloaded.manager.AbilityHelper
import com.github.secretx33.infernalmobsreloaded.manager.BossBarManager
import com.github.secretx33.infernalmobsreloaded.manager.CharmsManager
import com.github.secretx33.infernalmobsreloaded.manager.InfernalMobsManager
import com.github.secretx33.infernalmobsreloaded.manager.ParticlesHelper
import com.github.secretx33.infernalmobsreloaded.manager.hook.WorldGuardChecker
import com.github.secretx33.infernalmobsreloaded.manager.hook.WorldGuardCheckerDummy
import com.github.secretx33.infernalmobsreloaded.manager.hook.WorldGuardCheckerImpl
import com.github.secretx33.infernalmobsreloaded.model.KeyChain
import com.github.secretx33.infernalmobsreloaded.packetlisteners.InvisibleEntitiesEquipVanisherListener
import com.github.secretx33.infernalmobsreloaded.repositories.CharmsRepo
import com.github.secretx33.infernalmobsreloaded.repositories.GlobalDropsRepo
import com.github.secretx33.infernalmobsreloaded.repositories.InfernalMobTypesRepo
import com.github.secretx33.infernalmobsreloaded.repositories.LootItemsRepo
import com.github.secretx33.infernalmobsreloaded.utils.other.CustomKoinComponent
import com.github.secretx33.infernalmobsreloaded.utils.other.Metrics
import com.github.secretx33.infernalmobsreloaded.utils.other.get
import com.github.secretx33.infernalmobsreloaded.utils.other.getOrNull
import com.github.secretx33.infernalmobsreloaded.utils.other.loadKoinModules
import com.github.secretx33.infernalmobsreloaded.utils.other.startKoin
import com.github.secretx33.infernalmobsreloaded.utils.other.stopKoin
import com.github.secretx33.infernalmobsreloaded.utils.other.unloadKoinModules
import me.mattstudios.msg.adventure.AdventureMessage
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.PluginDescriptionFile
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.plugin.java.JavaPluginLoader
import org.koin.core.logger.Level
import org.koin.dsl.bind
import org.koin.dsl.module
import java.io.File

open class InfernalMobsReloaded: JavaPlugin, CustomKoinComponent {

    constructor() : super()

    constructor(
        loader: JavaPluginLoader,
        description: PluginDescriptionFile,
        dataFolder: File,
        file: File
    ) : super(loader, description, dataFolder, file)

    private val mod = module {
        single<Plugin> { this@InfernalMobsReloaded } bind JavaPlugin::class
        single { get<Plugin>().logger }
        single { AdventureMessage.create() }
        single { Config(get(), get()) }
        single { Messages(get(), get()) }
        single { AbilityConfig(get(), get()) }
        single { KeyChain(get()) }
        single { BossBarManager(get(), get()) }
        single { ParticlesHelper(get()) }
        single { LootItemsRepo(get(), get(), get(), get()) }
        single { CharmsRepo(get(), get(), get(), get(), get()) }
        single { CharmsManager(get(), get(), get()) }
        single { AbilityHelper(get(),get(), get(), get(), get(), get(), get(), get(), get(), get()) }
        single { GlobalDropsRepo(get(), get(), get()) }
        single { InfernalMobTypesRepo(get(), get(), get(), get(), get(), get()) }
        single { InfernalMobsManager(get(), get(), get(), get(), get(), get(), get()) }
        single { FireworkAbilityListener(get(), get(), get(), get()) }
        single { LightningAbilityListener(get(), get(), get()) }
        single { MountRemovalListener(get(), get()) }
        single { ThiefAbilityListener(get(), get(), get()) }
        single { CancelCharmEffectsListener(get(), get()) }
        single { PlayerDamageCharmListener(get(), get()) }
        single { PlayerItemMoveListener(get(), get()) }
        single { EntityDamageEntityListener(get(), get(), get()) }
        single { EntityDeathListener(get(), get()) }
        single { EntitySpawnListener(get(), get(), get(), get()) }
        single { BossBarListener(get(), get(), get()) }
        single { InfernalDamageDoneListener(get(), get()) }
        single { InfernalDamageTakenListener(get(), get()) }
        single { InfernalDeathListener(get(), get(), get(), get(), get(), get()) }
        single { InfernalSpawnListener(get(), get(), get(), get()) }
        single { InfernalTargetListener(get(), get()) }
        single { LethalPoisonListener(get(), get()) }
        single { PlayerMoveListener(get(), get(), get()) }
        single { SpawnerBreakListener(get(), get(), get(), get()) }
        single { SpawnerInteractListener(get(), get(), get()) }
        single { SpawnerPlaceListener(get(), get(), get()) }
        single { SpawnerSpawnListener(get(), get(), get()) }
        single { EntityLoadListener(get(), get(), get(), get()) }
        single { EntityUnloadListener(get(), get(), get()) }
        single { InvisibleEntitiesEquipVanisherListener(get(), get(), get()) }
        single { SpawnerBreakWithSilkSpawnersListener(get(), get(), get(), get()) }
        single { TownyListener(get(), get(), get(), get()) }
        single { Commands(get()) }
        single { Metrics(get(), 11253) }
    }

    override fun onLoad() {
        // if worldguard is enabled, replace dummy module with real one
        if(isWorldGuardEnabled) {
            // creation of the WorldGuardChecker happens here because WG is bae and requires hooking to happen on method onLoad
            mod.single<WorldGuardChecker> { WorldGuardCheckerImpl() }
        } else {
            mod.single<WorldGuardChecker> { WorldGuardCheckerDummy() }
        }
    }

    override fun onEnable() {
        startKoin {
            printLogger(Level.ERROR)
            loadKoinModules(mod)
        }
        get<FireworkAbilityListener>()
        get<LightningAbilityListener>()
        get<MountRemovalListener>()
        get<ThiefAbilityListener>()
        get<CancelCharmEffectsListener>()
        get<PlayerDamageCharmListener>()
        get<PlayerItemMoveListener>()
        get<EntityDamageEntityListener>()
        get<EntityDeathListener>()
        get<EntitySpawnListener>()
        get<InfernalDamageDoneListener>()
        get<InfernalDamageTakenListener>()
        get<InfernalDeathListener>()
        get<InfernalSpawnListener>()
        get<InfernalTargetListener>()
        get<BossBarListener>()
        get<LethalPoisonListener>()
        get<PlayerMoveListener>()
        if(silkSpawnerHandlesSpawnerDrops) {
            logger.info("Enabling SilkSpawners hook.")
            get<SpawnerBreakWithSilkSpawnersListener>()
        } else {
            get<SpawnerBreakListener>()
        }
        get<SpawnerInteractListener>()
        get<SpawnerPlaceListener>()
        get<SpawnerSpawnListener>()
        get<EntityUnloadListener>()
        get<EntityLoadListener>()
        if(isProtocolLibEnabled)
            logger.info("Enabling ProtocolLib hook.")
            get<InvisibleEntitiesEquipVanisherListener>()
        if(isTownyHookEnabled) {
            logger.info("Enabling Towny hook.")
            get<TownyListener>()
        }
        get<Commands>()
        get<Metrics>()
        get<InfernalMobsManager>().loadAllInfernals()
        get<BossBarManager>().showBarsOfNearbyInfernalsForAllPlayers()
        get<CharmsManager>().startAllCharmTasks()
    }

    override fun onDisable() {
        get<InfernalMobsManager>().unloadAllInfernals()
        get<BossBarManager>().hideAllBarsFromAllPlayers()
        get<CharmsManager>().stopAllCharmTasks()
        get<AbilityHelper>().revertPendingBlockModifications()
        getOrNull<TownyListener>()?.cancelRemovalTasks()
        unloadKoinModules(mod)
        stopKoin()
    }

    private val isWorldGuardEnabled
        get() = Bukkit.getPluginManager().getPlugin("WorldGuard") != null

    private val isProtocolLibEnabled
        get() = Bukkit.getPluginManager().isPluginEnabled("ProtocolLib")

    private val isTownyHookEnabled
        get() = Bukkit.getPluginManager().isPluginEnabled("Towny") && get<Config>().get(ConfigKeys.TOWNY_REMOVE_INFERNAL_IN_TOWNS)

    private val silkSpawnerHandlesSpawnerDrops
        get() = Bukkit.getPluginManager().isPluginEnabled("SilkSpawners") && get<Config>().get(ConfigKeys.SILKSPAWNERS_HANDLES_SPAWNER_DROP)
}
