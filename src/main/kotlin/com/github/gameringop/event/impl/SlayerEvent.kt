package com.github.gameringop.event.impl

import com.github.gameringop.event.Event
import net.minecraft.world.entity.Entity

sealed class SlayerEvent {
    sealed class Boss {
        data class Spawn(val entity: Entity, val slayerInfo: SlayerInfo) : Event()
        data class Death(val entity: Entity, val slayerInfo: SlayerInfo) : Event()
    }

    sealed class Reset : Event() {
        object Any : Reset()
    }
}