package com.github.gameringop.event

abstract class Event(val cancelable: Boolean = false) {
    var isCanceled = false
        set(value) {
            if (! cancelable) return
            field = value
        }

    open fun cancel() {
        isCanceled = true
    }
}