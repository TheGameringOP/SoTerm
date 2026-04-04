package com.github.gameringop.features.impl.visual

import com.github.gameringop.SoTerm
import com.github.gameringop.event.impl.*
import com.github.gameringop.features.Feature
import com.github.gameringop.ui.clickgui.components.getValue
import com.github.gameringop.ui.clickgui.components.impl.ColorSetting
import com.github.gameringop.ui.clickgui.components.impl.DropdownSetting
import com.github.gameringop.ui.clickgui.components.impl.ToggleSetting
import com.github.gameringop.ui.clickgui.components.provideDelegate
import com.github.gameringop.ui.clickgui.components.withDescription
import com.github.gameringop.utils.ChatUtils
import com.github.gameringop.utils.ChatUtils.formattedText
import com.github.gameringop.utils.ChatUtils.removeFormatting
import com.github.gameringop.utils.ColorUtils.withAlpha
import com.github.gameringop.utils.Utils.equalsOneOf
import com.github.gameringop.utils.location.LocationUtils
import com.github.gameringop.utils.location.WorldType
import com.github.gameringop.utils.render.Render3D
import com.github.gameringop.utils.render.RenderHelper.renderX
import com.github.gameringop.utils.render.RenderHelper.renderY
import com.github.gameringop.utils.render.RenderHelper.renderZ
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.world.entity.decoration.ArmorStand
import java.awt.Color
import java.util.concurrent.CopyOnWriteArraySet

object PestBox: Feature("Highlights garden pests in the Garden.") {

    private val mode by DropdownSetting("Render Mode", 1, listOf("Fill", "Outline", "Filled Outline"))
        .withDescription("Choose how to render the box around pests.")

    private val esp by ToggleSetting("See Through Walls", true)
        .withDescription("Box visible through walls.")

    private val pestColor by ColorSetting("Pest Color", Color(0, 255, 0), false)
        .withDescription("Color used for pest highlighting (default: green).")

    private data class PestInfo(val id: Int, val oneBelow: Boolean)

    private val trackedPests = CopyOnWriteArraySet<PestInfo>()
    private val checked = CopyOnWriteArraySet<Int>()

    override fun init() {
        register<MainThreadPacketReceivedEvent.Post> {
            if (LocationUtils.world != WorldType.Garden) return@register
            if (event.packet !is ClientboundSetEntityDataPacket) return@register

            val entity = mc.level?.getEntity(event.packet.id) ?: return@register
            if (entity !is ArmorStand) return@register

            val name = entity.customName?.formattedText ?: return@register
            val cleanName = name.removeFormatting().lowercase()

            if (SoTerm.debugFlags.contains("pestbox")) {
                ChatUtils.modMessage("§7ArmorStand ${entity.id}: raw='$name', clean='$cleanName'")
            }

            val oneBelow = cleanName.contains("moth") || cleanName.contains("dragonfly")

            val pestNames = listOf("mite", "cricket", "beetle", "slug", "fly", "moth", "mosquito", "locust", "earthworm", "dragonfly", "firefly", "rat", "praying mantis")
            if (pestNames.any { cleanName.contains(it) }) {
                if (SoTerm.debugFlags.contains("pestbox")) {
                    ChatUtils.modMessage("§aPest detected! Adding to tracking...")
                }
                trackedPests.add(PestInfo(entity.id, oneBelow))
            }
        }

        register<TickEvent.Start> {
            if (LocationUtils.world != WorldType.Garden) return@register
            if (mc.level == null || mc.player == null) return@register
            if (mc.player!!.tickCount % 20 != 0) return@register

            mc.level!!.entitiesForRendering().forEach { entity ->
                if (entity is ArmorStand && !checked.contains(entity.id)) {
                    val name = entity.customName?.formattedText ?: return@forEach
                    val cleanName = name.removeFormatting().lowercase()
                    val oneBelow = cleanName.contains("moth") || cleanName.contains("dragonfly") || cleanName.contains("firefly")
                    val pestNames = listOf("mite", "cricket", "beetle", "slug", "fly", "moth", "mosquito", "locust", "earthworm", "dragonfly", "firefly", "rat", "praying mantis", "field mouse")
                    if (pestNames.any { cleanName.contains(it) }) {
                        trackedPests.add(PestInfo(entity.id, oneBelow))
                        checked.add(entity.id)
                    }
                }
            }
        }

        register<EntityDeathEvent> {
            trackedPests.removeIf { it.id == event.entity.id }
            checked.remove(event.entity.id)
        }

        register<WorldChangeEvent> {
            trackedPests.clear()
            checked.clear()
        }

        register<RenderWorldEvent> {
            if (LocationUtils.world != WorldType.Garden) return@register
            if (trackedPests.isEmpty()) return@register

            for (pest in trackedPests) {
                val entity = mc.level?.getEntity(pest.id) ?: continue
                if (!entity.isAlive) continue

                val boxSize = 0.7
                val yOffset = if (pest.oneBelow) 1.0 else 0.5

                Render3D.renderBox(
                    ctx = event.ctx,
                    x = entity.renderX,
                    y = entity.renderY - yOffset,
                    z = entity.renderZ,
                    width = boxSize,
                    height = boxSize,
                    outlineColor = pestColor.value,
                    fillColor = pestColor.value.withAlpha(50),
                    outline = mode.value.equalsOneOf(1, 2),
                    fill = mode.value.equalsOneOf(0, 2),
                    phase = esp.value
                )
            }
        }
    }
}