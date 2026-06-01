package com.github.gameringop.websocket.packets

import com.github.gameringop.utils.dungeons.map.DungeonInfo
import com.github.gameringop.websocket.WebSocketPacket

class S2CPacketRoomSecrets(val room: String, val secrets: Int): WebSocketPacket {
    override fun handle() {
        DungeonInfo.uniqueRooms[room]?.let {
            if (it.foundSecrets < secrets) it.foundSecrets = secrets
        }
    }
}