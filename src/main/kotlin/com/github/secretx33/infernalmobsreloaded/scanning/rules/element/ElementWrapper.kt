package com.github.secretx33.infernalmobsreloaded.scanning.rules.element

import java.lang.reflect.AnnotatedElement

data class ElementWrapper(
    val type: Class<*>,
    val element: AnnotatedElement,
)
