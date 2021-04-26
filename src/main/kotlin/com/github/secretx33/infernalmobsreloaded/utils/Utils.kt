package com.github.secretx33.infernalmobsreloaded.utils

import com.github.secretx33.infernalmobsreloaded.utils.Utils.audience
import net.kyori.adventure.platform.bukkit.BukkitAudiences
import net.kyori.adventure.text.ComponentLike
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.command.CommandSender
import org.bukkit.craftbukkit.libs.org.apache.commons.lang3.text.WordUtils
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataHolder
import org.bukkit.plugin.Plugin
import org.koin.core.component.KoinApiExtension
import java.util.*

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

fun String.capitalizeFully(): String = WordUtils.capitalizeFully(this)

fun String.toUuid(): UUID = UUID.fromString(this)

fun runSync(plugin: Plugin, delay: Long = 0L, block: () -> Unit) {
    if(delay < 0) return
    if(delay == 0L) Bukkit.getScheduler().runTask(plugin, Runnable { block() })
    else Bukkit.getScheduler().runTaskLater(plugin, Runnable { block() }, delay / 50L)
}

fun Block.isAir() = type.isAir

fun ItemStack.isAir() = type.isAir

fun Location.formattedString(): String = "Location(world=${world?.name ?: "Unknown"}, x=${x.toLong()}, y=${y.toLong()}, z=${z.toLong()})"

fun Material.formattedTypeName(): String = name.replace('_', ' ').capitalizeFully()

fun Block.formattedTypeName(): String = type.formattedTypeName()

fun ItemStack.formattedTypeName(): String = type.formattedTypeName()

fun ItemStack.formattedItemName(): String = itemMeta?.displayName?.takeIf { it.isNotBlank() } ?: formattedTypeName()

fun Player.isInventoryFull() = inventory.firstEmpty() == -1

