package com.github.secretx33.infernalmobsreloaded

import com.github.secretx33.infernalmobsreloaded.commands.Commands
import com.github.secretx33.infernalmobsreloaded.config.AbilityConfig
import com.github.secretx33.infernalmobsreloaded.config.Config
import com.github.secretx33.infernalmobsreloaded.config.ConfigKeys
import com.github.secretx33.infernalmobsreloaded.config.Messages
import com.github.secretx33.infernalmobsreloaded.eventlisteners.ability.FireworkAbilityListener
import com.github.secretx33.infernalmobsreloaded.eventlisteners.ability.LightningAbilityListener
import com.github.secretx33.infernalmobsreloaded.eventlisteners.ability.MountRemovalListener
import com.github.secretx33.infernalmobsreloaded.eventlisteners.charm.CancelCharmEffectsListener
import com.github.secretx33.infernalmobsreloaded.eventlisteners.charm.PlayerDamageCharmListener
import com.github.secretx33.infernalmobsreloaded.eventlisteners.charm.PlayerItemMoveListener
import com.github.secretx33.infernalmobsreloaded.eventlisteners.entity.EntityDamageEntityListener
import com.github.secretx33.infernalmobsreloaded.eventlisteners.entity.EntityDeathListener
import com.github.secretx33.infernalmobsreloaded.eventlisteners.entity.EntitySpawnListener
import com.github.secretx33.infernalmobsreloaded.eventlisteners.infernalmobs.*
import com.github.secretx33.infernalmobsreloaded.eventlisteners.integration.TownyListener
import com.github.secretx33.infernalmobsreloaded.eventlisteners.player.LethalPoisonListener
import com.github.secretx33.infernalmobsreloaded.eventlisteners.player.PlayerMoveListener
import com.github.secretx33.infernalmobsreloaded.eventlisteners.spawner.SpawnerBreakListener
import com.github.secretx33.infernalmobsreloaded.eventlisteners.spawner.SpawnerInteractListener
import com.github.secretx33.infernalmobsreloaded.eventlisteners.spawner.SpawnerPlaceListener
import com.github.secretx33.infernalmobsreloaded.eventlisteners.spawner.SpawnerSpawnListener
import com.github.secretx33.infernalmobsreloaded.eventlisteners.world.EntityLoadListener
import com.github.secretx33.infernalmobsreloaded.eventlisteners.world.EntityUnloadListener
import com.github.secretx33.infernalmobsreloaded.manager.*
import com.github.secretx33.infernalmobsreloaded.model.KeyChain
import com.github.secretx33.infernalmobsreloaded.packetlisteners.InvisibleEntitiesEquipVanisherListener
import com.github.secretx33.infernalmobsreloaded.repositories.CharmsRepo
import com.github.secretx33.infernalmobsreloaded.repositories.GlobalDropsRepo
import com.github.secretx33.infernalmobsreloaded.repositories.InfernalMobTypesRepo
import com.github.secretx33.infernalmobsreloaded.repositories.LootItemsRepo
import com.github.secretx33.infernalmobsreloaded.utils.*
import me.mattstudios.msg.adventure.AdventureMessage
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin
import org.koin.core.logger.Level
import org.koin.dsl.bind
import org.koin.dsl.module

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
        single { LootItemsRepo(get(), get(), get(), get()) }
        single { CharmsRepo(get(), get(), get(), get(), get()) }
        single { CharmsManager(get(), get(), get()) }
        single { AbilityHelper(get(),get(), get(), get(), get(), get(), get(), get(), get(), get()) }
        single { GlobalDropsRepo(get(), get(), get()) }
        single { InfernalMobTypesRepo(get(), get(), get(), get(), get(), get()) }
        single { InfernalMobsManager(get(), get(), get(), get(), get(), get()) }
        single { FireworkAbilityListener(get(), get(), get(), get()) }
        single { CancelCharmEffectsListener(get(), get()) }
        single { PlayerDamageCharmListener(get(), get()) }
        single { PlayerItemMoveListener(get(), get()) }
        single { EntityDamageEntityListener(get(), get(), get()) }
        single { EntityDeathListener(get(), get()) }
        single { EntitySpawnListener(get(), get(), get(), get()) }
        single { InfernalDamageDoneListener(get(), get()) }
        single { InfernalDamageTakenListener(get(), get()) }
        single { InfernalDeathListener(get(), get(), get(), get(), get(), get(), get()) }
        single { InfernalSpawnListener(get(), get(), get(), get()) }
        single { InfernalTargetListener(get(), get()) }
        single { BossBarListener(get(), get(), get()) }
        single { LethalPoisonListener(get(), get()) }
        single { PlayerMoveListener(get(), get(), get()) }
        single { SpawnerBreakListener(get(), get(), get(), get()) }
        single { SpawnerInteractListener(get(), get(), get(), get()) }
        single { SpawnerPlaceListener(get(), get(), get()) }
        single { SpawnerSpawnListener(get(), get(), get()) }
        single { LightningAbilityListener(get(), get(), get()) }
        single { MountRemovalListener(get(), get()) }
        single { EntityUnloadListener(get(), get(), get()) }
        single { EntityLoadListener(get(), get()) }
        single { InvisibleEntitiesEquipVanisherListener(get(), get(), get()) }
        single { TownyListener(get(), get(), get(), get()) }
        single { Commands(get()) }
        single { Metrics(get(), 11253) }
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
        get<FireworkAbilityListener>()
        get<LightningAbilityListener>()
        get<MountRemovalListener>()
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
        get<SpawnerBreakListener>()
        get<SpawnerInteractListener>()
        get<SpawnerPlaceListener>()
        get<SpawnerSpawnListener>()
        get<EntityUnloadListener>()
        get<EntityLoadListener>()
        get<Commands>()
        if(isProtocolLibEnabled)
            get<InvisibleEntitiesEquipVanisherListener>()
        if(isTownyHookEnabled) {
            logger.info("Enabling Towny hook.")
            get<TownyListener>()
        }
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
}
