package com.github.gameringop.features.impl.visual

import com.github.gameringop.features.Feature
import com.github.gameringop.ui.clickgui.components.getValue
import com.github.gameringop.ui.clickgui.components.impl.ColorSetting
import com.github.gameringop.ui.clickgui.components.provideDelegate
import com.github.gameringop.utils.render.Render2D
import com.github.gameringop.utils.render.Render2D.height
import com.github.gameringop.utils.render.Render2D.width
import java.awt.Color

object FpsDisplay: Feature("Displays the game's FPS on screen.") {
    private val color by ColorSetting("Color", Color(230, 114, 230), false)

    private val fpsDisplayHud = hudElement("FpsDisplay") { ctx, _ ->
        val text = "${mc.fps} fps"
        Render2D.drawString(ctx, text, 0, 0, color.value)
        return@hudElement text.width().toFloat() to text.height().toFloat()
    }
}

