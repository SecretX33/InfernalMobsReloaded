package com.github.secretx33.infernalmobsreloaded.scanning

import com.github.secretx33.infernalmobsreloaded.scanning.annotations.Hidden
import com.github.secretx33.infernalmobsreloaded.scanning.rules.annotation.AnnotationUtils
import com.github.secretx33.infernalmobsreloaded.scanning.rules.annotation.AnnotationWrapper
import com.github.secretx33.infernalmobsreloaded.scanning.rules.element.ElementWrapper
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Modifier
import java.util.function.Predicate

class Rules<R>(val rules: List<Predicate<ElementWrapper>>) {

    companion object {
        private val EMPTY: Rules<*> = Rules<Predicate<ElementWrapper>>(emptyList())

        fun empty(): Rules<*> = EMPTY

        fun <R> builder(): Builder<R> {
            return Builder()
        }
    }

    class Builder<R> {

        private val rules = ArrayList<Predicate<ElementWrapper>>()

        fun add(rule: Predicate<ElementWrapper>): Builder<R> {
            rules.add(rule)
            return this
        }

        fun addType(typeRule: Predicate<Class<*>>): Builder<R> {
            rules.add(Predicate { element: ElementWrapper -> typeRule.test(element.type) })
            return this
        }

        fun addAnnotated(annotatedRule: Predicate<AnnotatedElement>): Builder<R> {
            rules.add(Predicate { element: ElementWrapper -> annotatedRule.test(element.element) })
            return this
        }

        fun <T : Annotation?> addAnnotated(annotation: Class<T>, annotationRule: Predicate<T>): Builder<R> {
            rules.add(Predicate { element: ElementWrapper ->
                annotationRule.test(element.element.getAnnotation(annotation))
            })
            return this
        }

        fun <T> typeEquals(type: Class<T>): Builder<T> {
            return addType { element: Class<*> -> element == type } as Builder<T>
        }

        fun <T> typeExtends(type: Class<T>): Builder<out T> {
            return addType { cls: Class<*>? -> type.isAssignableFrom(cls) } as Builder<out T>
        }

        fun <T> typeSupers(type: Class<T>): Builder<in T> {
            return addType { element: Class<*> -> element.isAssignableFrom(type) } as Builder<in T>
        }

        fun disallowImmutableClasses(): Builder<R> {
            return addType { element: Class<*> -> !Modifier.isFinal(element.modifiers) }
        }

        fun disallowMutableClasses(): Builder<R> {
            return addType { element: Class<*> -> !Modifier.isAbstract(element.modifiers) && !element.isInterface }
        }

        fun hasAnnotation(annotation: Class<out Annotation?>): Builder<R> {
            return hasAnnotation(AnnotationWrapper(annotation))
        }

        fun hasAnnotation(annotation: Annotation): Builder<R> {
            return hasAnnotation(AnnotationWrapper(annotation))
        }

        private fun hasAnnotation(annotationWrapper: AnnotationWrapper): Builder<R> {
            return addAnnotated { element: AnnotatedElement ->
                AnnotationUtils.isAnnotationPresent(element, annotationWrapper)
            }
        }

        fun allowHidden(): Builder<R> {
            rules.removeAt(0)
            return this
        }

        fun build(): Rules<R> {
            return Rules(rules)
        }

        init {
            addAnnotated { element: AnnotatedElement ->
                !element.isAnnotationPresent(
                    Hidden::class.java
                )
            }
        }
    }
}
