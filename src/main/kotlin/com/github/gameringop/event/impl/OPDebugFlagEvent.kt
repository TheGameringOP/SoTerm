package com.github.gameringop.event.impl

import com.github.gameringop.SoTerm
import com.github.gameringop.event.Event

sealed class OPDebugFlagEvent(val flag: String): Event(false) {
    class Add(flag: String): OPDebugFlagEvent(flag)
    class Remove(flag: String): OPDebugFlagEvent(flag)

    override fun cancel() {
        SoTerm.debugFlags.remove(flag)
    }
}