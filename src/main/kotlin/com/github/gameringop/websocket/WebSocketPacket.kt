package com.github.gameringop.websocket

abstract class WebSocketPacket(val type: String) {
    abstract fun handle()
}