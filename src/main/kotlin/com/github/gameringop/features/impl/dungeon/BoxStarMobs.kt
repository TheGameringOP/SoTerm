package com.github.gameringop.features.impl.dungeon

import com.github.gameringop.event.impl.EntityUnloadEvent
import com.github.gameringop.event.impl.MainThreadPacketReceivedEvent
import com.github.gameringop.event.impl.RenderWorldEvent
import com.github.gameringop.event.impl.WorldChangeEvent
import com.github.gameringop.features.Feature
import com.github.gameringop.ui.clickgui.components.impl.ColorSetting
import com.github.gameringop.ui.clickgui.components.impl.DropdownSetting
import com.github.gameringop.ui.clickgui.components.impl.SliderSetting
import com.github.gameringop.ui.clickgui.components.impl.ToggleSetting
import com.github.gameringop.utils.ChatUtils.formattedText
import com.github.gameringop.utils.ChatUtils.removeFormatting
import com.github.gameringop.utils.ChatUtils.unformattedText
import com.github.gameringop.utils.ColorUtils.withAlpha
import com.github.gameringop.utils.equalsOneOf
import com.github.gameringop.utils.location.LocationUtils
import com.github.gameringop.utils.location.LocationUtils.dungeonFloorNumber
import com.github.gameringop.utils.location.LocationUtils.inBoss
import com.github.gameringop.utils.render.OPRenderLayers
import com.github.gameringop.utils.render.Render3D
import com.github.gameringop.utils.render.RenderHelper.renderVec
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.ambient.Bat
import net.minecraft.world.entity.boss.wither.WitherBoss
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.monster.EnderMan
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import java.awt.Color
import kotlin.math.max

object BoxStarMob: Feature("Highlights all starred mobs in a dungeon.") {

    private val starMobs = HashSet<Int>()
    private val checked = HashSet<Int>()

    private val mode by DropdownSetting("Render Mode", 1, listOf("Fill", "Outline", "Filled Outline"))
    private val espMode by DropdownSetting("ESP Mode", 0, listOf("3D", "2D"))

    private val boxPhase by ToggleSetting("See Through Walls")
    private val lineWidth2d by SliderSetting("2D Line Width", 2.5f, 0.5f, 8.0f, 0.5f)
        .withDescription("Thickness of the 2D rectangle border.")
        .showIf{espMode.value == 1}

    private val starMobColor by ColorSetting("Star Mob Color", Color.GREEN, false).section("General Colors").withDescription("Default color for all Starred mobs.")
    private val batColor by ColorSetting("Bat Color", Color.GREEN, false).withDescription("The color used for highlighted bats.")
    private val felColor by ColorSetting("Fel Color", Color.GREEN, false).withDescription("The color used for fels.")

    override fun init() {
        register<MainThreadPacketReceivedEvent.Post> {
            if (! LocationUtils.inDungeon || inBoss) return@register
            if (event.packet !is ClientboundSetEntityDataPacket) return@register
            val entity = mc.level?.getEntity(event.packet.id) ?: return@register
            if (entity is ArmorStand) {
                val name = entity.customName?.formattedText ?: return@register
                if (name.endsWith("§c❤") && name.contains("✯")) {
                    checkStarMob(entity, name)
                }
            }
            else if (entity is Player) {
                val name = mc.connection?.getPlayerInfo(entity.uuid)?.profile?.name ?: return@register
                if (name.equalsOneOf("Shadow Assassin", "Lost Adventurer", "Diamond Guy", "King Midas")) {
                    starMobs.add(entity.id)
                }
            }
        }

        register<EntityUnloadEvent> {
            if (! LocationUtils.inDungeon || inBoss) return@register
            starMobs.remove(event.entity.id)
            checked.remove(event.entity.id)
        }

        register<WorldChangeEvent> {
            starMobs.clear()
            checked.clear()
        }

        register<RenderWorldEvent> {
            if (!LocationUtils.inDungeon || inBoss) return@register
            if (starMobs.isEmpty()) return@register

            for (id in starMobs) {
                val entity = mc.level?.getEntity(id) ?: continue
                if (!entity.isAlive) continue

                val bb = entity.boundingBox
                val width = bb.xsize
                val height = bb.ysize

                val color = getColor(entity) ?: starMobColor.value

                if (espMode.value == 0) {
                    Render3D.renderBox(
                        ctx = event.ctx,
                        x = entity.renderVec.x,
                        y = entity.renderVec.y,
                        z = entity.renderVec.z,
                        width = width,
                        height = height,
                        outlineColor = color,
                        fillColor = color.withAlpha(50),
                        outline = mode.value.equalsOneOf(1, 2),
                        fill = mode.value.equalsOneOf(0, 2),
                        phase = boxPhase.value
                    )
                } else {
                    render2DBox(event, entity, color)
                }
            }
        }
    }

