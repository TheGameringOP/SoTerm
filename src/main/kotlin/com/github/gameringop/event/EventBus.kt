@file:Suppress("UNCHECKED_CAST")

package com.github.gameringop.event

import com.github.gameringop.SoTerm
import com.github.gameringop.utils.ChatUtils
import java.util.concurrent.ConcurrentHashMap

object EventBus {
    class EventContext<T: Event>(val event: T, val listener: EventListener<T>)

    val listeners = ConcurrentHashMap<Class<out Event>, List<EventListener<*>>>()

    @JvmStatic
    fun post(event: Event): Boolean {
        val list = listeners[event.javaClass] ?: return event.isCanceled

        for (i in list.indices) {
            val handler = list[i]
            try {
                val callback = handler.callback as EventContext<Event>.() -> Unit
                callback.invoke(EventContext(event, handler as EventListener<Event>))
            } catch (e: Exception) {
                SoTerm.logger.error("EventBus Error in ${event.javaClass.name}", e)
                ChatUtils.clickableChat("EventBus Error: class ${event.javaClass.name}. message: ${e.message}", true, copy = e.stackTrace.joinToString("\n"))
            }
        }

        return if (event.cancelable) event.isCanceled else false
    }

    @JvmStatic
    inline fun <reified T: Event> register(
        priority: EventPriority = EventPriority.NORMAL,
        noinline block: EventContext<T>.() -> Unit
    ): EventListener<T> {
        val eventListener = EventListener(T::class.java, priority, block)

        synchronized(listeners) {
            val oldList = listeners[T::class.java] ?: emptyList()
            val newList = (oldList + eventListener).sortedBy { it.priority.ordinal }
            listeners[T::class.java] = newList
        }

        return eventListener
    }

    fun unregister(listener: EventListener<*>) {
        synchronized(listeners) {
            val oldList = listeners[listener.eventClass] ?: return
            val newList = oldList.filter { it !== listener }

            if (newList.isEmpty()) listeners.remove(listener.eventClass)
            else listeners[listener.eventClass] = newList
        }
    }

    fun register(listener: EventListener<*>) {
        synchronized(listeners) {
            val oldList = listeners[listener.eventClass] ?: emptyList()
            if (oldList.contains(listener)) return
            val newList = (oldList + listener).sortedBy { it.priority.ordinal }
            listeners[listener.eventClass] = newList
        }
    }
}