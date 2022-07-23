package com.github.secretx33.infernalmobsreloaded.util.other

import com.github.secretx33.infernalmobsreloaded.InfernalMobsReloaded
import org.bukkit.Bukkit
import toothpick.configuration.Configuration

private val IS_DEVELOPMENT = System.getenv("BUKKIT_DEVELOPMENT").toBoolean()

fun getEnvConfiguration(): Configuration =
    when (IS_DEVELOPMENT) {
        true -> {
            Bukkit.getServer().consoleSender.sendMessage("[${InfernalMobsReloaded.PLUGIN_NAME}] Running in development mode.")
            Configuration.forDevelopment()
        }
        false -> Configuration.forProduction()
    }
