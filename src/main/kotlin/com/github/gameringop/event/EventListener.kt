package com.github.gameringop.event


data class EventListener<T: Event>(
    val eventClass: Class<out Event>,
    val priority: EventPriority = EventPriority.NORMAL,
    val callback: EventBus.EventContext<T>.() -> Unit
) {
    fun isRegistered() = EventBus.listeners[eventClass]?.contains(this) == true

    fun unregister(): EventListener<T> {
        EventBus.unregister(this)
        return this
    }

    fun register(): EventListener<T> {
        EventBus.register(this)
        return this
    }

}