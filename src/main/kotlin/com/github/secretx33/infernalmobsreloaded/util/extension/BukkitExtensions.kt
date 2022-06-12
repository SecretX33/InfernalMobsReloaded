package com.github.secretx33.infernalmobsreloaded.util.extension

import com.github.secretx33.infernalmobsreloaded.InfernalMobsReloaded
import com.github.secretx33.infernalmobsreloaded.config.toComponent
import com.github.secretx33.infernalmobsreloaded.model.InfernalMobType
import com.github.secretx33.infernalmobsreloaded.model.KeyChain
import kotlinx.coroutines.delay
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.EntityEquipment
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataHolder
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin
import toothpick.ktp.extension.getInstance
import java.util.concurrent.Callable
import java.util.concurrent.Future

/**
 * Small static access to avoid a lot of boilerplate code.
 */
private val keyChain by lazy(LazyThreadSafetyMode.NONE) { InfernalMobsReloaded.scope.getInstance<KeyChain>() }

fun ItemStack.turnIntoSpawner(infernalType: InfernalMobType): ItemStack {
    require(type == Material.SPAWNER) { "may only turn into spawn actual spawners, $type is not spawner" }
    itemMeta.let {
        it.displayName(infernalType.mobSpawnerName)
        it.pdc.set(keyChain.spawnerCategoryKey, PersistentDataType.STRING, infernalType.name)
        itemMeta = it
    }
    return this
}

fun ItemMeta.markWithInfernalTag(itemName: String): ItemMeta {
    pdc.set(keyChain.infernalItemNameKey, PersistentDataType.STRING, itemName)
    return this
}

fun Player.getTarget(range: Int): LivingEntity? = (world.rayTraceEntities(eyeLocation, eyeLocation.direction, range.toDouble()) { it is LivingEntity && type != EntityType.ENDER_DRAGON && it.uniqueId != uniqueId }
    ?.hitEntity as? LivingEntity)
    ?.takeIf { hasLineOfSight(it) }

fun runSync(plugin: Plugin, delay: Long = 0L, runnable: Runnable) {
    when {
        delay < 0L -> {}
        delay == 0L -> Bukkit.getScheduler().runTask(plugin, runnable)
        else -> Bukkit.getScheduler().runTaskLater(plugin, runnable, delay / 50L)
    }
}

suspend fun <T> futureSync(plugin: Plugin, callable: Callable<T>): T = Bukkit.getScheduler().callSyncMethod(plugin, callable).await()

suspend fun <T> Future<T>.await(): T {
    while(!isDone)
        delay(25) // or whatever you want your polling frequency to be
    @Suppress("BlockingMethodInNonBlockingContext")
    return get()
}

fun ItemStack.isAir(): Boolean = type.isAir

fun Material.formattedTypeName(): String = name.replace('_', ' ').capitalizeFully()

fun ItemStack.formattedTypeName(): String = type.formattedTypeName()

val ItemStack.displayName: Component
    get() = itemMeta?.displayName()?.takeIf { it.toString().isNotBlank() } ?: formattedTypeName().toComponent()

fun EntityType.formattedTypeName(): String = name.replace('_', ' ').capitalizeFully()

fun Entity.formattedTypeName(): String = type.formattedTypeName()

val Entity.displayName: Component
    get() = customName() ?: type.formattedTypeName().toComponent()

val PersistentDataHolder.pdc
    get() = persistentDataContainer

fun LivingEntity.getHealthPercent(damageTaken: Double = 0.0): Float =
    getAttribute(Attribute.GENERIC_MAX_HEALTH)?.let { (health - damageTaken).coerceAtLeast(0.0) / it.value }?.toFloat() ?: 1f

@Suppress("UselessCallOnCollection")
val EntityEquipment.contents: List<ItemStack>
    get() = EquipmentSlot.values()
        .mapNotNull { getItem(it) }
        .filter { !it.isAir() }

@Suppress("UNNECESSARY_SAFE_CALL", "UselessCallOnCollection")
val EntityEquipment.contentsMap: Map<EquipmentSlot, ItemStack>
    get() = EquipmentSlot.values()
        .mapNotNull { slot -> getItem(slot)?.let { slot to it } }
        .filter { !it.second.isAir() }
        .toMap()
