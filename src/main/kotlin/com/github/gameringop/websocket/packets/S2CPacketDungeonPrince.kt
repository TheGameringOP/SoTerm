package com.github.gameringop.websocket.packets

import com.github.gameringop.utils.dungeons.map.handlers.ScoreCalculation
import com.github.gameringop.websocket.WebSocketPacket

object S2CPacketDungeonPrince: WebSocketPacket {
    override fun handle() = ScoreCalculation::princeKilled.set(true)
}