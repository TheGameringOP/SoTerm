package com.github.gameringop.features.impl.misc

import com.github.gameringop.features.Feature
import com.github.gameringop.features.impl.general.teleport.EtherwarpHelper
import com.github.gameringop.ui.clickgui.components.getValue
import com.github.gameringop.ui.clickgui.components.hideIf
import com.github.gameringop.ui.clickgui.components.impl.ColorSetting
import com.github.gameringop.ui.clickgui.components.impl.DropdownSetting
import com.github.gameringop.ui.clickgui.components.impl.SliderSetting
import com.github.gameringop.ui.clickgui.components.impl.ToggleSetting
import com.github.gameringop.ui.clickgui.components.provideDelegate
import com.github.gameringop.utils.ColorUtils.withAlpha
import com.github.gameringop.utils.Utils
import com.github.gameringop.utils.Utils.equalsOneOf
import com.github.gameringop.utils.render.Render3D
import com.github.gameringop.utils.render.RenderContext
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents

object BlockOverlay: Feature() {
    private val mode by DropdownSetting("Mode", 2, listOf("Outline", "Fill", "Filled Outline"))
    private val fillColor by ColorSetting("Fill Color", Utils.favoriteColor.withAlpha(50)).hideIf { mode.value == 0 }
    private val outlineColor by ColorSetting("Outline Color", Utils.favoriteColor, false).hideIf { mode.value == 1 }
    private val lineWidth by SliderSetting("Line Width", 2.5, 1, 10, 0.1).hideIf { mode.value == 1 }
    private val phase by ToggleSetting("Phase")
    private val hideDuringEtherwarp by ToggleSetting("Hide with Etherwarp")

    override fun init() {
        WorldRenderEvents.BEFORE_BLOCK_OUTLINE.register { context, blockOutlineContext ->
            if (! enabled) return@register true
            if (mc.options.hideGui) return@register true
            if (hideDuringEtherwarp.value && shouldHide()) return@register false

            Render3D.renderBlock(
                RenderContext.fromContext(context),
                blockOutlineContext.pos,
                outlineColor.value,
                fillColor.value,
                mode.value.equalsOneOf(0, 2),
                mode.value.equalsOneOf(1, 2),
                phase = phase.value,
                lineWidth.value
            )

            false
        }
    }

    private fun shouldHide(): Boolean {
        if (! hideDuringEtherwarp.value) return false
        val player = mc.player ?: return false
        if (! player.isCrouching) return false

        val held = player.mainHandItem.takeUnless { it.isEmpty } ?: return false
        return EtherwarpHelper.getEtherwarpDistance(held) != null
    }
}
