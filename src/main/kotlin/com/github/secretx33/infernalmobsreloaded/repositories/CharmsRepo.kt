package com.github.secretx33.infernalmobsreloaded.repositories

import com.github.secretx33.infernalmobsreloaded.config.Config
import com.github.secretx33.infernalmobsreloaded.model.CharmEffect
import com.github.secretx33.infernalmobsreloaded.model.InfernalMobType
import com.github.secretx33.infernalmobsreloaded.utils.YamlManager
import com.google.common.collect.ImmutableSetMultimap
import me.mattstudios.msg.adventure.AdventureMessage
import org.bukkit.entity.EntityType
import org.bukkit.plugin.Plugin
import org.koin.core.component.KoinApiExtension
import java.util.*
import java.util.logging.Logger

@KoinApiExtension
class CharmsRepo (
    plugin: Plugin,
    private val log: Logger,
    private val config: Config,
    private val adventureMessage: AdventureMessage,
    private val lootItemsRepo: LootItemsRepo,
) {
    private val manager = YamlManager(plugin, "charms")
    private var infernalTypeCache = emptyMap<String, CharmEffect>()    // lowercase groupName, infernalType

    init { reload() }

    fun reload() {
        manager.reload()
        ensureUniqueKeys()
        loadMobTypes()
    }

    private fun ensureUniqueKeys() {
        val duplicatedKeys = manager.getKeys(false).groupBy { it.toLowerCase(Locale.US) }.filterValues { it.size > 1 }
        // if there are duplicates in keys
        if(duplicatedKeys.isNotEmpty()) {
            val sb = StringBuilder("Oops, seems like there are duplicate mob categories in file '${manager.fileName}', remember that categories are caSE inSenSiTiVe, so make sure that each category has a unique name. Duplicated mob categories: ")
            duplicatedKeys.entries.forEachIndexed { index, (k, v) ->
                sb.append("\n${index + 1}) $k = {${v.joinToString()}}")
            }
            log.warning(sb.toString())
        }
    }

    private fun loadMobTypes() {
        val keys = manager.getKeys(false)
        infernalTypeCache = keys.map { it.toLowerCase(Locale.US) }.associateWithTo(HashMap(keys.size)) { makeMobType(it) }
    }

    private fun makeMobType(name: String): InfernalMobType {
        return CharmEffect(name,
            playerMessage = ,
            targetMessage = ,
        )
    }
}
