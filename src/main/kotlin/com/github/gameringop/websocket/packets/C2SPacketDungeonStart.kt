package com.github.gameringop.websocket.packets

import com.github.gameringop.websocket.WebSocketPacket

class C2SPacketDungeonStart(
    val serverId: String,
    val floor: String,
    val members: List<String>,
    val entrance: Pair<Int, Int>
): WebSocketPacket