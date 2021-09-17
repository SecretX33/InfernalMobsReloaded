package com.github.secretx33.infernalmobsreloaded.scanning.implementations

import com.github.secretx33.infernalmobsreloaded.scanning.Rules
import com.github.secretx33.infernalmobsreloaded.scanning.exceptions.ScanningException
import com.github.secretx33.infernalmobsreloaded.scanning.framework.Scanner
import com.github.secretx33.infernalmobsreloaded.scanning.rules.RuleUtils
import com.github.secretx33.infernalmobsreloaded.scanning.rules.element.ElementWrapper
import java.io.File
import java.io.FileInputStream
import java.lang.reflect.Field
import java.lang.reflect.Parameter
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

// ------------------------------
// Copyright (c) PiggyPiglet 2021
// https://www.piggypiglet.me
// ------------------------------
class ZISScanner private constructor(private val classes: Set<Class<*>>) : Scanner {

    override fun <T> classes(rules: Rules<T>): List<Class<T>> =
        classes.filter { RuleUtils.match(ElementWrapper(it, it), rules) }
            .map { it as Class<T> }

    override fun parametersInConstructors(rules: Rules<*>): List<Parameter> =
        classes.flatMap { it.declaredConstructors.asSequence() }
            .flatMap { it.parameters.asSequence() }
            .filter { RuleUtils.match(ElementWrapper(it.type, it), rules) }

    override fun fields(rules: Rules<*>): List<Field> =
        classes.flatMap { it.declaredFields.asSequence() }
            .filter { RuleUtils.match(ElementWrapper(it.type, it), rules) }

    companion object {
        fun create(main: Class<*>, pckg: String): ZISScanner {
            val loader = main.classLoader
            val src = File("/${main.protectionDomain.codeSource.location.path.split("!")
                    .toTypedArray()[0].replace("file:/", "")}")
            return create(src, loader, pckg.replace('.', '/'))
        }

        fun create(jar: File, loader: ClassLoader, pckg: String): ZISScanner {
            val classes: MutableSet<Class<*>> = HashSet()
            try {
                ZipInputStream(FileInputStream(jar)).use { zip ->
                    var entry: ZipEntry
                    while (zip.nextEntry.also { entry = it } != null) {
                        val name = entry.name
                        if (!name.endsWith(".class") || !name.startsWith(pckg)) {
                            continue
                        }
                        val clazz = loadClass(loader, name.replace('/', '.').replace(".class", "")) ?: continue
                        classes.add(clazz)
                    }
                }
            } catch (exception: Exception) {
                throw ScanningException(exception)
            }
            return ZISScanner(classes)
        }

        private fun loadClass(loader: ClassLoader, name: String): Class<*>? {
            return try {
                loader.loadClass(name)
            } catch (e: ClassNotFoundException) {
                null
            } catch (e: NoClassDefFoundError) {
                null
            }
        }
    }
}