    private fun render2DBox(event: RenderWorldEvent, entity: Entity, color: Color) {
        if (color.alpha <= 0) return

        val player = mc.player ?: return
        val bb = entity.boundingBox
        // Use interpolated render position for smooth movement
        val center = entity.renderVec.add(0.0, bb.ysize / 2.0, 0.0)

        // Make a clean 4-line rectangle, facing the player.
        // NOTE: boundingBox.center is mid-point, so use half extents for correct Y alignment.
        val width = max(bb.xsize, bb.zsize).coerceAtLeast(0.6)
        val height = bb.ysize.coerceAtLeast(0.8)

        val toPlayer = player.position().subtract(center)
        val forward = Vec3(toPlayer.x, 0.0, toPlayer.z).let {
            if (it.lengthSqr() < 1.0E-6) Vec3(0.0, 0.0, 1.0) else it.normalize()
        }
        val right = Vec3(-forward.z, 0.0, forward.x)

        val halfW = width / 2.0
        val halfH = height / 2.0
        val top = center.add(0.0, halfH, 0.0)
        val bottom = center.add(0.0, -halfH, 0.0)
        val leftOffset = right.scale(-halfW)
        val rightOffset = right.scale(halfW)

        val topLeft = top.add(leftOffset)
        val topRight = top.add(rightOffset)
        val bottomLeft = bottom.add(leftOffset)
        val bottomRight = bottom.add(rightOffset)

        val drawFill = mode.value.equalsOneOf(0, 2)
        val drawOutline = mode.value.equalsOneOf(1, 2)

        val throughWalls = boxPhase.value
        if (drawFill) {
            renderFilled2DQuad(event, topLeft, topRight, bottomRight, bottomLeft, color.withAlpha(50), throughWalls)
        }

        if (drawOutline) {
            val thickness = lineWidth2d.value
            Render3D.renderLine(event.ctx, topLeft, topRight, color, thickness, throughWalls)
            Render3D.renderLine(event.ctx, topRight, bottomRight, color, thickness, throughWalls)
            Render3D.renderLine(event.ctx, bottomRight, bottomLeft, color, thickness, throughWalls)
            Render3D.renderLine(event.ctx, bottomLeft, topLeft, color, thickness, throughWalls)
        }
    }

    private fun renderFilled2DQuad(
        event: RenderWorldEvent,
        topLeft: Vec3,
        topRight: Vec3,
        bottomRight: Vec3,
        bottomLeft: Vec3,
        fillColor: Color,
        throughWalls: Boolean
    ) {
        if (fillColor.alpha <= 0) return

        val cameraPos = event.ctx.camera.position
        event.ctx.matrixStack.pushPose()
        event.ctx.matrixStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z)

        val layer = if (throughWalls) OPRenderLayers.FILLED_THROUGH_WALLS else OPRenderLayers.FILLED
        val consumers = event.ctx.consumers as MultiBufferSource.BufferSource
        val matrix = event.ctx.matrixStack.last().pose()

        val r = fillColor.red / 255f
        val g = fillColor.green / 255f
        val b = fillColor.blue / 255f
        val a = fillColor.alpha / 255f

        // Emit both windings so fill is visible regardless of face culling.
        run {
            val consumer = consumers.getBuffer(layer)
            consumer.addVertex(matrix, topLeft.x.toFloat(), topLeft.y.toFloat(), topLeft.z.toFloat()).setColor(r, g, b, a)
            consumer.addVertex(matrix, bottomLeft.x.toFloat(), bottomLeft.y.toFloat(), bottomLeft.z.toFloat()).setColor(r, g, b, a)
            consumer.addVertex(matrix, topRight.x.toFloat(), topRight.y.toFloat(), topRight.z.toFloat()).setColor(r, g, b, a)
            consumer.addVertex(matrix, bottomRight.x.toFloat(), bottomRight.y.toFloat(), bottomRight.z.toFloat()).setColor(r, g, b, a)
            consumers.endBatch(layer)
        }

        run {
            val consumer = consumers.getBuffer(layer)
            consumer.addVertex(matrix, topRight.x.toFloat(), topRight.y.toFloat(), topRight.z.toFloat()).setColor(r, g, b, a)
            consumer.addVertex(matrix, bottomRight.x.toFloat(), bottomRight.y.toFloat(), bottomRight.z.toFloat()).setColor(r, g, b, a)
            consumer.addVertex(matrix, topLeft.x.toFloat(), topLeft.y.toFloat(), topLeft.z.toFloat()).setColor(r, g, b, a)
            consumer.addVertex(matrix, bottomLeft.x.toFloat(), bottomLeft.y.toFloat(), bottomLeft.z.toFloat()).setColor(r, g, b, a)
            consumers.endBatch(layer)
        }

        event.ctx.matrixStack.popPose()
    }

    private fun getColor(entity: Entity): Color? {
        if (entity is Bat) return if (! entity.isPassenger) batColor.value else null
        if (entity is EnderMan) return if (entity.name.unformattedText == "Dinnerbone") felColor.value else null
        if (entity is Player) {
            val name = entity.name.unformattedText.takeUnless { it.isBlank() } ?: return null
            if (name.contains("Shadow Assassin")) return starMobColor.value

            if (dungeonFloorNumber != 4 && ! inBoss) {
                val bootsName = entity.getItemBySlot(EquipmentSlot.FEET).takeUnless { it.isEmpty }?.hoverName?.unformattedText ?: return null

                return when (name) {
                    "Lost Adventurer" -> starMobColor.value
                    "Diamond Guy" -> if ("Perfect Boots" in bootsName) starMobColor.value else null
                    else -> null
                }
            }
        }

        return null
    }

    private fun checkStarMob(armorStand: Entity, name: String) {
        if (! checked.add(armorStand.id)) return
        val name = name.removeFormatting().uppercase()
        // withermancers are always -3 to real entity the -1 and -2 are the wither skulls that they shoot
        val offset = if (name.contains("WITHERMANCER")) 3 else 1
        val id = armorStand.id - offset

        val mob = armorStand.level().getEntity(id)
        if (mob !is ArmorStand && id !in starMobs && mob != null) {
            starMobs.add(id)
            return
        }

        val possibleEntities = armorStand.level().getEntities(
            armorStand, armorStand.boundingBox.move(0.0, - 1.0, 0.0)
        ) { it !is ArmorStand }

        possibleEntities.find {
            ! starMobs.contains(it.id) && when (it) {
                is Player -> ! it.isInvisible && it.uuid.version() == 2 && it != mc.player
                is WitherBoss -> false
                else -> true
            }
        }?.let {
            if (getColor(it) == null) starMobs.add(it.id)
        }
    }
}