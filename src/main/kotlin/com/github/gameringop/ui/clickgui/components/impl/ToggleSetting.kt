package com.github.gameringop.ui.clickgui.components.impl

import com.github.gameringop.config.Savable
import com.github.gameringop.ui.clickgui.components.Setting
import com.github.gameringop.ui.clickgui.components.Style
import com.github.gameringop.ui.utils.Animation
import com.github.gameringop.utils.ColorUtils.lerp
import com.github.gameringop.utils.render.Render2D
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import net.minecraft.client.gui.GuiGraphics
import java.awt.Color

class ToggleSetting(name: String, value: Boolean = false): Setting<Boolean>(name, value), Savable {
    private val toggleAnim = Animation(200, if (value) 1f else 0f)
    private val hoverAnim = Animation(200, 0f)

    override fun draw(ctx: GuiGraphics, mouseX: Int, mouseY: Int) {
        val isHovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height
        toggleAnim.update(if (value) 1f else 0f)
        hoverAnim.update(if (isHovered) 1f else 0f)

        Style.drawBackground(ctx, x, y, width, height)
        Style.drawHoverBar(ctx, x, y, height, hoverAnim.value)
        Style.drawNudgedText(ctx, name, x + 8f, y + 6f, hoverAnim.value)

        val sw = 18f;
        val sh = 6f
        val sx = x + width - sw - 10f;
        val sy = y + (height / 2f) - (sh / 2f)
        Render2D.drawRect(ctx, sx, sy, sw, sh, Color(40, 40, 40, 120).lerp(Style.accentColorTrans, toggleAnim.value))
        val tx = sx + (toggleAnim.value * (sw - 8f))
        Render2D.drawRect(ctx, tx, sy - 1f, 8f, 8f, Color(160, 160, 160).lerp(Style.accentColor, toggleAnim.value))
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button == 0 && mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
            value = ! value
            Style.playClickSound(1f)
            return true
        }
        return false
    }

    override fun write(): JsonElement {
        return JsonPrimitive(value)
    }

    override fun read(element: JsonElement?) {
        element?.asBoolean?.let { value = it }
    }
}