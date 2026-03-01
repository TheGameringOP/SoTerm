package com.github.gameringop.utils.dungeons.map.core

import java.awt.Color

interface Tile {
    val x: Int
    val z: Int
    var state: RoomState
    val color: Color
}