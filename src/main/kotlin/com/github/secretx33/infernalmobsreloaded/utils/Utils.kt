package com.github.secretx33.infernalmobsreloaded.utils

import com.github.secretx33.infernalmobsreloaded.utils.Utils.audience
import net.kyori.adventure.platform.bukkit.BukkitAudiences
import net.kyori.adventure.text.ComponentLike
import org.bukkit.command.CommandSender
import org.bukkit.persistence.PersistentDataHolder
import org.koin.core.component.KoinApiExtension

@KoinApiExtension
private object Utils : CustomKoinComponent {
    val audience: BukkitAudiences
        get() = get()
}

val PersistentDataHolder.pdc
    get() = persistentDataContainer

@KoinApiExtension
fun CommandSender.sendMessage(component: ComponentLike) = audience.sender(this).sendMessage(component)

@KoinApiExtension
fun CommandSender.sendActionBar(component: ComponentLike) = audience.sender(this).sendActionBar(component)
