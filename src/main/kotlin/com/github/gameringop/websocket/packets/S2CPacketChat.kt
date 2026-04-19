package com.github.gameringop.websocket.packets

import com.github.gameringop.utils.ChatUtils
import com.github.gameringop.websocket.WebSocketPacket

class S2CPacketChat(val message: String): WebSocketPacket("chat") {
    override fun handle() = ChatUtils.chat("§b[WS]§r $message")
}