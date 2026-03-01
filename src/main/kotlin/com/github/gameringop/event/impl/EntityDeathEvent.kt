package com.github.gameringop.event.impl

import com.github.gameringop.event.Event
import net.minecraft.world.entity.Entity

class EntityDeathEvent(val entity: Entity): Event(false)
