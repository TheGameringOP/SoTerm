package com.github.gameringop.features.impl.visual

import com.github.gameringop.SoTerm
import com.github.gameringop.event.impl.EntityDeathEvent
import com.github.gameringop.event.impl.MainThreadPacketReceivedEvent
import com.github.gameringop.event.impl.RenderWorldEvent
import com.github.gameringop.event.impl.WorldChangeEvent
import com.github.gameringop.event.impl.TickEvent
import com.github.gameringop.features.Feature
import com.github.gameringop.ui.clickgui.components.getValue
import com.github.gameringop.ui.clickgui.components.impl.ButtonSetting
import com.github.gameringop.ui.clickgui.components.impl.ColorSetting
import com.github.gameringop.ui.clickgui.components.impl.DropdownSetting
import com.github.gameringop.ui.clickgui.components.impl.TextInputSetting
import com.github.gameringop.ui.clickgui.components.impl.ToggleSetting
import com.github.gameringop.ui.clickgui.components.provideDelegate
import com.github.gameringop.ui.clickgui.components.withDescription
import com.github.gameringop.ui.clickgui.components.showIf
import com.github.gameringop.utils.ChatUtils
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
import net.minecraft.world.entity.boss.wither.WitherBoss
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.ambient.Bat
import java.awt.Color

object BoxMobs : Feature("Highlights custom selected mobs everywhere in Skyblock.") {
    
    private val mode by DropdownSetting("Render Mode", 1, listOf("Fill", "Outline", "Filled Outline"))
        .withDescription("Choose how to render the box around selected mobs.")
    
    private val esp by ToggleSetting("See Through Walls", true)
        .withDescription("Box visible through walls.")
    
    private val mobColor by ColorSetting("Box Color", Color(0, 255, 0), false)
        .withDescription("Color used for mob highlighting (default: green).")

    private val batToggle by ToggleSetting("Bat", true)
        .withDescription("Highlight bats in the world.")
    
    private val batColor by ColorSetting("Bat Color", Color.GREEN, false)
        .showIf { batToggle.value }
        .withDescription("The color used for highlighted bats.")
    
    private val mobListInput by TextInputSetting("Mob Names", "")
        .withDescription("Enter mob names separated by commas (e.g., Zealot, Bruiser, Sadan)")
    
    private val refreshBtn by ButtonSetting("Refresh Cache", false) {
        trackedMobs.clear()
        checked.clear()
        cachedMobNames = emptyList()
        ChatUtils.modMessage("§aBoxMobs cache cleared!")
    }.withDescription("Clears the tracked mobs cache and refreshes detection.")

    private val trackedMobs = HashSet<Int>()
    private val checked = HashSet<Int>()
    private var cachedMobNames = emptyList<String>()

    private fun updateMobList() {
        cachedMobNames = mobListInput.value
            .split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
        
        if (SoTerm.debugFlags.contains("boxmobs")) {
            ChatUtils.modMessage("§7Loaded mob names: ${cachedMobNames.joinToString()}")
        }
    }

    override fun init() {
        register<MainThreadPacketReceivedEvent.Post> {
            if (!LocationUtils.inSkyblock) return@register
            if (event.packet !is ClientboundSetEntityDataPacket) return@register
            
            val entity = mc.level?.getEntity(event.packet.id) ?: return@register
            processEntity(entity)
        }

        register<TickEvent.Start> {
            if (!LocationUtils.inSkyblock || mc.level == null || mc.player == null) return@register
            if (mc.player!!.tickCount % 20 != 0) return@register

            mc.level!!.entitiesForRendering().forEach { entity ->
                processEntity(entity)
            }
        }

        register<EntityDeathEvent> {
            trackedMobs.remove(event.entity.id)
            checked.remove(event.entity.id)
        }

        register<WorldChangeEvent> {
            trackedMobs.clear()
            checked.clear()
            cachedMobNames = emptyList()
        }

        register<RenderWorldEvent> {
            if (!LocationUtils.inSkyblock || trackedMobs.isEmpty()) return@register

            for (id in trackedMobs) {
                val entity = mc.level?.getEntity(id) ?: continue
                if (!entity.isAlive) continue

                val renderColor = if (entity is Bat) {
                    batColor.value
                } else {
                    mobColor.value
                }

                Render3D.renderBox(
                    ctx = event.ctx,
                    x = entity.renderX,
                    y = entity.renderY,
                    z = entity.renderZ,
                    width = entity.boundingBox.xsize,
                    height = entity.boundingBox.ysize,
                    outlineColor = renderColor,
                    fillColor = renderColor.withAlpha(50),
                    outline = mode.value.equalsOneOf(1, 2),
                    fill = mode.value.equalsOneOf(0, 2),
                    phase = esp.value
                )
            }
        }
    }

    private fun processEntity(entity: Entity) {
        if (entity is Bat) {
            if (batToggle.value && !entity.isPassenger) {
                trackedMobs.add(entity.id)
            }
            return
        }

        if (entity is ArmorStand) {
            if (checked.contains(entity.id)) return

            val name = entity.customName?.formattedText ?: return
            val cleanName = name.removeFormatting().lowercase()
            
            if (cachedMobNames.isEmpty()) updateMobList()
            
            if (cachedMobNames.any { cleanName.contains(it) }) {
                checkMob(entity)
            }
        }
    }

    private fun checkMob(armorStand: Entity) {
        if (!checked.add(armorStand.id)) return
        
        val possibleEntities = armorStand.level().getEntities(
            armorStand, armorStand.boundingBox.move(0.0, -1.0, 0.0)
        ) { it !is ArmorStand }

        possibleEntities.find {
            !trackedMobs.contains(it.id) && when (it) {
                is Player -> !it.isInvisible && it.uuid.version() == 2 && it != mc.player
                is WitherBoss -> false
                else -> true
            }
        }?.let {
            trackedMobs.add(it.id)
        }
    }
}
