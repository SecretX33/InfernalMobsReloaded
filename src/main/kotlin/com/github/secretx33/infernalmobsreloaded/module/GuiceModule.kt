package com.github.secretx33.infernalmobsreloaded.module

import com.github.secretx33.infernalmobsreloaded.annotations.PluginId
import com.google.inject.AbstractModule
import com.google.inject.util.Modules
import me.mattstudios.msg.adventure.AdventureMessage
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin

class GuiceModule(private val plugin: JavaPlugin) : AbstractModule() {

    private val extraBindings = HashMap<Class<*>, Any>()

    override fun configure() {
        install(Modules.disableCircularProxiesModule())
        bind(Plugin::class.java).toInstance(plugin)
        bind(JavaPlugin::class.java).toInstance(plugin)
        bind(AdventureMessage::class.java).toInstance(AdventureMessage.create())
        bind(Int::class.java).annotatedWith(PluginId::class.java).toInstance(11253)
//        extraBindings.forEach { (clazz, instance) -> bind(clazz).toInstance(clazz.cast(instance)) }
    }

//    fun <T : Any> addBind(clazz: Class<T>, instance: T) {
//        extraBindings[clazz] = instance
//    }

//    private fun <T : Any> Any.()
}
