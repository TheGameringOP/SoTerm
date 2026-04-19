package com.github.gameringop.websocket.packets

import com.github.gameringop.features.impl.floor7.dragons.WitherDragonEnum
import com.github.gameringop.features.impl.floor7.dragons.WitherDragonState
import com.github.gameringop.utils.dungeons.DungeonListener
import com.github.gameringop.websocket.WebSocketPacket

class S2CPacketM7Dragon(val event: DragonEvent, val dragon: WitherDragonEnum): WebSocketPacket("m7dragon") {
    override fun handle() {
        WitherDragonEnum.valueOf(dragon.name).let {
            when (event) {
                DragonEvent.SPAWN -> {
                    if (it.state == WitherDragonState.ALIVE) return@let
                    it.state = WitherDragonState.ALIVE
                    it.timeToSpawn = 100
                    it.spawnedTime = DungeonListener.currentTime
                    it.sprayedTime = null
                    it.arrowsHit = 0
                }

                DragonEvent.DEATH -> it.setDead()
            }
        }

    }

    enum class DragonEvent { SPAWN, DEATH }
}