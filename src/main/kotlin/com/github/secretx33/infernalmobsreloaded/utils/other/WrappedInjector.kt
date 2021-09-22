package com.github.secretx33.infernalmobsreloaded.utils.other

import com.google.inject.Binding
import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.Key
import com.google.inject.MembersInjector
import com.google.inject.Module
import com.google.inject.Provider
import com.google.inject.Scope
import com.google.inject.Stage
import com.google.inject.TypeLiteral
import com.google.inject.spi.Element
import com.google.inject.spi.InjectionPoint
import com.google.inject.spi.TypeConverterBinding

/**
 * Wrapper to allow Guice to print errors when running on Bukkit.
 */
class WrappedInjector(private val composition: Injector) : Injector {

    override fun injectMembers(instance: Any) {
        composition.injectMembers(instance)
    }

    override fun <T> getMembersInjector(typeLiteral: TypeLiteral<T>): MembersInjector<T> {
        return composition.getMembersInjector(typeLiteral)
    }

    inline fun <reified T> getMembersInjector(): MembersInjector<T> =
        getMembersInjector(T::class.java)

    override fun <T> getMembersInjector(type: Class<T>): MembersInjector<T> {
        return composition.getMembersInjector(type)
    }

    override fun getBindings(): Map<Key<*>, Binding<*>> {
        return composition.bindings
    }

    override fun getAllBindings(): Map<Key<*>, Binding<*>> {
        return composition.allBindings
    }

    override fun <T> getBinding(key: Key<T>): Binding<T> {
        return composition.getBinding(key)
    }

    inline fun <reified T> getBinding(): Binding<T> = getBinding(T::class.java)

    override fun <T> getBinding(type: Class<T>): Binding<T> {
        return composition.getBinding(type)
    }

    override fun <T> getExistingBinding(key: Key<T>): Binding<T> {
        return composition.getExistingBinding(key)
    }

    override fun <T> findBindingsByType(type: TypeLiteral<T>): List<Binding<T>> {
        return composition.findBindingsByType(type)
    }

    override fun <T> getProvider(key: Key<T>): Provider<T> {
        return composition.getProvider(key)
    }

    inline fun <reified T> getProvider(): Provider<T> = getProvider(T::class.java)

    override fun <T> getProvider(type: Class<T>): Provider<T> {
        return composition.getProvider(type)
    }

    override fun <T> getInstance(key: Key<T>): T {
        return try {
            composition.getInstance(key)
        } catch (e: Exception) {
            throw RuntimeException("Error with Guice while trying to get instance of a class", e)
        }
    }

    inline fun <reified T> getInstance(): T = getInstance(T::class.java)

    override fun <T> getInstance(type: Class<T>): T {
        return try {
            composition.getInstance(type)
        } catch (e: Exception) {
            throw RuntimeException("Could not get an instance of ${type.name}", e)
        }
    }

    override fun getParent(): Injector {
        return composition.parent
    }

    override fun createChildInjector(modules: Iterable<Module>): Injector = try {
        WrappedInjector(composition.createChildInjector(modules))
    } catch(e: Exception) {
        throw RuntimeException(e)
    }

    override fun createChildInjector(vararg modules: Module): Injector = try {
        WrappedInjector(composition.createChildInjector(*modules))
    } catch(e: Exception) {
        throw RuntimeException(e)
    }

    override fun getScopeBindings(): Map<Class<out Annotation>, Scope> {
        return composition.scopeBindings
    }

    override fun getTypeConverterBindings(): Set<TypeConverterBinding> {
        return composition.typeConverterBindings
    }

    override fun getElements(): List<Element> {
        return composition.elements
    }

    override fun getAllMembersInjectorInjectionPoints(): Map<TypeLiteral<*>, List<InjectionPoint>> {
        return composition.allMembersInjectorInjectionPoints
    }

    companion object {
        fun getGuiceInjector(vararg modules: Module): WrappedInjector = try {
            WrappedInjector(Guice.createInjector(Stage.PRODUCTION, *modules));
        } catch(e: Exception) {
            throw RuntimeException("Error creating new instance of Guice Injector!", e);
        }
    }
}
