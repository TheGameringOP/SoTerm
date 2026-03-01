package com.github.gameringop.utils.dungeons.map.core

import com.github.gameringop.features.impl.dungeon.map.MapConfig
import java.awt.Color

class Door(override val x: Int, override val z: Int, var type: DoorType): Tile {
    var opened = false
    override var state: RoomState = RoomState.UNDISCOVERED
    override val color: Color
        get() {
            if (state == RoomState.UNOPENED) return MapConfig.colorUnopenedDoor.value
            val color = when (this.type) {
                DoorType.BLOOD -> MapConfig.colorBloodDoor
                DoorType.ENTRANCE -> MapConfig.colorEntranceDoor
                DoorType.WITHER -> if (opened && state != RoomState.UNDISCOVERED) MapConfig.colorOpenWitherDoor
                else MapConfig.colorWitherDoor

                else -> MapConfig.colorRoomDoor
            }.value

            return if (state == RoomState.UNDISCOVERED) color.darker().darker() else color
        }
}