package com.github.secretx33.infernalmobsreloaded.util

import com.github.secretx33.infernalmobsreloaded.util.extension.random
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.entity.Player
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class Cuboid(point1: Location, point2: Location) {

    private val xMin: Int = min(point1.blockX, point2.blockX)
    private val xMax: Int = max(point1.blockX, point2.blockX)
    private val yMin: Int = min(point1.blockY, point2.blockY)
    private val yMax: Int = max(point1.blockY, point2.blockY)
    private val zMin: Int = min(point1.blockZ, point2.blockZ)
    private val zMax: Int = max(point1.blockZ, point2.blockZ)
    private val xMinCentered: Double = xMin + 0.5
    private val xMaxCentered: Double = xMax + 0.5
    private val yMinCentered: Double = yMin + 0.5
    private val yMaxCentered: Double = yMax + 0.5
    private val zMinCentered: Double = zMin + 0.5
    private val zMaxCentered: Double = zMax + 0.5
    private val world = point1.world ?: throw IllegalArgumentException("World cannot be null")

    fun bordersBlockList(): Set<Block> {
        val blockList = mutableSetOf<Block>()

        // For all heights
        for (y in yMin..yMax) {
            // Get North and South Walls
            for (x in xMin..xMax) {
                blockList += world.getBlockAt(x, y, zMin)  // South block
                blockList += world.getBlockAt(x, y, zMax)  // North block
            }

            // Get West and East Walls
            for (z in zMin..zMax) {
                blockList += world.getBlockAt(xMin, y, z)  // East block
                blockList += world.getBlockAt(xMax, y, z)  // West block
            }
        }
        return blockList
    }

    fun getFloorAndCeil(): Set<Block> {
        val blockList = mutableSetOf<Block>()

        // Adding floor and ceil of cube
        for (x in xMin..xMax) {
            for (z in zMin..zMax) {
                val floor = world.getBlockAt(x, yMin, z)
                blockList += floor
                val ceil = world.getBlockAt(x, yMax, z)
                blockList += ceil
            }
        }
        return blockList
    }

    fun getWalls(): Set<Block> {
        // There is no walls on this cuboid, just floor and ceil
        if (yMax - yMin < 2) return emptySet()

        val blockList = mutableSetOf<Block>()
        // For all block in between floor and ceil
        for (y in yMin + 1..yMax - 1) {
            // Get North and South Walls
            for (x in xMin..xMax) {
                blockList += world.getBlockAt(x, y, zMin)  // South block
                blockList += world.getBlockAt(x, y, zMax)  // North block
            }

            // Get West and East Walls
            for (z in zMin..zMax) {
                blockList += world.getBlockAt(xMin, y, z)  // East block
                blockList += world.getBlockAt(xMax, y, z)  // West block
            }
        }
        return blockList
    }

    fun blockList(): Set<Block> {
        val blockList = HashSet<Block>(totalBlockSize)
        for (x in xMin..xMax) {
            for (y in yMin..yMax) {
                for (z in zMin..zMax) {
                    blockList += world.getBlockAt(x, y, z)
                }
            }
        }
        return blockList
    }

    val center: Location
        get() = Location(world, (xMaxCentered - xMinCentered) / 2.0 + xMinCentered, (yMaxCentered - yMinCentered) / 2.0 + yMinCentered, (zMaxCentered - zMinCentered) / 2.0 + zMinCentered)

    val distance: Double
        get() = point1.distance(point2)

    val distanceSquared: Double
        get() = point1.distanceSquared(point2)

    val height: Int
        get() = yMax - yMin + 1

    val point1: Location
        get() = Location(world, xMin.toDouble(), yMin.toDouble(), zMin.toDouble())

    val point2: Location
        get() = Location(world, xMax.toDouble(), yMax.toDouble(), zMax.toDouble())

    val randomLocation: Location
        get() {
            val x: Double = random.nextInt(abs(xMax - xMin) + 1).toDouble() + xMin
            val y: Double = random.nextInt(abs(yMax - yMin) + 1).toDouble() + yMin
            val z: Double = random.nextInt(abs(zMax - zMin) + 1).toDouble() + zMin
            return Location(world, x, y, z)
        }

    val totalBlockSize: Int
        get() = height * xWidth * zWidth

    val xWidth: Int
        get() = xMax - xMin + 1

    val zWidth: Int
        get() = zMax - zMin + 1

    fun isIn(loc: Location): Boolean = loc.world?.uid == world.uid
        && loc.blockX >= xMin && loc.blockX <= xMax
        && loc.blockY >= yMin && loc.blockY <= yMax
        && loc.blockZ >= zMin && loc.blockZ <= zMax

    fun isIn(player: Player): Boolean = isIn(player.location)

    fun isInWithMarge(loc: Location, marge: Double): Boolean = loc.world?.uid == world.uid && loc.x >= xMinCentered - marge && loc.x <= xMaxCentered + marge && loc.y >= yMinCentered - marge && loc.y <= yMaxCentered + marge && loc.z >= zMinCentered - marge && loc.z <= zMaxCentered + marge
}
