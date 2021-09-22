package com.github.secretx33.infernalmobsreloaded.repositories

import com.cryptomorin.xseries.XEnchantment
import com.cryptomorin.xseries.XMaterial
import com.github.secretx33.infernalmobsreloaded.config.toComponent
import com.github.secretx33.infernalmobsreloaded.model.CustomEnchantment
import com.github.secretx33.infernalmobsreloaded.model.KeyChain
import com.github.secretx33.infernalmobsreloaded.model.items.BannerLootItem
import com.github.secretx33.infernalmobsreloaded.model.items.LootBook
import com.github.secretx33.infernalmobsreloaded.model.items.LootItem
import com.github.secretx33.infernalmobsreloaded.model.items.LootItemType
import com.github.secretx33.infernalmobsreloaded.model.items.NormalLootItem
import com.github.secretx33.infernalmobsreloaded.model.items.ShieldWithPatternLootItem
import com.github.secretx33.infernalmobsreloaded.utils.extension.formattedTypeName
import com.github.secretx33.infernalmobsreloaded.utils.extension.pdc
import com.github.secretx33.infernalmobsreloaded.utils.other.YamlManager
import me.mattstudios.msg.adventure.AdventureMessage
import net.kyori.adventure.text.Component
import org.bukkit.Color
import org.bukkit.DyeColor
import org.bukkit.Material
import org.bukkit.block.banner.Pattern
import org.bukkit.block.banner.PatternType
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BookMeta
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin
import java.util.EnumSet
import java.util.Locale
import java.util.logging.Logger
import kotlin.math.max
import kotlin.math.min

