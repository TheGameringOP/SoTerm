package com.github.gameringop.event.impl

import com.github.gameringop.event.Event

abstract class TickEvent: Event(false) {
    object Start: TickEvent()
    object End: TickEvent()

    object Server: TickEvent()
}