package com.github.gameringop.event.impl

import com.github.gameringop.event.Event

sealed class WebSocketEvent: Event(cancelable = false) {
    class Payload(val message: String): WebSocketEvent()
    object Connect: WebSocketEvent()
    object Disconnect: WebSocketEvent()
}