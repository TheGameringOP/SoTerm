package com.github.gameringop.websocket.packets

import com.github.gameringop.utils.dungeons.map.handlers.ScoreCalculation
import com.github.gameringop.websocket.WebSocketPacket

class S2CPacketDungeonScore: WebSocketPacket("dungeonprince") {
    override fun handle() {
        ScoreCalculation.princeKilled = true
    }
}