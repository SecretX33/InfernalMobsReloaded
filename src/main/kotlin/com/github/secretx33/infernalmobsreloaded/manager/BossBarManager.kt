package com.github.secretx33.infernalmobsreloaded.manager

import com.github.secretx33.infernalmobsreloaded.config.Config
import com.github.secretx33.infernalmobsreloaded.config.ConfigKeys
import com.github.secretx33.infernalmobsreloaded.utils.extension.getHealthPercent
import net.kyori.adventure.bossbar.BossBar
import org.bukkit.Bukkit
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import toothpick.InjectConstructor
import java.util.UUID
import javax.inject.Singleton

@Singleton
@InjectConstructor
class BossBarManager (
    private val config: Config,
    private val mobsManager: InfernalMobsManager,
) {

    private val bossBarMap = HashMap<UUID, BossBar>()

    fun updateBossBar(entity: LivingEntity, newHealth: Float) {
        if(!bossBarEnabled) return
        entity.getBossBar()?.progress(newHealth)
    }

    private fun LivingEntity.getBossBar(): BossBar? {
        val infernalType = mobsManager.getInfernalTypeOrNull(this) ?: return null
        return bossBarMap.getOrPut(uniqueId) {
            BossBar.bossBar(infernalType.bossBarName, getHealthPercent(), infernalType.bossBarColor, infernalType.bossBarOverlay, infernalType.bossBarFlags)
        }
    }

    fun removeBossBar(entity: LivingEntity) {
        if(!bossBarEnabled) return
        bossBarMap.remove(entity.uniqueId)?.let { bar ->
            entity.world.players.forEach { it.hideBossBar(bar) }
        }
    }

    private fun manageInfernalBossBarsVisibility(player: Player, nearbyInfernals: Collection<LivingEntity>) {
        if(!bossBarEnabled) return
        val infernalUuids = nearbyInfernals.map { it.uniqueId }
        // filter the infernals further away and hide their bar from the player
        bossBarMap.filterKeys { it !in infernalUuids }.forEach { (_, bar) -> player.hideBossBar(bar) }
        // show only the bars from nearby infernals
        nearbyInfernals.mapNotNull { it.getBossBar() }.forEach { player.showBossBar(it) }
    }

    fun showBarOfNearbyInfernals(player: Player) {
        if(!bossBarEnabled) return
        val nearbyInfernals = player.getNearbyInfernals()
        manageInfernalBossBarsVisibility(player, nearbyInfernals)
    }

    private fun Player.getNearbyInfernals() = location.getNearbyLivingEntities(bossBarShowDistance, bossBarShowHeight, bossBarShowDistance) { !it.isDead && it.isValid && mobsManager.isValidInfernalMob(it) && (!bossBarRequireLineOfSight || hasLineOfSight(it)) }

    fun showBossBarForNearbyPlayers(entity: LivingEntity) {
        if(!bossBarEnabled) return
        val bossBar = entity.getBossBar() ?: return
        entity.location.getNearbyPlayers(bossBarShowDistance, bossBarShowHeight, bossBarShowDistance) { !it.isDead && it.isValid && (!bossBarRequireLineOfSight || it.hasLineOfSight(entity)) }.forEach {
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

    private val bossBarShowDistance get() = config.getDouble(ConfigKeys.BOSS_BAR_SHOW_RANGE_DISTANCE, maxValue = 256.0)

    private val bossBarShowHeight get() = config.getDouble(ConfigKeys.BOSS_BAR_SHOW_RANGE_HEIGHT, maxValue = 256.0)

    private val bossBarRequireLineOfSight
        get() = config.get<Boolean>(ConfigKeys.INFERNAL_BOSS_BAR_REQUIRE_LINE_OF_SIGHT)
}
