package com.github.gameringop.features.impl.visual

import com.github.gameringop.event.impl.MainThreadPacketReceivedEvent
import com.github.gameringop.event.impl.TickEvent
import com.github.gameringop.features.Feature
import com.github.gameringop.utils.items.ItemUtils.skyblockId
import com.github.gameringop.utils.location.LocationUtils
import com.github.gameringop.utils.render.Render2D.width
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.EquipmentSlot
import java.awt.Color
import kotlin.math.roundToInt

object SpringBoots: Feature("Shows the spring boots charge progress on screen.") {
    private var progress = 0

    private val pitchList = listOf(
        0.6984127, 0.6666667, 0.82539684,
        0.8888889, 0.93650794, 1.0476191,
        1.1746032, 1.3174603, 1.6984127
    )

    override fun init() {
        hudElement("Spring Boots Display", shouldDraw = { LocationUtils.inSkyblock }) { context, example ->
            if (progress <= 0 && ! example) return@hudElement 0f to 0f
            val text = (if (example) 33 else ((progress / 42.0) * 100.0).roundToInt()).toString() + "%"
            val color = getColorForProgress(if (example) 14 else progress)

            context.drawString(mc.font, text, 0, 0, color)
            return@hudElement text.width().toFloat() to 9f
        }

        register<MainThreadPacketReceivedEvent.Pre> {
            if (event.packet !is ClientboundSoundPacket) return@register
            if (event.packet.sound != SoundEvents.NOTE_BLOCK_PLING) return@register
            val player = mc.player ?: return@register
            if (! player.isCrouching) return@register
            if (! player.onGround()) return@register
            if (! isWearingSpringBoots()) return@register
            if (progress >= 42) return@register

            if (pitchList.any { it.toFloat() == event.packet.pitch }) {
                progress ++
            }
        }

        register<TickEvent.Start> {
            if (progress == 0) return@register
            val player = mc.player ?: return@register

            if (! player.isCrouching || ! player.onGround() || ! isWearingSpringBoots()) {
                progress = 0
            }
        }
    }

    private fun isWearingSpringBoots(): Boolean {
        return mc.player?.getItemBySlot(EquipmentSlot.FEET)?.skyblockId == "SPRING_BOOTS"
    }

    private fun getColorForProgress(current: Int): Int {
        val percent = (current / 42.0).coerceIn(0.0, 1.0)
        return Color.HSBtoRGB((percent * 0.33).toFloat(), 1f, 1f)
    }
}