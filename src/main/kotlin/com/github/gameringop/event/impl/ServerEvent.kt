package com.github.gameringop.event.impl

import com.github.gameringop.event.Event

abstract class ServerEvent: Event() {
    object Connect: ServerEvent()
    object Disconnect: ServerEvent()
}

