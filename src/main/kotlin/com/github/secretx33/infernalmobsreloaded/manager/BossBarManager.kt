package com.github.secretx33.infernalmobsreloaded.manager

import com.github.secretx33.infernalmobsreloaded.config.Config
import com.github.secretx33.infernalmobsreloaded.config.ConfigKeys
import com.github.secretx33.infernalmobsreloaded.utils.getCurrentHpPercent
import net.kyori.adventure.bossbar.BossBar
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.koin.core.component.KoinApiExtension
import java.util.*

@KoinApiExtension
class BossBarManager (
    private val config: Config,
    private val mobsManager: InfernalMobsManager,
) {

    private val bossBarMap = HashMap<UUID, BossBar>()

    fun updateBossBar(entity: LivingEntity) {
        if(!bossBarEnabled) return
        val bossBar = entity.getBossBar() ?: return
        bossBar.progress(entity.getCurrentHpPercent())
    }

    private fun LivingEntity.getBossBar(): BossBar? {
        val infernalType = mobsManager.getInfernalTypeOrNull(this) ?: return null
        return bossBarMap.getOrPut(uniqueId) {
            BossBar.bossBar(infernalType.bossBarName, getCurrentHpPercent(), infernalType.bossBarColor, infernalType.bossBarOverlay, infernalType.bossBarFlags)
        }
    }

    fun manageInfernalBossBarsVisibility(player: Player, nearbyInfernals: Collection<LivingEntity>) {
        val infernalUuids = nearbyInfernals.map { it.uniqueId }
        // filter the infernals further away and hide their bar from the player
        bossBarMap.filterKeys { !infernalUuids.contains(it) }.forEach { (_, bar) -> player.hideBossBar(bar) }
        // show only the bars from nearby infernals
        nearbyInfernals.mapNotNull { it.getBossBar() }.forEach { player.showBossBar(it) }
    }

    fun removeBossBar(entity: LivingEntity) {
        bossBarMap.remove(entity.uniqueId)?.let { bar ->
            entity.world.players.forEach { it.hideBossBar(bar) }
        }
    }

    private val bossBarEnabled get() = config.get<Boolean>(ConfigKeys.ENABLE_BOSS_BARS)
    private val bossBarShowRange get() = config.getDouble(ConfigKeys.BOSS_BAR_SHOW_RANGE, maxValue = 256.0)
}
