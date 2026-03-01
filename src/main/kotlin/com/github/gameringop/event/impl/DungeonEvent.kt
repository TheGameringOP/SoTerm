package com.github.gameringop.event.impl

import com.github.gameringop.event.Event
import com.github.gameringop.utils.dungeons.DungeonPlayer
import com.github.gameringop.utils.dungeons.enums.SecretType
import com.github.gameringop.utils.dungeons.map.core.RoomState
import com.github.gameringop.utils.dungeons.map.core.UniqueRoom
import net.minecraft.core.BlockPos

abstract class DungeonEvent: Event(false) {
    abstract class RoomEvent(val room: UniqueRoom): DungeonEvent() {
        class onEnter(room: UniqueRoom): RoomEvent(room)
        class onExit(room: UniqueRoom): RoomEvent(room)

        class onStateChange(room: UniqueRoom, val oldState: RoomState, val newState: RoomState, val roomPlayers: List<DungeonPlayer>): RoomEvent(room)
    }

    class SecretEvent(val type: SecretType, val pos: BlockPos): DungeonEvent()

    class PlayerDeathEvent(val name: String, val reason: String): DungeonEvent()

    class Score(val score: Int): DungeonEvent()
    object BossEnterEvent: DungeonEvent()
    object RunStatedEvent: DungeonEvent()
    object RunEndedEvent: DungeonEvent()
}