package com.github.secretx33.infernalmobsreloaded.utils

import org.koin.core.Koin
import org.koin.core.KoinApplication
import org.koin.core.component.KoinApiExtension
import org.koin.core.component.KoinComponent
import org.koin.core.context.KoinContext
import org.koin.core.error.KoinAppAlreadyStartedException
import org.koin.core.module.Module
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.qualifier.Qualifier
import org.koin.dsl.KoinAppDeclaration

@KoinApiExtension
interface CustomKoinComponent : KoinComponent {
    override fun getKoin(): Koin = CustomScope.get()
}

private object CustomScope : KoinContext {

    private var _koin: Koin? = null

    override fun get(): Koin = _koin ?: error("Custom KoinApplication has not been started")

    override fun getOrNull(): Koin? = _koin

    override fun register(koinApplication: KoinApplication) {
        if (_koin != null) {
            throw KoinAppAlreadyStartedException("A Custom Koin Application has already been started")
        }
        _koin = koinApplication.koin
    }

    override fun stop() = synchronized(this) {
        _koin?.close()
        _koin = null
    }

    fun startKoin(koinContext: KoinContext = CustomScope, koinApplication: KoinApplication): KoinApplication = synchronized(this) {
        koinContext.register(koinApplication)
        koinApplication.createEagerInstances()
        return koinApplication
    }

    /**
     * Start a custom Koin Application as StandAlone
     */
    fun startKoin(koinContext: KoinContext = CustomScope, appDeclaration: KoinAppDeclaration): KoinApplication = synchronized(this) {
        val koinApplication = KoinApplication.init()
        koinContext.register(koinApplication)
        appDeclaration(koinApplication)
        koinApplication.createEagerInstances()
        return koinApplication
    }

    /**
     * load Koin module in custom Koin context
     */
    fun loadKoinModules(module: Module) = synchronized(this) {
        get().loadModules(listOf(module))
    }

    /**
     * load Koin modules in custom Koin context
     */
    fun loadKoinModules(modules: List<Module>) = synchronized(this) {
        get().loadModules(modules)
    }

    /**
     * unload Koin modules from custom Koin context
     */
    fun unloadKoinModules(module: Module) = synchronized(this) {
        get().unloadModules(listOf(module))
    }

    /**
     * unload Koin modules from custom Koin context
     */
    fun unloadKoinModules(modules: List<Module>) = synchronized(this) {
        get().unloadModules(modules)
    }
}

/**
 * Start a Koin Application as StandAlone
 */
fun startKoin(koinContext: KoinContext = CustomScope,
              koinApplication: KoinApplication): KoinApplication = CustomScope.startKoin(koinContext, koinApplication)

/**
 * Start a Koin Application as StandAlone
 */
fun startKoin(koinContext: KoinContext = CustomScope,
              appDeclaration: KoinAppDeclaration): KoinApplication = CustomScope.startKoin(koinContext, appDeclaration)

/**
 * Stop current StandAlone Koin application
 */
fun stopKoin() = CustomScope.stop()

/**
 * load Koin module in global Koin context
 */
fun loadKoinModules(module: Module) = CustomScope.loadKoinModules(module)

/**
 * load Koin modules in global Koin context
 */
fun loadKoinModules(modules: List<Module>) = CustomScope.loadKoinModules(modules)

/**
 * unload Koin modules from global Koin context
 */
fun unloadKoinModules(module: Module) = CustomScope.unloadKoinModules(module)

/**
 * unload Koin modules from global Koin context
 */
fun unloadKoinModules(modules: List<Module>) = CustomScope.unloadKoinModules(modules)

@KoinApiExtension
inline fun <reified T : Any> CustomKoinComponent.inject(
    qualifier: Qualifier? = null,
    mode: LazyThreadSafetyMode = LazyThreadSafetyMode.SYNCHRONIZED,
    noinline parameters: ParametersDefinition? = null
): Lazy<T> =
    lazy(mode) { getKoin().get(qualifier, parameters) }

@KoinApiExtension
inline fun <reified T : Any> CustomKoinComponent.get(qualifier: Qualifier? = null, noinline parameters: ParametersDefinition? = null): T = getKoin().get(qualifier = qualifier, parameters = parameters)