class LootItemsRepo (
    plugin: Plugin,
    private val keyChain: KeyChain,
    private val adventureMessage: AdventureMessage,
) {
    private val logger: Logger = plugin.logger
    private val manager = YamlManager(plugin, "loot_table")

    private var lootItemCache = emptyMap<String, LootItem>()     // lowercase lootNames, LootItem
    private var lootItemNames = emptyList<String>()              // original lootNames

    init { reload() }

    fun reload() {
        manager.reload()
        loadLootTable()
    }

    fun getLootItemOrNull(name: String) = lootItemCache[name.lowercase(Locale.US)]

    fun getLootItem(name: String): LootItem = getLootItemOrNull(name) ?: throw NoSuchElementException("Loot item named $name was not found.")

    fun hasLootItem(name: String) = lootItemCache.containsKey(name.lowercase(Locale.US))

    fun getAllLootItems() = lootItemNames

    fun isLootItem(item: ItemStack) = item.itemMeta?.pdc?.get(keyChain.infernalItemNameKey, PersistentDataType.STRING)?.let { lootItemCache.keys.contains(it) } == true

    fun getLootItemTagOrNull(item: ItemStack) = item.itemMeta?.pdc?.get(keyChain.infernalItemNameKey, PersistentDataType.STRING)

    fun getLootItemTag(item: ItemStack) = getLootItemTagOrNull(item) ?: throw IllegalStateException("Tried to get charm effect of item ${item.formattedTypeName()} but this item doesn't contain the infernalItemNameKey pdc key")

    private fun loadLootTable() {
        lootItemNames = manager.getKeys(false).sorted()
        lootItemCache = lootItemNames.map { it.lowercase(Locale.US) }.associateWithTo(HashMap(lootItemNames.size)) { makeLootItem(it) }
    }

    private fun makeLootItem(name: String): LootItem {
        return when(getLootType(name)) {
            LootItemType.NORMAL -> makeNormalLootItem(name)
            LootItemType.BOOK -> makeLootBook(name)
        }
    }

    private fun getLootType(name: String): LootItemType {
        val itemType = manager.getString("$name.type") ?: ""

        // if item type is absent or blank, assume normal item
        if(itemType.isBlank()) return LootItemType.NORMAL

        return LootItemType.values().firstOrNull { it.name.equals(itemType, ignoreCase = true) } ?: run {
            logger.warning("Inside item loot '$name', item type named '$itemType' doesn't exist, please fix your item loot configurations. Defaulting $name item type to 'normal'.")
            LootItemType.NORMAL
        }
    }

    // Loot Book

    private fun makeLootBook(name: String): LootItem {
        val material = getBookMaterial(name)
        return LootBook(
            name,
            material = material,
            title = getBookTitle(name, material),
            author = getBookAuthor(name),
            generation = getBookGeneration(name),
            pages = getBookPages(name),
            flags = getItemFlags(name),
            enchants = getItemEnchants(name),
        )
    }

    private fun getBookMaterial(name: String): Material {
        val materialName = manager.getString("$name.material") ?: ""

        // if material name is absent or blank
        if(materialName.isBlank()) return Material.WRITTEN_BOOK

        return XMaterial.matchXMaterial(materialName).map { it.parseMaterial() }.filter { it == Material.WRITTEN_BOOK || it == Material.WRITABLE_BOOK }.orElseGet {
            logger.warning("Inside item loot '$name', material '$materialName' doesn't exist or is invalid, please fix your item loot configurations. Defaulting $name material to 'Written Book'.")
            Material.WRITTEN_BOOK
        }!!
    }

    private fun getBookTitle(name: String, material: Material): Component {
        val bookTitle = manager.getString("$name.title") ?: ""

        // if book title is absent or blank
        if(bookTitle.isBlank()) {
            logger.warning("You must provide a title for the loot book '$name'! Defaulting '$name' title to its material name.")
            return material.formattedTypeName().toComponent()
        }

        return adventureMessage.parse(bookTitle)
    }

    private fun getBookAuthor(name: String): Component? {
        val bookAuthor = manager.getString("$name.author") ?: return null  // if book author is absent or blank
        return bookAuthor.toComponent()
    }

    private fun getBookGeneration(name: String): BookMeta.Generation {
        val generation = manager.getString("$name.generation") ?: ""

        // if book generation is absent or blank
        if(generation.isBlank()) return BookMeta.Generation.ORIGINAL

        return BookMeta.Generation.values().firstOrNull { it.name.equals(generation, ignoreCase = true) } ?: run {
            logger.warning("Inside item loot '$name', book generation '$generation' doesn't exist or is invalid, please fix your item loot configurations. Defaulting $name generation to 'Original'.")
            BookMeta.Generation.ORIGINAL
        }
    }

    private fun getBookPages(name: String): List<Component>
        = manager.getStringList("$name.pages").map { adventureMessage.parse("<#000000>$it") } // otherwise the text becomes transparent

    // Normal Loot Item

    private fun makeNormalLootItem(name: String): NormalLootItem {
        val material = getItemMaterial(name)
        val displayName = parseDisplayName(name, material)
        val color = getItemColor(name)
        val dyeColor = getItemDyeColor(name)
        val amounts = getAmounts(name)
        val flags = getItemFlags(name)
        val lore = getItemLore(name)
        val enchants = getItemEnchants(name)

        if(material == Material.SHIELD) return shieldWithPatternLootItem(name, displayName, material, color, dyeColor, amounts, flags, lore, enchants)

        if(material.name.contains("banner", ignoreCase = true)) return bannerLootItem(name, displayName, material, color, dyeColor, amounts, flags, lore, enchants)

        return genericLootItem(name, displayName, material, color, dyeColor, amounts, flags, lore, enchants)
    }

    private fun bannerLootItem(name: String, displayName: Component, material: Material, color: Color?, dyeColor: DyeColor?, amounts: Pair<Int, Int>, flags: Set<ItemFlag>, lore: List<Component>, enchants: Set<CustomEnchantment>) =
        BannerLootItem(
            name,
            displayName = displayName,
            material = material,
            color = color,
            dyeColor = dyeColor,
            amount = amounts,
            flags = flags,
            lore = lore,
            enchants = enchants,
            patterns = getPatterns(name),
        )

    private fun shieldWithPatternLootItem(name: String, displayName: Component, material: Material, color: Color?, dyeColor: DyeColor?, amounts: Pair<Int, Int>, flags: Set<ItemFlag>, lore: List<Component>, enchants: Set<CustomEnchantment>) =
        ShieldWithPatternLootItem(
            name,
            displayName = displayName,
            material = material,
            color = color,
            dyeColor = dyeColor,
            amount = amounts,
            flags = flags,
            lore = lore,
            enchants = enchants,
            patterns = getPatterns(name),
        )

    private fun getPatterns(name: String): List<Pattern> {
        val patternList = manager.getStringList("$name.patterns").takeUnless { it.isEmpty() } ?: return emptyList()

        return patternList.mapNotNull { line ->
            val split = line.split(':', limit = 2).takeIf { it.isNotEmpty() } ?: return@mapNotNull null

            val pattern = PatternType.values().firstOrNull { it.name.equals(split[0], ignoreCase = true) || it.identifier.equals(split[0], ignoreCase = true) } ?: run {
                logger.warning("Invalid pattern '${split[0]}' for loot item '$name', please fix your configurations and reload.")
                return@mapNotNull null
            }
            if(split.size == 1) return@mapNotNull Pattern(DyeColor.WHITE, pattern)

            val dyeColor = DyeColor.values().firstOrNull { it.name.equals(split[1], ignoreCase = true) } ?: run {
                logger.warning("Invalid dye color '${split[0]}' in pattern '${split[0]}' for loot item '$name', please fix your configurations and reload.")
                return@mapNotNull Pattern(DyeColor.WHITE, pattern)
            }
            return@mapNotNull Pattern(dyeColor, pattern)
        }
    }

    private fun getItemFlags(name: String): Set<ItemFlag> {
        val itemFlags = manager.getStringList("$name.item-flags").filter { it.isNotBlank() }.takeUnless { it.isEmpty() } ?: return emptySet()

        return itemFlags.mapNotNullTo(EnumSet.noneOf(ItemFlag::class.java)) { flag ->
            ItemFlag.values().firstOrNull { it.name.equals(flag, ignoreCase = true) } ?: run {
                logger.warning("Invalid item flag '$flag' for loot item '$name', please fix your configurations and reload.")
                null
            }
        }
    }

    private fun getItemColor(name: String): Color? {
        val colorRGB = manager.getString("$name.color") ?: ""
        // if color name is absent or blank
        if(colorRGB.isBlank()) return null
        return colorRGB.toColor()
    }

    private fun parseDisplayName(name: String, material: Material): Component {
        val displayName = manager.getString("$name.name") ?: ""

        // if display name is absent or blank
        if(displayName.isBlank()) {
            logger.warning("You must provide a display name for the item '$name'! Defaulting '$name' display name to its material name.")
            return Component.text(material.formattedTypeName())
        }
        return adventureMessage.parse(displayName)
    }

    private fun genericLootItem(name: String, displayName: Component, material: Material, color: Color?, dyeColor: DyeColor?, amounts: Pair<Int, Int>, flags: Set<ItemFlag>, lore: List<Component>, enchants: Set<CustomEnchantment>): NormalLootItem {
        return NormalLootItem(name,
            displayName = displayName,
            material = material,
            color = color,
            dyeColor = dyeColor,
            amount = amounts,
            flags = flags,
            lore = lore,
            enchants = enchants,
        )
    }

    private fun String.toColor(): Color {
        val results = COLOR_PATTERN.find(this.trim())?.groupValues
        if(results?.size != 4) {
            logger.warning("Inside loot items, seems like you have malformed color string in your config file, please fix color entry with value '$this' and reload the plugin configurations.")
            return Color.WHITE
        }
        val r = results[1].toInt()
        val g = results[2].toInt()
        val b = results[3].toInt()
        return try {
            Color.fromRGB(r, g, b)
        } catch(e: IllegalArgumentException) {
            logger.warning("Inside loot items, seems like you have typoed a invalid number somewhere in '$this', please only use values between 0 and 255 to write the colors. Original error message: ${e.message}")
            Color.WHITE
        }
    }

    private fun getItemDyeColor(name: String): DyeColor? {
        val colorName = manager.getString("$name.dye-color")?.takeIf { it.isNotBlank() } ?: return null

        return DyeColor.values().firstOrNull { it.name.equals(colorName, ignoreCase = true) } ?: run {
            logger.warning("Inside item loot '$name', dye color named '$colorName' doesn't exist, please fix your item loot configurations. Defaulting $name color to white.")
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
                logger.warning("Inside item loot '$name', enchantment with name '${fields[0]}' doesn't exist, please fix your item loot configurations. Defaulting this enchantment to LUCK.")
                Enchantment.LUCK
            }!!
            if(fields.size == 1) return@mapTo CustomEnchantment(type = enchant, levels = Pair(1, 1), chance = 1.0)

            // split the level section by '-' to get the enchant minLevel and maxLevel
            val levels = fields[1].split('-')

            // get the enchant minLevel and maxLevel as well, if present
            val minLevel = levels[0].toIntOrNull()?.let { max(1, it) } ?: run {
                logger.warning("Inside item loot '$name', level '${levels[0]}' for enchantment $enchant is not an integer. Defaulting $name's $enchant level to 1.")
                1
            }
            // get the enchant maxLevel or just default it to minLevel, in case of missing or invalid argument
            val maxLevel = levels.getOrNull(1)?.toIntOrNull()?.let { max(minLevel, it) } ?: minLevel

            if(fields.size == 2) return@mapTo CustomEnchantment(type = enchant, levels = Pair(minLevel, maxLevel), chance = 1.0)

            // parse the chance of that enchant to be applied to the item
            val chance = fields[2].toDoubleOrNull()?.let { max(0.0, min(1.0, it)) } ?: run {
                logger.warning("Inside item loot '$name', chance for enchantment '${levels[0]}' is invalid. Defaulting $name's $enchant enchant chance to 100%.")
                1.0
            }
            CustomEnchantment(type = enchant, levels = Pair(minLevel, maxLevel), chance = chance)
        }
    }

    // returns a pair with <Min, Max> amount of that item
    private fun getAmounts(name: String): Pair<Int, Int> {
        val amounts = (manager.getString("$name.amount") ?: "").split('-', limit = 2)

        // if there's no amount field, default it to 1
        if(amounts[0].isBlank()) return Pair(1, 1)

        // if typed amount is not an integer
        val minAmount = amounts[0].toIntOrNull()?.let { max(1, it) } ?: run {
            logger.warning("Amount provided for item loot '$name' is not an integer. Defaulting '$name' amount to 1.")
            return Pair(1, 1)
        }

        // if there's one one number, min and max amounts should be equal
        if(amounts.size < 2 || amounts[1].isBlank()) return Pair(minAmount, minAmount)

        val maxAmount = amounts[1].toIntOrNull()?.let { max(minAmount, it) } ?: run {
            logger.warning("Max amount provided for item loot '$name' is not an integer, please fix the typo and reload the configurations. Defaulting '$name' max amount to its minimum amount, which is $minAmount.")
            minAmount
        }
        return Pair(minAmount, maxAmount)
    }

    private fun getItemMaterial(name: String): Material {
        val materialName = manager.getString("$name.material") ?: ""

        // if material name is absent or blank
        if(materialName.isBlank()) {
            logger.warning("You must provide a material for the item loot '$name'! Please fix your item loot configurations, defaulting $name material to Stone.")
            return Material.STONE
        }

        return XMaterial.matchXMaterial(materialName).map { it.parseMaterial() }.filter { it?.isItem == true }.orElseGet {
            logger.warning("Inside item loot '$name', material '$materialName' doesn't exist, please fix your item loot configurations. Defaulting $name material to Stone.")
            Material.STONE
        }!!
    }

    private fun getItemLore(name: String): List<Component> = manager.getStringList("$name.lore").map { adventureMessage.parse(it) }

    private companion object {
        val COLOR_PATTERN = """^(\d+?),\s*(\d+?),\s*(\d+)$""".toRegex()
    }
}
