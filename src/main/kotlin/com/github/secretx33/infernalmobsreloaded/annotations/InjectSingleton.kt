package com.github.secretx33.infernalmobsreloaded.annotations

import toothpick.InjectConstructor
import javax.inject.Singleton

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Singleton
@InjectConstructor
annotation class InjectSingleton
