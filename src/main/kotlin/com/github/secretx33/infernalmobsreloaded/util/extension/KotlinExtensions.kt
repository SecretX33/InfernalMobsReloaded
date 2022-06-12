package com.github.secretx33.infernalmobsreloaded.util.extension

import com.google.common.reflect.ClassPath
import org.apache.commons.lang.WordUtils
import java.util.UUID
import java.util.concurrent.ThreadLocalRandom
import kotlin.reflect.KClass

fun Pair<Int, Int>.random(): Int {
    val (minValue, maxValue) = this
    return ThreadLocalRandom.current().nextInt(maxValue - minValue + 1) + minValue
}

fun Pair<Double, Double>.random(): Double {
    val (minValue, maxValue) = this
    return minValue + (maxValue - minValue) * ThreadLocalRandom.current().nextDouble()
}

fun String.capitalizeFully(): String = WordUtils.capitalizeFully(this)

fun String.toUuid(): UUID = UUID.fromString(this)

fun Regex.matchOrNull(line: String, index: Int): String? = this.matchEntire(line)?.groupValues?.get(index)

fun <T> MutableSet<T>.replace(oldElement: T, newElement: T) {
    remove(oldElement)
    add(newElement)
}

@Suppress("UnstableApiUsage", "UNCHECKED_CAST")
inline fun <T : Any> Any.findClasses(pkg: String, filter: (KClass<*>) -> Boolean): Set<KClass<T>> =
    ClassPath.from(this::class.java.classLoader)
        .getTopLevelClassesRecursive(pkg)
        .map { it.load().kotlin }
        .filterTo(mutableSetOf(), filter) as Set<KClass<T>>
