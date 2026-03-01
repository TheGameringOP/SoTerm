package com.github.gameringop.features.impl.general.teleport

import com.github.gameringop.event.impl.RenderWorldEvent
import com.github.gameringop.features.Feature
import com.github.gameringop.ui.clickgui.components.getValue
import com.github.gameringop.ui.clickgui.components.hideIf
import com.github.gameringop.ui.clickgui.components.impl.ColorSetting
import com.github.gameringop.ui.clickgui.components.impl.DropdownSetting
import com.github.gameringop.ui.clickgui.components.impl.SliderSetting
import com.github.gameringop.ui.clickgui.components.impl.ToggleSetting
import com.github.gameringop.ui.clickgui.components.provideDelegate
import com.github.gameringop.ui.clickgui.components.section
import com.github.gameringop.utils.ColorUtils.withAlpha
import com.github.gameringop.utils.Utils
import com.github.gameringop.utils.Utils.equalsOneOf
import com.github.gameringop.utils.render.Render3D
import java.awt.Color

object EtherwarpOverlay: Feature() {
    private val mode by DropdownSetting("Mode", 0, listOf("Outline", "Fill", "Filled Outline")).section("Settings")
    private val phase by ToggleSetting("Phase")
    private val lineWidth by SliderSetting("Line Width", 1.0, 1.0, 10.0, 0.1).hideIf { mode.value == 1 }

    private val fillColor by ColorSetting("Fill Color", Utils.favoriteColor.withAlpha(50)).hideIf { mode.value == 0 }.section("Colors")
    private val outlineColor by ColorSetting("Outline Color", Utils.favoriteColor, false).hideIf { mode.value == 1 }

    private val invalidFillColor by ColorSetting("Invalid Fill Color ", Color.RED.withAlpha(50)).hideIf { mode.value == 0 }
    private val invalidOutlineColor by ColorSetting("Invalid Outline Color ", Color.RED, false).hideIf { mode.value == 1 }

    override fun init() {
        register<RenderWorldEvent> {
            val player = mc.player ?: return@register
            if (! player.isCrouching) return@register
            val heldItem = player.mainHandItem.takeUnless { it.isEmpty } ?: return@register
            val distance = EtherwarpHelper.getEtherwarpDistance(heldItem) ?: return@register
            val (valid, pos) = EtherwarpHelper.getEtherPos(player.position(), distance)
            
            Render3D.renderBlock(
                event.ctx, pos ?: return@register,
                if (valid) outlineColor.value else invalidOutlineColor.value,
                if (valid) fillColor.value else invalidFillColor.value,
                mode.value.equalsOneOf(0, 2),
                mode.value.equalsOneOf(1, 2),
                phase = phase.value,
                lineWidth.value.toFloat()
            )
        }
    }
}