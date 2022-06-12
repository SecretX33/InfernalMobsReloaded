package com.github.secretx33.infernalmobsreloaded.eventbus

import com.github.secretx33.infernalmobsreloaded.eventbus.internalevent.InternalEvent
import java.util.UUID
import kotlin.reflect.KClass

data class Subscriber<T : InternalEvent>(
    val owner: KClass<out Any>,
    val eventType: KClass<T>,
    val order: Int = 0,
    private val callback: (T) -> Unit,
) : Comparable<Subscriber<*>> {
    val uuid: UUID = UUID.randomUUID()

    @Suppress("UNCHECKED_CAST")
    fun onEvent(event: InternalEvent) = callback(event as T)

    override fun compareTo(other: Subscriber<*>): Int = compareBy<Subscriber<*>> { it.order }
        .thenBy { it.owner.qualifiedName }
        .thenBy { it.uuid }
        .compare(this, other)
}
