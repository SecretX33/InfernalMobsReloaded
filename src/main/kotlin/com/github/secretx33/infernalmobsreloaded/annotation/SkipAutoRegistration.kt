package com.github.secretx33.infernalmobsreloaded.annotation

import org.bukkit.event.Listener

/**
 * If a [Listener] class is annotated with `SkipAutoRegistration`, it won't be automatically discovered in the
 * classpath scan, thus requiring manual registering later.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class SkipAutoRegistration
