package com.github.gameringop.websocket.packets

import com.github.gameringop.utils.dungeons.map.DungeonInfo
import com.github.gameringop.utils.dungeons.map.core.Room
import com.github.gameringop.utils.dungeons.map.core.Unknown
import com.github.gameringop.utils.dungeons.map.handlers.DungeonScanner
import com.github.gameringop.utils.dungeons.map.utils.ScanUtils
import com.github.gameringop.websocket.WebSocketPacket

class S2CPacketDungeonRoom(
    val name: String,
    val x: Int, val z: Int,
    val col: Int, val row: Int,
    val core: Int, val isSeparator: Boolean
): WebSocketPacket("dungeonroom") {
    override fun handle() {
        if (DungeonScanner.hasScanned) return
        val idx = row * 11 + col
        val tile = DungeonInfo.dungeonList[idx]
        if (tile is Unknown || (tile as? Room)?.data?.name == "Unknown") {
            val data = ScanUtils.getRoomData(name) ?: return
            DungeonInfo.dungeonList[idx] = Room(x, z, data).also {
                it.isSeparator = isSeparator
                it.core = core
                it.addToUnique(row, col)
            }
        }
    }
}