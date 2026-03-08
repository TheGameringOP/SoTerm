package com.github.gameringop.features.impl.visual

import com.github.gameringop.SoTerm
import com.github.gameringop.event.impl.MainThreadPacketReceivedEvent
import com.github.gameringop.event.impl.RenderWorldEvent
import com.github.gameringop.event.impl.WorldChangeEvent
import com.github.gameringop.features.Feature
import com.github.gameringop.ui.clickgui.components.getValue
import com.github.gameringop.ui.clickgui.components.impl.ColorSetting
import com.github.gameringop.ui.clickgui.components.impl.DropdownSetting
import com.github.gameringop.ui.clickgui.components.impl.ToggleSetting
import com.github.gameringop.ui.clickgui.components.provideDelegate
import com.github.gameringop.ui.clickgui.components.withDescription
import com.github.gameringop.utils.ChatUtils.formattedText
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
import java.awt.Color
import java.util.concurrent.CopyOnWriteArraySet

object RunicMobs : Feature("Highlights runic mobs everywhere in Skyblock.") {
    
    private val mode by DropdownSetting("Render Mode", 1, listOf("Fill", "Outline", "Filled Outline"))
        .withDescription("Choose how to render the box around runic mobs.")
    
    private val esp by ToggleSetting("See Through Walls", true)
        .withDescription("Box visible through walls.")
    
    private val runicColor by ColorSetting("Runic Color", Color(170, 0, 170), false)
        .withDescription("Color used for runic mob highlighting (default: purple).")

    private val runicMobs = CopyOnWriteArraySet<Int>()
    private val checked = CopyOnWriteArraySet<Int>()

    override fun init() {
        register<MainThreadPacketReceivedEvent.Post> {
            if (!LocationUtils.inSkyblock) return@register
            if (event.packet !is ClientboundSetEntityDataPacket) return@register
            
            val entity = mc.level?.getEntity(event.packet.id) ?: return@register
            if (entity !is ArmorStand) return@register
            
            val name = entity.customName?.formattedText ?: return@register
            
            if (name.startsWith("[") && name.contains("§5")) {
                runicMobs.add(entity.id)
                findRunicMob(entity)
            }
        }

        register<WorldChangeEvent> {
            runicMobs.clear()
            checked.clear()
        }

        register<RenderWorldEvent> {
            if (!LocationUtils.inSkyblock) return@register
            if (runicMobs.isEmpty()) return@register

            for (id in runicMobs) {
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
                    outlineColor = runicColor.value,
                    fillColor = runicColor.value.withAlpha(50),
                    outline = mode.value.equalsOneOf(1, 2),
                    fill = mode.value.equalsOneOf(0, 2),
                    phase = esp.value
                )
            }
        }
    }

    private fun findRunicMob(armorStand: Entity) {
        if (!checked.add(armorStand.id)) return
        
        val possibleEntities = armorStand.level().getEntities(
            armorStand,
            armorStand.boundingBox.inflate(2.0, 2.0, 2.0)
        ) { it !is ArmorStand }
        
        possibleEntities.find { mob ->
            !runicMobs.contains(mob.id) && when (mob) {
                is Player -> false
                is WitherBoss -> false
                else -> true
            }
        }?.let { mob ->
            runicMobs.add(mob.id)
        }
    }
}
