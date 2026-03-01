package com.github.gameringop.event.impl

import com.github.gameringop.event.Event
import net.minecraft.network.chat.Component

class BossBarUpdateEvent(val name: Component, val progress: Float): Event(false)