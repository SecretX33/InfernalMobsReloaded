package com.github.secretx33.infernalmobsreloaded.scanning.rules.annotation

// ------------------------------
// Copyright (c) PiggyPiglet 2021
// https://www.piggypiglet.me
// ------------------------------
class AnnotationWrapper private constructor(
    val annotationClass: Class<out Annotation>?,
    val annotationInstance: Annotation?
) {
    constructor(annotation: Class<out Annotation>) : this(annotation, null)
    constructor(annotation: Annotation) : this(null, annotation)

    init {
        require(annotationClass != null || annotationInstance != null) { "Both annotation class & instance are null in an AnnotationWrapper." }
    }


}
