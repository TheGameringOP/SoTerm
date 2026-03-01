package com.github.gameringop.features.impl.general

import com.github.gameringop.event.impl.RenderWorldEvent
import com.github.gameringop.features.Feature
import com.github.gameringop.ui.clickgui.components.getValue
import com.github.gameringop.ui.clickgui.components.impl.ColorSetting
import com.github.gameringop.ui.clickgui.components.impl.SliderSetting
import com.github.gameringop.ui.clickgui.components.impl.ToggleSetting
import com.github.gameringop.ui.clickgui.components.provideDelegate
import com.github.gameringop.ui.clickgui.components.section
import com.github.gameringop.ui.clickgui.components.withDescription
import com.github.gameringop.utils.ColorUtils.withAlpha
import com.github.gameringop.utils.MathUtils
import com.github.gameringop.utils.MathUtils.add
import com.github.gameringop.utils.MathUtils.toVec
import com.github.gameringop.utils.Utils
import com.github.gameringop.utils.items.ItemUtils.skyblockId
import com.github.gameringop.utils.render.Render3D
import com.github.gameringop.utils.world.WorldUtils
import net.minecraft.tags.BlockTags

object GyroHelper: Feature("Renders a circle where your gyro will be located.", "Gyro Helper") {
    private val drawBox by ToggleSetting("Draw Box", true).withDescription("Draws a box in the middle of the Gyrokinetic Wand sucking range").section("Render")
    private val drawRing by ToggleSetting("Draw Sucking Range", true).withDescription("Draws the sucking range of the Gyrokinetic Wand")
    private val lineWidth by SliderSetting("Ring Width", 2, 1, 10, 1).withDescription("Controls the thickness of the ring")

    private val boxColor by ColorSetting("Box Color", Utils.favoriteColor.withAlpha(0.3f)).section("Color")
    private val ringColor by ColorSetting("Ring Color", Utils.favoriteColor)

    override fun init() {
        register<RenderWorldEvent> {
            if (! drawRing.value && ! drawBox.value) return@register
            if (boxColor.value.alpha + ringColor.value.alpha == 0) return@register
            if (mc.player?.mainHandItem?.skyblockId != "GYROKINETIC_WAND") return@register
            val gyroPos = MathUtils.raytrace(mc.player !!, 25) ?: return@register
            val stateAtPos = WorldUtils.getStateAt(gyroPos)
            val stateAbove = WorldUtils.getStateAt(gyroPos.above())

            if (stateAtPos.isAir || (! stateAbove.isAir && ! stateAbove.`is`(BlockTags.WOOL_CARPETS))) return@register

            if (drawBox.value) Render3D.renderBox(event.ctx, gyroPos.x + 0.5, gyroPos.y, gyroPos.z + 0.5, 1, 1, boxColor.value)
            if (drawRing.value) {
                val center = gyroPos.add(0.5, 2.05, 0.5).toVec()
                Render3D.renderCircle(event.ctx, center, 10.0, ringColor.value, lineWidth.value)
            }
        }
    }
}