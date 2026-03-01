package com.github.gameringop.ui.utils.componnents

import com.github.gameringop.SoTerm.mc
import com.github.gameringop.ui.clickgui.components.Style
import com.github.gameringop.utils.render.Render2D
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.EditBox
import net.minecraft.network.chat.Component
import java.awt.Color

class UISearchBox(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    message: Component
): EditBox(mc.font, x, y, width, height, message) {

    init {
        this.isBordered = false
        this.setTextColor(Color.WHITE.rgb)
    }

    override fun renderWidget(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        if (! this.isVisible) return

        Render2D.drawRect(guiGraphics, x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat(), Color(15, 15, 15, 200))

        val borderColor = if (this.isFocused) Style.accentColor else Color(255, 255, 255, 30)
        Render2D.drawRect(guiGraphics, x.toFloat(), (y + height - 2).toFloat(), width.toFloat(), 2f, borderColor)

        val pose = guiGraphics.pose()
        pose.pushMatrix()
        pose.translate(5f, (height / 2f) - mc.font.lineHeight / 2)
        super.renderWidget(guiGraphics, mouseX, mouseY, partialTick)
        pose.popMatrix()
    }

    override fun setFocused(focused: Boolean) {
        super.setFocused(focused)
        if (focused) {
            Style.playClickSound(1.1f)
        }
    }
}