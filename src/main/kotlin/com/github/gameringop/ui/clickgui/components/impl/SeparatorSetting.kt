package com.github.gameringop.ui.clickgui.components.impl

import com.github.gameringop.ui.clickgui.components.Setting
import com.github.gameringop.utils.render.Render2D
import net.minecraft.client.gui.GuiGraphics
import java.awt.Color

class SeparatorSetting(name: String = ""): Setting<Unit>(name, Unit) {
    override val height = 10
    override fun draw(ctx: GuiGraphics, mouseX: Int, mouseY: Int) {
        Render2D.drawRect(ctx, x + 10f, y + 5f, width - 20f, 0.5f, Color(255, 255, 255, 30))
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int) = false
}