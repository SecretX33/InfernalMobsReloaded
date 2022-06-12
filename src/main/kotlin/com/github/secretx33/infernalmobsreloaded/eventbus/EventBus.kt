package com.github.secretx33.infernalmobsreloaded.eventbus

import com.github.secretx33.infernalmobsreloaded.eventbus.internalevent.InternalEvent
import com.github.secretx33.infernalmobsreloaded.eventbus.internalevent.PluginUnload
import org.bukkit.Bukkit
import toothpick.InjectConstructor
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Logger
import javax.inject.Singleton
import kotlin.reflect.KClass

/**
 * `EventBus` for internal plugin events.
 *
 * **Events are always dispatched from Bukkit's main thread.**
 *
 * Do not depend on this class because it is subject to change at any given time.
 */
@Singleton
@InjectConstructor
class EventBus(private val log: Logger) {

    private val isClosed = AtomicBoolean(false)
    private val subscribers = ConcurrentHashMap<Any, MutableSet<Subscriber<*>>>()

    init {
        // Register an auto unsubscription for all events when the plugin is unloaded
        subscribe<PluginUnload>(this, order = Integer.MAX_VALUE) {
            isClosed.set(true)
            subscribers.clear()
        }
    }

    inline fun <reified T : InternalEvent> subscribe(
        owner: Any,
        order: Int = 0,
        noinline callback: (T) -> Unit,
    ): Unit = subscribe(owner, T::class, order, callback)

    /**
     * Register a callback for an event.
     */
    fun <T : InternalEvent> subscribe(
        owner: Any,
        eventType: KClass<T>,
        order: Int,
        callback: (T) -> Unit,
    ) {
        require(!isClosed.get()) { "EventBus is closed, thus it no longer accepts any subscribers" }

        val thisSubscribes = subscribers.getOrPut(owner) { ConcurrentHashMap.newKeySet() }
        val newSubscription = Subscriber(
            owner = owner::class,
            eventType = eventType,
            order = order,
            callback = callback
        )
        thisSubscribes += newSubscription
    }

    fun post(event: InternalEvent) {
        require(Bukkit.getServer().isPrimaryThread) { "EventBus.post(event) must be called from Bukkit main thread" }

        subscribers.flatMap { it.value }
            .filter { it.eventType.isInstance(event) }
            .sorted()
            .forEach {
                try {
                    it.onEvent(event)
                } catch (e: Exception) {
                    log.severe("Exception while dispatching event ${event::class.simpleName} to subscriber ${it.ownerName} (${it.order}). Message: ${e.message}\n${e.stackTraceToString()}")
                }
            }
    }
}
