package com.github.secretx33.infernalmobsreloaded.scanning.framework

import com.github.secretx33.infernalmobsreloaded.scanning.Rules
import java.lang.reflect.Field
import java.lang.reflect.Parameter

interface Scanner {

    fun <T> classes(rules: Rules<T>): List<Class<T>>

    fun parametersInConstructors(rules: Rules<*>): List<Parameter>

    fun fields(rules: Rules<*>): List<Field>
}
