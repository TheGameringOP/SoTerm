package com.github.gameringop.features.impl.visual

import com.github.gameringop.features.Feature
import com.github.gameringop.ui.clickgui.components.getValue
import com.github.gameringop.ui.clickgui.components.impl.ColorSetting
import com.github.gameringop.ui.clickgui.components.provideDelegate
import com.github.gameringop.utils.NumbersUtils.toFixed
import com.github.gameringop.utils.ServerUtils
import com.github.gameringop.utils.render.Render2D
import com.github.gameringop.utils.render.Render2D.height
import com.github.gameringop.utils.render.Render2D.width
import java.awt.Color


object TpsDisplay: Feature("Displays the Server's Ticks Per Second (TPS) on screen.") {
    private val color by ColorSetting("Color", Color(0, 114, 255), false)

    private val tpsDisplayHud = hudElement("TpsDisplay") { ctx, example ->
        val text = "TPS: &f${if (example) 20 else ServerUtils.tps.toFixed(1)}"
        Render2D.drawString(ctx, text, 0, 0, color.value)
        return@hudElement text.width().toFloat() to text.height().toFloat()
    }
}

