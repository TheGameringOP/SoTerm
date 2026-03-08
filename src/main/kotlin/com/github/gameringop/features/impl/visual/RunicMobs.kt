package com.github.gameringop.features.impl.visual

import com.github.gameringop.event.impl.MainThreadPacketReceivedEvent
import com.github.gameringop.event.impl.RenderWorldEvent
import com.github.gameringop.event.impl.WorldChangeEvent
import com.github.gameringop.features.Feature
import com.github.gameringop.ui.clickgui.components.getValue
import com.github.gameringop.ui.clickgui.components.impl.ColorSetting
import com.github.gameringop.ui.clickgui.components.impl.DropdownSetting
import com.github.gameringop.ui.clickgui.components.impl.TextInputSetting
import com.github.gameringop.ui.clickgui.components.impl.ToggleSetting
import com.github.gameringop.ui.clickgui.components.provideDelegate
import com.github.gameringop.ui.clickgui.components.withDescription
import com.github.gameringop.utils.ChatUtils.formattedText
import com.github.gameringop.utils.ChatUtils.removeFormatting
import com.github.gameringop.utils.ColorUtils.withAlpha
import com.github.gameringop.utils.Utils.equalsOneOf
import com.github.gameringop.utils.location.LocationUtils
import com.github.gameringop.utils.render.Render3D
import com.github.gameringop.utils.render.RenderHelper.renderX
import com.github.gameringop.utils.render.RenderHelper.renderY
import com.github.gameringop.utils.render.RenderHelper.renderZ
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.player.Player
import java.awt.Color
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.text.Regex

object BoxMobs : Feature("Highlights custom selected mobs everywhere in Skyblock.") {
    
    private val mode by DropdownSetting("Render Mode", 1, listOf("Fill", "Outline", "Filled Outline"))
        .withDescription("Choose how to render the box around selected mobs.")
    
    private val esp by ToggleSetting("See Through Walls", true)
        .withDescription("Box visible through walls.")
    
    private val mobColor by ColorSetting("Box Color", Color(0, 255, 0), false)
        .withDescription("Color used for mob highlighting (default: green).")
    
    private val mobListInput by TextInputSetting("Mob Names", "")
        .withDescription("Enter mob names separated by commas (e.g., Zombie, Tank Zombie, Sadan)")

    private val trackedMobs = CopyOnWriteArraySet<Int>()
    private var cachedMobNames = emptyList<String>()
    
    private val healthRegex = Regex("\\[Lv\\d+\\]|❤|/|\\d+%|\\d+\\.?\\d*[kKmM]?")
    private val bracketRegex = Regex("\\[.*?\\]|\\(.*?\\)")

    private fun cleanMobName(rawName: String): String {
        return rawName
            .removeFormatting()
            .replace(healthRegex, "")
            .replace(bracketRegex, "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun updateMobList() {
        cachedMobNames = mobListInput.value
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    override fun init() {
        register<MainThreadPacketReceivedEvent.Post> {
            if (!LocationUtils.inSkyblock) return@register
            if (event.packet !is ClientboundSetEntityDataPacket) return@register
            
            val entity = mc.level?.getEntity(event.packet.id) ?: return@register
            if (entity is ArmorStand || entity is Player) return@register
            
            val customName = entity.customName?.formattedText ?: return@register
            val cleanName = cleanMobName(customName)
            
            if (cachedMobNames.isEmpty()) {
                updateMobList()
            }
            
            if (cachedMobNames.any { targetName ->
                cleanName.equals(targetName, ignoreCase = true) || 
                cleanName.contains(targetName, ignoreCase = true)
            }) {
                trackedMobs.add(entity.id)
            }
        }

        register<WorldChangeEvent> {
            trackedMobs.clear()
            cachedMobNames = emptyList()
        }

        register<RenderWorldEvent> {
            if (!LocationUtils.inSkyblock) return@register
            if (trackedMobs.isEmpty()) return@register

            for (id in trackedMobs) {
                val entity = mc.level?.getEntity(id) ?: continue
                if (!entity.isAlive) continue

                val renderX = entity.renderX
                val renderY = entity.renderY
                val renderZ = entity.renderZ
                
                val bb = entity.boundingBox
                val width = bb.xsize
                val height = bb.ysize
                
                Render3D.renderBox(
                    ctx = event.ctx,
                    x = renderX,
                    y = renderY,
                    z = renderZ,
                    width = width,
                    height = height,
                    outlineColor = mobColor.value,
                    fillColor = mobColor.value.withAlpha(50),
                    outline = mode.value.equalsOneOf(1, 2),
                    fill = mode.value.equalsOneOf(0, 2),
                    phase = esp.value
                )
            }
        }
    }
}
