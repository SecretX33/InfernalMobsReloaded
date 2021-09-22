package com.github.secretx33.infernalmobsreloaded.annotations

import com.google.inject.BindingAnnotation

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@BindingAnnotation
annotation class PluginId
