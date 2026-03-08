package com.github.gameringop.features.impl.visual

import com.github.gameringop.SoTerm
import com.github.gameringop.event.impl.EntityDeathEvent
import com.github.gameringop.event.impl.MainThreadPacketReceivedEvent
import com.github.gameringop.event.impl.RenderWorldEvent
import com.github.gameringop.event.impl.WorldChangeEvent
import com.github.gameringop.features.Feature
import com.github.gameringop.ui.clickgui.components.getValue
import com.github.gameringop.ui.clickgui.components.impl.ButtonSetting
import com.github.gameringop.ui.clickgui.components.impl.ColorSetting
import com.github.gameringop.ui.clickgui.components.impl.DropdownSetting
import com.github.gameringop.ui.clickgui.components.impl.TextInputSetting
import com.github.gameringop.ui.clickgui.components.impl.ToggleSetting
import com.github.gameringop.ui.clickgui.components.provideDelegate
import com.github.gameringop.ui.clickgui.components.withDescription
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
import java.awt.Color
import java.util.concurrent.CopyOnWriteArraySet

object BoxMobs : Feature("Highlights custom selected mobs everywhere in Skyblock.") {
    
    private val mode by DropdownSetting("Render Mode", 1, listOf("Fill", "Outline", "Filled Outline"))
        .withDescription("Choose how to render the box around selected mobs.")
    
    private val esp by ToggleSetting("See Through Walls", true)
        .withDescription("Box visible through walls.")
    
    private val mobColor by ColorSetting("Box Color", Color(0, 255, 0), false)
        .withDescription("Color used for mob highlighting (default: green).")
    
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
            if (entity !is ArmorStand) return@register
            
            val name = entity.customName?.formattedText ?: return@register
            val cleanName = name.removeFormatting().lowercase()
            
            if (SoTerm.debugFlags.contains("boxmobs")) {
                ChatUtils.modMessage("§7ArmorStand ${entity.id}: '$cleanName'")
            }
            
            if (cachedMobNames.isEmpty()) {
                updateMobList()
            }
            
            if (cachedMobNames.any { cleanName == it }) {
                if (SoTerm.debugFlags.contains("boxmobs")) {
                    ChatUtils.modMessage("§aExact match found for: $cleanName")
                }
                checkMob(entity, name)
            }
            else if (cachedMobNames.any { cleanName.contains(it) }) {
                if (SoTerm.debugFlags.contains("boxmobs")) {
                    ChatUtils.modMessage("§aContains match found for: $cleanName")
                }
                checkMob(entity, name)
            }
        }

        register<EntityDeathEvent> {
            trackedMobs.removeIf { it == event.entity.id }
            checked.removeIf { it == event.entity.id }
            if (SoTerm.debugFlags.contains("boxmobs") && trackedMobs.contains(event.entity.id)) {
                ChatUtils.modMessage("§cMob ${event.entity.id} died, removed from tracking")
            }
        }

        register<WorldChangeEvent> {
            trackedMobs.clear()
            checked.clear()
            cachedMobNames = emptyList()
            if (SoTerm.debugFlags.contains("boxmobs")) {
                ChatUtils.modMessage("§eWorld changed, cleared cache")
            }
        }

        register<RenderWorldEvent> {
            if (!LocationUtils.inSkyblock) return@register
            if (trackedMobs.isEmpty()) return@register

            if (SoTerm.debugFlags.contains("boxmobs")) {
                ChatUtils.modMessage("§7Rendering ${trackedMobs.size} tracked mobs")
            }

            for (id in trackedMobs) {
                val entity = mc.level?.getEntity(id) ?: continue
                if (!entity.isAlive) {
                    if (SoTerm.debugFlags.contains("boxmobs")) {
                        ChatUtils.modMessage("§cMob $id is dead, skipping")
                    }
                    continue
                }

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

    private fun checkMob(armorStand: Entity, name: String) {
        if (!checked.add(armorStand.id)) return
        
        if (SoTerm.debugFlags.contains("boxmobs")) {
            ChatUtils.modMessage("§eChecking for mob below armorStand ${armorStand.id}")
        }
        
        val possibleEntities = armorStand.level().getEntities(
            armorStand, armorStand.boundingBox.move(0.0, -1.0, 0.0)
        ) { it !is ArmorStand }

        val foundMob = possibleEntities.find {
            !trackedMobs.contains(it.id) && when (it) {
                is Player -> !it.isInvisible && it.uuid.version() == 2 && it != mc.player
                is WitherBoss -> false
                else -> true
            }
        }
        
        if (foundMob != null) {
            trackedMobs.add(foundMob.id)
            if (SoTerm.debugFlags.contains("boxmobs")) {
                ChatUtils.modMessage("§aAdded mob ${foundMob.id} to tracking")
            }
        } else if (SoTerm.debugFlags.contains("boxmobs")) {
            ChatUtils.modMessage("§cNo mob found below armorStand")
        }
    }
}
