package com.github.secretx33.infernalmobsreloaded.scanning.rules.annotation

import java.lang.reflect.AnnotatedElement

// ------------------------------
// Copyright (c) PiggyPiglet 2021
// https://www.piggypiglet.me
// ------------------------------
object AnnotationUtils {

    fun isAnnotationPresent(element: AnnotatedElement, annotation: AnnotationWrapper): Boolean {
        val clazz = annotation.annotationClass
        val instance = annotation.annotationInstance
        if (clazz != null) return element.isAnnotationPresent(clazz)
        return instance?.annotationClass?.java?.let { element.getAnnotation(it) == instance } == true
    }
}
