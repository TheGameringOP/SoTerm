package com.github.gameringop.websocket.packets

import com.github.gameringop.utils.ThreadUtils
import com.github.gameringop.utils.dungeons.map.handlers.ScoreCalculation
import com.github.gameringop.websocket.WebSocketPacket

class S2CPacketDungeonMimic: WebSocketPacket("dungeonmimic") {
    override fun handle() {
        ThreadUtils.scheduledTask {
            ScoreCalculation.mimicKilled = true
        }
    }
}