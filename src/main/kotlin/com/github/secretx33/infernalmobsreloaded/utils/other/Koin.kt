package com.github.secretx33.infernalmobsreloaded.utils.other

import org.koin.core.Koin
import org.koin.core.KoinApplication
import org.koin.core.component.KoinComponent
import org.koin.core.context.KoinContext
import org.koin.core.error.KoinAppAlreadyStartedException
import org.koin.core.module.Module
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.qualifier.Qualifier
import org.koin.dsl.KoinAppDeclaration

interface CustomKoinComponent : KoinComponent {
    override fun getKoin(): Koin = CustomScope.get()
}

private object CustomScope : KoinContext {

    private var _koin: Koin? = null
    private var _koinApplication: KoinApplication? = null

    override fun get(): Koin = _koin ?: error("KoinApplication has not been started")

    override fun getOrNull(): Koin? = _koin

    fun getKoinApplicationOrNull(): KoinApplication? = _koinApplication

    private fun register(koinApplication: KoinApplication) {
        if (_koin != null) {
            throw KoinAppAlreadyStartedException("A Koin Application has already been started")
        }
        _koinApplication = koinApplication
        _koin = koinApplication.koin
    }

    override fun stopKoin() = synchronized(this) {
        _koin?.close()
        _koin = null
    }


    override fun startKoin(koinApplication: KoinApplication): KoinApplication = synchronized(this) {
        register(koinApplication)
        koinApplication.koin.createEagerInstances()
        return koinApplication
    }

    override fun startKoin(appDeclaration: KoinAppDeclaration): KoinApplication = synchronized(this) {
        val koinApplication = KoinApplication.init()
        register(koinApplication)
        appDeclaration(koinApplication)
        koinApplication.koin.createEagerInstances()
        return koinApplication
    }


    override fun loadKoinModules(module: Module) = synchronized(this) {
        get().loadModules(listOf(module))
    }

    override fun loadKoinModules(modules: List<Module>) = synchronized(this) {
        get().loadModules(modules)
    }

    override fun unloadKoinModules(module: Module) = synchronized(this) {
        get().unloadModules(listOf(module))
    }

    override fun unloadKoinModules(modules: List<Module>) = synchronized(this) {
        get().unloadModules(modules)
    }
}

/**
 * Start a Koin Application as StandAlone
 */
fun startKoin(koinApplication: KoinApplication): KoinApplication = CustomScope.startKoin(koinApplication)

/**
 * Start a Koin Application as StandAlone
 */
fun startKoin(appDeclaration: KoinAppDeclaration): KoinApplication = CustomScope.startKoin(appDeclaration)

/**
 * Stop current StandAlone Koin application
 */
fun stopKoin() = CustomScope.stopKoin()

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

inline fun <reified T : Any> CustomKoinComponent.inject(
    qualifier: Qualifier? = null,
    mode: LazyThreadSafetyMode = LazyThreadSafetyMode.SYNCHRONIZED,
    noinline parameters: ParametersDefinition? = null
): Lazy<T> =
    lazy(mode) { getKoin().get(qualifier, parameters) }

inline fun <reified T : Any> CustomKoinComponent.injectOrNull(
    qualifier: Qualifier? = null,
    mode: LazyThreadSafetyMode = LazyThreadSafetyMode.SYNCHRONIZED,
    noinline parameters: ParametersDefinition? = null
): Lazy<T?> =
    lazy(mode) { getKoin().getOrNull(qualifier, parameters) }

inline fun <reified T : Any> CustomKoinComponent.get(
    qualifier: Qualifier? = null,
    noinline parameters: ParametersDefinition? = null
): T = getKoin().get(T::class, qualifier = qualifier, parameters = parameters)

inline fun <reified T : Any> CustomKoinComponent.getOrNull(
    qualifier: Qualifier? = null,
    noinline parameters: ParametersDefinition? = null
): T? = getKoin().getOrNull(T::class, qualifier, parameters)
