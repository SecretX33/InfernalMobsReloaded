@file:Suppress("UnstableApiUsage")

package com.github.secretx33.infernalmobsreloaded.util.extension

import com.github.secretx33.infernalmobsreloaded.InfernalMobsReloaded
import com.github.secretx33.infernalmobsreloaded.annotation.SkipAutoRegistration
import com.google.common.reflect.ClassPath
import com.google.common.reflect.TypeToken
import org.apache.commons.lang.WordUtils
import java.lang.reflect.Modifier
import java.lang.reflect.Type
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Duration
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ThreadLocalRandom
import kotlin.reflect.KClass

/**
 * Simplify `Random` usage by providing an extension function to retrieve the current thread local random instance.
 */
val random: ThreadLocalRandom get() = ThreadLocalRandom.current()

fun Pair<Int, Int>.random(): Int {
    val (minValue, maxValue) = this
    return random.nextInt(maxValue - minValue + 1) + minValue
}

fun Pair<Double, Double>.random(): Double {
    val (minValue, maxValue) = this
    return minValue + (maxValue - minValue) * random.nextDouble()
}

fun String.capitalizeFully(): String = WordUtils.capitalizeFully(this)

fun String.toUuid(): UUID = UUID.fromString(this)

fun Regex.matchOrNull(line: String, index: Int): String? = matchEntire(line)?.groupValues?.get(index)

inline fun <reified T : Any> gsonTypeToken(): Type = object : TypeToken<T>() {}.type

fun <T> MutableSet<T>.replace(oldElement: T, newElement: T) {
    remove(oldElement)
    add(newElement)
}

val KClass<*>.isConcreteType: Boolean
    get() = !Modifier.isAbstract(java.modifiers) && !Modifier.isInterface(java.modifiers)

fun KClass<*>.isSubclassOf(clazz: KClass<*>): Boolean = clazz.java.isAssignableFrom(java)

inline fun <reified T : Annotation> KClass<*>.hasAnnotation(): Boolean = java.isAnnotationPresent(T::class.java)

@Suppress("UNCHECKED_CAST")
inline fun <T : Any> findClasses(pkg: String, filter: (KClass<*>) -> Boolean): Set<KClass<T>> =
    ClassPath.from(InfernalMobsReloaded::class.java.classLoader)
        .getTopLevelClassesRecursive(pkg)
        .map { it.load().kotlin }
        .filterTo(mutableSetOf(), filter) as Set<KClass<T>>

fun <T : Any> Iterable<KClass<T>>.onlyRegisterableClasses(): Set<KClass<T>> =
    filterNotTo(mutableSetOf()) { it.hasAnnotation<SkipAutoRegistration>() }

inline fun <reified T : Any> findRegistrableClasses(pkg: String): Set<KClass<T>> =
    findClasses<T>(pkg) { it.isConcreteType && it.isSubclassOf(T::class) }
        .onlyRegisterableClasses()
