package com.github.gameringop.event.impl

import com.github.gameringop.event.Event
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen

abstract class ScreenEvent(val screen: Screen): Event(cancelable = true) {
    class PreRender(screen: Screen, val context: GuiGraphics, val mouseX: Int, val mouseY: Int): ScreenEvent(screen)
    class PostRender(screen: Screen, val context: GuiGraphics, val mouseX: Int, val mouseY: Int): ScreenEvent(screen)
}