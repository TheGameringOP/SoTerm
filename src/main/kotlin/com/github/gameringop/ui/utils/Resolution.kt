package com.github.gameringop.ui.utils

import com.github.gameringop.SoTerm.mc
import com.github.gameringop.utils.NumbersUtils.div
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics

object Resolution {
    private const val REFERENCE_HEIGHT = 540f

    var scale = 1f
        private set

    var width = 960f
        private set

    var height = 540f
        private set

    fun refresh() {
        val window = Minecraft.getInstance().window
        val guiWidth = window.guiScaledWidth.toFloat()
        val guiHeight = window.guiScaledHeight.toFloat()

        scale = guiHeight / REFERENCE_HEIGHT

        height = REFERENCE_HEIGHT
        width = guiWidth / scale
    }

    fun push(ctx: GuiGraphics) {
        ctx.pose().pushMatrix()
        ctx.pose().scale(scale, scale)
    }

    fun pop(ctx: GuiGraphics) {
        ctx.pose().popMatrix()
    }

    fun getMouseX(vanillaX: Number) = (vanillaX / scale).toInt()
    fun getMouseY(vanillaY: Number) = (vanillaY / scale).toInt()

    fun getMouseX() = (mc.mouseHandler.xpos() / mc.window.screenWidth.toDouble() * width).toInt()
    fun getMouseY() = (mc.mouseHandler.ypos() / mc.window.screenHeight.toDouble() * height).toInt()
}