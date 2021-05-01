package com.github.secretx33.infernalmobsreloaded.utils

import kotlinx.coroutines.delay
import org.apache.commons.lang.WordUtils
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.block.Block
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataHolder
import org.bukkit.plugin.Plugin
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.Future

fun Player.getTarget(range: Int): LivingEntity? = (world.rayTraceEntities(eyeLocation, eyeLocation.direction, range.toDouble()) { it is LivingEntity && type != EntityType.ENDER_DRAGON && it.uniqueId != uniqueId }?.hitEntity as? LivingEntity)?.takeIf { hasLineOfSight(it) }

fun String.capitalizeFully(): String = WordUtils.capitalizeFully(this)

fun String.toUuid(): UUID = UUID.fromString(this)

fun runSync(plugin: Plugin, delay: Long = 0L, block: () -> Unit) {
    if(delay < 0) return
    if(delay == 0L) Bukkit.getScheduler().runTask(plugin, Runnable { block() })
    else Bukkit.getScheduler().runTaskLater(plugin, Runnable { block() }, delay / 50L)
}

suspend fun <T> futureSync(plugin: Plugin, callable: Callable<T>): T = Bukkit.getScheduler().callSyncMethod(plugin, callable).await()

suspend fun <T> Future<T>.await(): T {
    while(!isDone)
        delay(25) // or whatever you want your polling frequency to be
    @Suppress("BlockingMethodInNonBlockingContext")
    return get()
}

fun Block.isAir() = type.isAir

fun ItemStack.isAir() = type.isAir

fun Location.formattedString(): String = "Location(world=${world?.name ?: "Unknown"}, x=${x.toLong()}, y=${y.toLong()}, z=${z.toLong()})"

fun Material.formattedTypeName(): String = name.replace('_', ' ').capitalizeFully()

fun Block.formattedTypeName(): String = type.formattedTypeName()

fun ItemStack.formattedTypeName(): String = type.formattedTypeName()

fun ItemStack.formattedItemName(): String = itemMeta?.displayName()?.toString()?.takeIf { it.isNotBlank() } ?: formattedTypeName()

fun EntityType.formattedTypeName(): String = name.replace('_', ' ').capitalizeFully()

fun Entity.formattedTypeName(): String = type.formattedTypeName()

fun Player.isInventoryFull() = inventory.firstEmpty() == -1

val PersistentDataHolder.pdc
    get() = persistentDataContainer

fun LivingEntity.getCurrentHpPercent() = getAttribute(Attribute.GENERIC_MAX_HEALTH)?.let { health / it.value }?.toFloat() ?: 1f

fun LivingEntity.getValidNearbyEntities(range: Double) = location.getNearbyLivingEntities(range) { !it.isDead && it.isValid }

