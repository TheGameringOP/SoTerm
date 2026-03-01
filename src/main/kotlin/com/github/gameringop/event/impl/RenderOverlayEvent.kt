package com.github.gameringop.event.impl

import com.github.gameringop.event.Event
import net.minecraft.client.DeltaTracker
import net.minecraft.client.gui.GuiGraphics

class RenderOverlayEvent(val context: GuiGraphics, val deltaTracker: DeltaTracker): Event(cancelable = false)