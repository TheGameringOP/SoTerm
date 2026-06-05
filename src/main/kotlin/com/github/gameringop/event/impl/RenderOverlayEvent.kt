package com.github.gameringop.event.impl

import com.github.gameringop.event.Event
import net.minecraft.client.DeltaTracker
import net.minecraft.client.gui.GuiGraphicsExtractor

class RenderOverlayEvent(val context: GuiGraphicsExtractor, val deltaTracker: DeltaTracker): Event(cancelable = false)