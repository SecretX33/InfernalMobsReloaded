package com.github.secretx33.infernalmobsreloaded.model

import org.bukkit.Bukkit
import java.util.*

data class InfernalMob (
    val uniqueId: UUID,
    val abilities: Set<String>,
    private var lives: Int = 1,
) {
    val isDead: Boolean
        get() = Bukkit.getEntity(uniqueId)?.isDead ?: true
}
