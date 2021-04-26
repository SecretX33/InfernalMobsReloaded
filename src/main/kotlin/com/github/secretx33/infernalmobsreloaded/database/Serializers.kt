package com.github.secretx33.herbalism.database

import com.github.secretx33.infernalmobsreloaded.utils.formattedString
import com.github.secretx33.infernalmobsreloaded.utils.toUuid
import com.google.gson.*
import org.bukkit.Bukkit
import org.bukkit.Location
import java.lang.reflect.Type

class LocationAdapter : JsonSerializer<Location>, JsonDeserializer<Location> {

    override fun serialize(location: Location, type: Type, context: JsonSerializationContext): JsonElement {
        val world = location.world ?: throw IllegalStateException("cannot serialize location with null world, ${location.formattedString()}")
        return JsonObject().apply {
            addProperty("world", world.uid.toString())
            addProperty("x", location.blockX)
            addProperty("y", location.blockY)
            addProperty("z", location.blockZ)
        }
    }

    override fun deserialize(json: JsonElement, type: Type, context: JsonDeserializationContext): Location {
        val root = json.asJsonObject

        root.run {
            val world = Bukkit.getWorld(get("world").asString.toUuid())
            val x = get("x").asDouble
            val y = get("y").asDouble
            val z = get("z").asDouble
            return Location(world, x, y, z)
        }
    }
}
