package com.github.secretx33.infernalmobsreloaded.repositories

import com.cryptomorin.xseries.XEnchantment
import com.cryptomorin.xseries.XMaterial
import com.github.secretx33.infernalmobsreloaded.model.CustomEnchantment
import com.github.secretx33.infernalmobsreloaded.model.LootItem
import com.github.secretx33.infernalmobsreloaded.utils.YamlManager
import com.github.secretx33.infernalmobsreloaded.utils.formattedTypeName
import me.mattstudios.msg.adventure.AdventureMessage
import net.kyori.adventure.text.Component
import org.bukkit.Color
import org.bukkit.DyeColor
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.plugin.Plugin
import java.util.*
import java.util.logging.Logger
import kotlin.math.max
import kotlin.math.min

class LootItemsRepo (
    plugin: Plugin,
    private val log: Logger,
    private val adventureMessage: AdventureMessage,
) {

    private val manager = YamlManager(plugin, "loot_table")
    private var lootItemCache = emptyMap<String, LootItem>()     // lowercase lootNames, LootItem
    private var lootItemNames = emptyList<String>()              // original lootNames

    init { reload() }

    fun reload() {
        manager.reload()
        ensureUniqueKeys()
        loadLootTable()
    }

    //    fun getTypeOrNull(group: String): HarvestBlockGroup? = typeCache[group.toLowerCase(Locale.US)]

    fun getLootItemOrNull(name: String) = lootItemCache[name.toLowerCase(Locale.US)]

    fun getLootItem(name: String): LootItem = getLootItemOrNull(name) ?: throw NoSuchElementException("Loot item named $name was not found.")

    fun hasLootItem(name: String) = lootItemCache.containsKey(name.toLowerCase(Locale.US))

    private fun ensureUniqueKeys() {
        val duplicatedKeys = manager.getKeys(false).groupBy { it.toLowerCase(Locale.US) }.filterValues { it.size > 1 }
        // if there are duplicates in keys
        if(duplicatedKeys.isNotEmpty()) {
            val sb = StringBuilder("Oops, seems like there are duplicate item loot names in file '${manager.fileName}', remember that item names are caSE inSenSiTiVe, so make sure that each item has a unique name. Duplicated item names: ")
            duplicatedKeys.entries.forEachIndexed { index, (k, v) ->
                sb.append("\n${index + 1}) $k = {${v.joinToString()}}")
            }
            log.severe(sb.toString())
        }
    }

    private fun loadLootTable() {
        lootItemNames = manager.getKeys(false).sorted()
        lootItemCache = lootItemNames.map { it.toLowerCase(Locale.US) }.associateWithTo(HashMap(lootItemNames.size)) { makeLootItem(it) }
    }

    private fun makeLootItem(name: String): LootItem {
        val material = getItemMaterial(name)
        val displayName = parseDisplayName(name, material)
        val color = getItemColor(name)
        val dyeColor = getItemDyeColor(name)
        val amounts = getAmounts(name)
        val lore = getItemLore(name)
        val enchants = getItemEnchants(name)
        return LootItem(name,
            displayName = displayName,
            material = material,
            color = color,
            dyeColor = dyeColor,
            minAmount = amounts.first,
            maxAmount = amounts.second,
            lore = lore,
            enchants = enchants,
        )
    }

    private fun getItemColor(name: String): Color? {
        val colorRGB = manager.getString("$name.color") ?: ""
        // if color name is absent or blank
        if(colorRGB.isBlank()) return null
        return colorRGB.toColor()
    }

    private fun String.toColor(): Color {
        val results = COLOR_PATTERN.find(this.trim())?.groupValues
        if(results?.size != 4) {
            log.warning("Inside loot items, seems like you have malformed color string in your config file, please fix color entry with value '$this' and reload the plugin configurations.")
            return Color.WHITE
        }
        val r = results[1].toInt()
        val g = results[2].toInt()
        val b = results[3].toInt()
        return try {
            Color.fromRGB(r, g, b)
        } catch(e: IllegalArgumentException) {
            log.warning("Inside loot items, seems like you have typoed a invalid number somewhere in '$this', please only use values between 0 and 255 to write the colors. Original error message: ${e.message}")
            Color.WHITE
        }
    }

    private fun getItemDyeColor(name: String): DyeColor {
        val colorName = manager.getString("$name.dye-color") ?: ""

        // if dye color name is absent or blank
        if(colorName.isBlank()) return DyeColor.WHITE

        return DyeColor.values().firstOrNull { it.name.equals(colorName, ignoreCase = true) } ?: run {
            log.warning("Inside item loot '$name', dye color named '$colorName' doesn't exist, please fix your item loot configurations. Defaulting $name color to white.")
            DyeColor.WHITE
        }
    }

    private fun getItemEnchants(name: String): Set<CustomEnchantment> {
        // enchant pattern "enchant_name:minLevel-maxLevel:chance", with only the first argument being mandatory
        val enchants = manager.getStringList("$name.enchants")

        // if there's no enchant
        if(enchants.isEmpty()) return emptySet()

        return enchants.mapTo(HashSet()) { line ->
            val fields = line.split(':')

            val enchant = XEnchantment.matchXEnchantment(fields[0]).map { it.parseEnchantment() }.orElseGet {
                log.warning("Inside item loot '$name', enchantment with name '${fields[0]}' doesn't exist, please fix your item loot configurations. Defaulting this enchantment to ${Enchantment.LUCK}.")
                Enchantment.LUCK
            }!!
            if(fields.size == 1) return@mapTo CustomEnchantment(type = enchant, minLevel = 0, maxLevel = 0, chance = 1.0)

            // split the level section by '-' to get the enchant minLevel and maxLevel
            val levels = fields[1].split('-')

            // get the enchant minLevel and maxLevel as well, if present
            val minLevel = levels[0].toIntOrNull()?.let { max(0, it - 1) } ?: run {
                log.warning("Inside item loot '$name', level '${levels[0]}' for enchantment $enchant is not an integer. Defaulting $name's $enchant level to 1.")
                1
            }
            // get the enchant maxLevel or just default it to minLevel, in case of missing or invalid argument
            val maxLevel = levels.getOrNull(1)?.toIntOrNull()?.let { max(minLevel, it - 1) } ?: minLevel

            if(fields.size == 2) return@mapTo CustomEnchantment(type = enchant, minLevel = minLevel, maxLevel = maxLevel, chance = 1.0)

            // parse the chance of that enchant to be applied to the item
            val chance = fields[2].toDoubleOrNull()?.let { max(0.0, min(1.0, it)) } ?: run {
                log.warning("Inside item loot '$name', chance for enchantment '${levels[0]}' is invalid. Defaulting $name's $enchant enchant chance to 100%.")
                1.0
            }
            CustomEnchantment(type = enchant, minLevel = minLevel, maxLevel = maxLevel, chance = chance)
        }
    }

    // returns a pair with <Min, Max> amount of that item
    private fun getAmounts(name: String): Pair<Int, Int> {
        val amounts = (manager.getString("$name.amount") ?: "").split('-', limit = 2)

        // if there's no amount field, default it to 1
        if(amounts[0].isBlank()) return Pair(1, 1)

        // if typed amount is not an integer
        val minAmount = amounts[0].toIntOrNull()?.let { max(1, it) } ?: run {
            log.warning("Amount provided for item loot '$name' is not an integer. Defaulting '$name' amount to 1.")
            return Pair(1, 1)
        }

        // if there's one one number, min and max amounts should be equal
        if(amounts.size < 2 || amounts[1].isBlank()) return Pair(minAmount, minAmount)

        val maxAmount = amounts[1].toIntOrNull()?.let { max(minAmount, it) } ?: run {
            log.warning("Max amount provided for item loot '$name' is not an integer, please fix the typo and reload the configurations. Defaulting '$name' max amount to its minimum amount, which is $minAmount.")
            minAmount
        }
        return Pair(minAmount, maxAmount)
    }

    private fun getItemMaterial(name: String): Material {
        val materialName = manager.getString("$name.material") ?: ""

        // if display name is absent or blank
        if(materialName.isBlank()) {
            log.warning("You must provide a material for the item loot '$name'! Please fix your item loot configurations, defaulting $name material to Stone.")
            return Material.STONE
        }

        return XMaterial.matchXMaterial(materialName).map { it.parseMaterial() }.filter { it?.isItem == true }.orElseGet {
            log.warning("Inside item loot '$name', material '$materialName' doesn't exist, please fix your item loot configurations. Defaulting $name material to Stone.")
            Material.STONE
        }!!
    }

    private fun getItemLore(name: String): List<Component> = manager.getStringList("$name.lore").map { adventureMessage.parse(it) }

    private fun parseDisplayName(name: String, material: Material): Component {
        val displayName = manager.getString("$name.name") ?: ""

        // if display name is absent or blank
        if(displayName.isBlank()) {
            log.warning("You must provide a display name for the item '$name'! Defaulting '$name' display name to its material name.")
            return Component.text(material.formattedTypeName())
        }
        return adventureMessage.parse(displayName)
    }

    private companion object {
        val COLOR_PATTERN = """^(\d+?),\s*(\d+?),\s*(\d+)$""".toRegex()
    }
}
