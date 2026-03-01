package com.github.gameringop.features.impl.visual

import com.github.gameringop.features.Feature
import com.github.gameringop.ui.clickgui.components.getValue
import com.github.gameringop.ui.clickgui.components.impl.SliderSetting
import com.github.gameringop.ui.clickgui.components.impl.ToggleSetting
import com.github.gameringop.ui.clickgui.components.provideDelegate
import com.github.gameringop.ui.clickgui.components.withDescription
import com.github.gameringop.utils.ColorUtils.withAlpha
import net.minecraft.client.gui.GuiGraphics
import java.awt.Color

/**
 * @see com.github.gameringop.mixin.MixinGui.onRenderHudPre
 * @see com.github.gameringop.mixin.MixinGui.onRenderHudPost
 */
object DarkMode: Feature("Darkens the screen") {
    private val opacity by SliderSetting("Opacity", 25, 1, 100, 1).withDescription("The strength of the dark tint.")
    val tintHud by ToggleSetting("Tint HUD").withDescription("Should the dark tint also apply to HUD elements?")

    @JvmStatic
    fun drawOverlay(ctx: GuiGraphics) {
        if (! enabled) return
        val window = mc.window
        ctx.fill(
            0, 0,
            window.guiScaledWidth,
            window.guiScaledHeight,
            Color.BLACK.withAlpha(opacity.value / 100f).rgb
        )
    }
}