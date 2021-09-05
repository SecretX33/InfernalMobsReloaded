package com.github.secretx33.infernalmobsreloaded.utils.extension

import org.apache.commons.lang.WordUtils
import java.util.UUID
import java.util.concurrent.ThreadLocalRandom

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
