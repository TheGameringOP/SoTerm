package com.github.gameringop.event.impl

import com.github.gameringop.event.Event

class MouseClickEvent(val button: Int, val action: Int, val modifiers: Int): Event(true)