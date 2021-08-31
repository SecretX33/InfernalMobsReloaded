package utils

import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

fun Any.invoke(name: String, vararg args: Any?): Any? =
    this::class
        .declaredFunctions
        .first { it.name == name }
        .apply { isAccessible = true }
        .call(this, *args)

@Suppress("UNCHECKED_CAST")
inline fun <reified T : Any, R> T.getPrivateProperty(name: String): R =
    T::class
        .memberProperties
        .first { it.name == name }
        .apply { isAccessible = true }
        .get(this) as R

fun Any.setPrivateProperty(name: String, args: Any) =
    this::class.java
        .run { getDeclaredField(name).apply { isAccessible = true } }
        .set(this, args)

inline fun <T : Listener, reified R : Event> T.callEventMethods(event: R) {
    this::class.java.declaredMethods
        .filter { it.parameterCount == 1
                && it.parameterTypes[0].isAssignableFrom(R::class.java)
                && it.returnType == Void.TYPE
                && it.declaredAnnotations.any { annotation -> annotation is EventHandler } }
        .forEach {
            it.isAccessible = true
            it.invoke(this, event)
        }
}
