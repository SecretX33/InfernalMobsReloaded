package com.github.secretx33.infernalmobsreloaded.manager

import com.github.secretx33.infernalmobsreloaded.config.Config
import com.github.secretx33.infernalmobsreloaded.config.ConfigKeys
import com.github.secretx33.infernalmobsreloaded.utils.getHealthPercent
import net.kyori.adventure.bossbar.BossBar
import org.bukkit.Bukkit
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.koin.core.component.KoinApiExtension
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@KoinApiExtension
class BossBarManager (
    private val config: Config,
    private val mobsManager: InfernalMobsManager,
) {

    private val bossBarMap = HashMap<UUID, BossBar>()

    fun updateBossBar(entity: LivingEntity, newHealth: Float) {
        if(!bossBarEnabled) return
        entity.getBossBar()?.progress(newHealth.toFloat())
    }

    private fun LivingEntity.getBossBar(): BossBar? {
        val infernalType = mobsManager.getInfernalTypeOrNull(this) ?: return null
        return bossBarMap.getOrPut(uniqueId) {
            BossBar.bossBar(infernalType.bossBarName, getHealthPercent(), infernalType.bossBarColor, infernalType.bossBarOverlay, infernalType.bossBarFlags)
        }
    }

    fun removeBossBar(entity: LivingEntity) {
        bossBarMap.remove(entity.uniqueId)?.let { bar ->
            entity.world.players.forEach { it.hideBossBar(bar) }
        }
    }

    private fun manageInfernalBossBarsVisibility(player: Player, nearbyInfernals: Collection<LivingEntity>) {
        if(!bossBarEnabled) return
        val infernalUuids = nearbyInfernals.map { it.uniqueId }
        // filter the infernals further away and hide their bar from the player
        bossBarMap.filterKeys { !infernalUuids.contains(it) }.forEach { (_, bar) -> player.hideBossBar(bar) }
        // show only the bars from nearby infernals
        nearbyInfernals.mapNotNull { it.getBossBar() }.forEach { player.showBossBar(it) }
    }

    fun showBarOfNearbyInfernals(player: Player) {
        if(!bossBarEnabled) return
        val nearbyInfernals = player.getNearbyInfernals().takeIf { it.isNotEmpty() } ?: return
        manageInfernalBossBarsVisibility(player, nearbyInfernals)
    }

    private fun LivingEntity.getNearbyInfernals() = location.getNearbyLivingEntities(bossBarShowRange) { !it.isDead && it.isValid && mobsManager.isValidInfernalMob(it) }

    fun showBossBarForNearbyPlayers(entity: LivingEntity) {
        if(!bossBarEnabled) return
        val bossBar = entity.getBossBar() ?: return
        entity.location.getNearbyPlayers(bossBarShowRange) { !it.isDead && it.isValid }.forEach {
            it.showBossBar(bossBar)
        }
    }

    fun showBarsOfNearbyInfernalsForAllPlayers() {
        if(!bossBarEnabled) return
        Bukkit.getOnlinePlayers().forEach { showBarOfNearbyInfernals(it) }
    }

    fun hideAllBarsFromAllPlayers() {
        Bukkit.getOnlinePlayers().forEach { hideAllBossBarsFor(it) }
        bossBarMap.clear()
    }

    fun hideAllBossBarsFor(player: Player) {
        bossBarMap.values.forEach { player.hideBossBar(it) }
    }

    private val bossBarEnabled get() = config.get<Boolean>(ConfigKeys.ENABLE_BOSS_BARS)

    private val bossBarShowRange get() = config.getDouble(ConfigKeys.BOSS_BAR_SHOW_RANGE, maxValue = 256.0)
}
