package com.github.gameringop.features.impl.visual

import com.github.gameringop.features.Feature
import com.github.gameringop.ui.clickgui.components.getValue
import com.github.gameringop.ui.clickgui.components.impl.ColorSetting
import com.github.gameringop.ui.clickgui.components.impl.ToggleSetting
import com.github.gameringop.ui.clickgui.components.provideDelegate
import com.github.gameringop.utils.render.Render2D
import com.github.gameringop.utils.render.Render2D.height
import com.github.gameringop.utils.render.Render2D.width
import java.awt.Color
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object ClockDisplay: Feature("Displays the system time on screen.") {
    private val seconds by ToggleSetting("Show Seconds", true)
    private val color by ColorSetting("Color", Color(255, 134, 0), false)

    private val clockDisplayHud = hudElement("ClockDisplay") { ctx, _ ->
        val text = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm${if (seconds.value) ":ss" else ""}"))
        Render2D.drawString(ctx, text, 0, 0, color.value)
        return@hudElement text.width().toFloat() to text.height().toFloat()
    }
}