package com.github.gameringop.features.impl.combat

import com.github.gameringop.event.impl.RenderWorldEvent
import com.github.gameringop.event.impl.TickEvent
import com.github.gameringop.features.Feature
import com.github.gameringop.ui.clickgui.components.impl.ColorSetting
import com.github.gameringop.ui.clickgui.components.impl.SliderSetting
import com.github.gameringop.ui.clickgui.components.impl.ToggleSetting
import com.github.gameringop.utils.items.ItemUtils.skyblockId
import com.github.gameringop.utils.render.Render3D
import com.github.gameringop.utils.render.RenderContext
import net.minecraft.core.Direction
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.projectile.AbstractArrow
import net.minecraft.world.item.BowItem
import net.minecraft.world.item.EnderpearlItem
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import java.awt.Color
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

object Trajectories: Feature(name = "Trajectories", description = "Shows the trajectories of arrows, snowballs, etc.") {
    private val bows by ToggleSetting("Bows", true)
    private val pearls by ToggleSetting("Pearls", true)
    private val plane by ToggleSetting("Show Plane", false)
    private val boxes by ToggleSetting("Show Boxes", true)
    private val lines by ToggleSetting("Show Lines", true)
    private val range by SliderSetting("Solver Range", 30, 1, 120, 1)
    private val width by SliderSetting("Line Width", 1f, 0.1f, 5.0, 0.1f)
    private val planeSize by SliderSetting("Plane Size", 2f, 0.1f, 5.0, 0.1f).showIf { plane.value }
    private val boxSize by SliderSetting("Box Size", 0.5f, 0.5f, 3.0f, 0.1f).showIf { boxes.value }
    private val color by ColorSetting("Color", Color(0, 255, 255, 255))
    private val depth by ToggleSetting("Depth Check", true)

    private var charge = 0f
    private var lastCharge = 0f
    private var cachedTrajectory: List<Pair<List<Vec3>, BlockHitResult?>> = emptyList()
    private var cachedPearlTrajectory: Pair<List<Vec3>, BlockHitResult?>? = null
    private var cachedEntityHits: List<Entity> = emptyList()

    override fun init() {
        register<TickEvent.Start> {
            val player = mc.player ?: return@register
            lastCharge = charge
            val useCount = player.useItemRemainingTicks
            charge = min((72000 - useCount) / 20f, 1.0f) * 2f
            if ((lastCharge - charge) > 1f) lastCharge = charge

            cachedTrajectory = emptyList()
            cachedPearlTrajectory = null
            cachedEntityHits = emptyList()

            val heldItem = player.mainHandItem

            if (bows.value && heldItem.item is BowItem) {
                val isTerminator = heldItem.skyblockId == "TERMINATOR"
                cachedTrajectory = if (isTerminator) {
                    listOf(
                        calculateTrajectory(0f, isPearl = false, useCharge = true),
                        calculateTrajectory(-5f, isPearl = false, useCharge = true),
                        calculateTrajectory(5f, isPearl = false, useCharge = true)
                    )
                } else {
                    listOf(calculateTrajectory(0f, isPearl = false, useCharge = true))
                }
            } else if (pearls.value && heldItem.item is EnderpearlItem) {
                if (heldItem.displayName?.string?.contains("Spirit") != true) {
                    cachedPearlTrajectory = calculateTrajectory(0f, isPearl = true, useCharge = false)
                }
            }
        }

        register<RenderWorldEvent> {
            if (!lines.value && !boxes.value && !plane.value) return@register
            val ctx = event.ctx

            if (bows.value) {
                for ((points, hit) in cachedTrajectory) {
                    if (lines.value && points.isNotEmpty()) drawTrajectoryLine(ctx, points)
                    if (boxes.value) drawCollisionBoxes(ctx, isPearl = false)
                    if (plane.value && hit != null) drawPlaneCollision(ctx, hit)
                }
            }

            if (pearls.value && cachedPearlTrajectory != null) {
                val (points, hit) = cachedPearlTrajectory!!
                if (lines.value && points.isNotEmpty()) drawTrajectoryLine(ctx, points)
                if (boxes.value) drawCollisionBoxes(ctx, isPearl = true)
                if (plane.value && hit != null) drawPlaneCollision(ctx, hit)
            }
        }
    }

    private fun drawTrajectoryLine(ctx: RenderContext, points: List<Vec3>) {
        for (i in 0 until points.size - 1) {
            Render3D.renderLine(ctx, points[i], points[i + 1], color.value, width.value, !depth.value)
        }
    }

    private fun drawCollisionBoxes(ctx: RenderContext, isPearl: Boolean) {
        if (isPearl) {
            val hit = cachedPearlTrajectory?.second
            if (hit != null && boxes.value) {
                val box = AABB(
                    hit.location.x - 0.15 * boxSize.value.toDouble(),
                    hit.location.y - 0.15 * boxSize.value.toDouble(),
                    hit.location.z - 0.15 * boxSize.value.toDouble(),
                    hit.location.x + 0.15 * boxSize.value.toDouble(),
                    hit.location.y + 0.15 * boxSize.value.toDouble(),
                    hit.location.z + 0.15 * boxSize.value.toDouble()
                )
                drawBox(ctx, box)
            }
            return
        }

        for ((_, hit) in cachedTrajectory) {
            if (hit != null) {
                val box = AABB(
                    hit.location.x - 0.15 * boxSize.value.toDouble(),
                    hit.location.y - 0.15 * boxSize.value.toDouble(),
                    hit.location.z - 0.15 * boxSize.value.toDouble(),
                    hit.location.x + 0.15 * boxSize.value.toDouble(),
                    hit.location.y + 0.15 * boxSize.value.toDouble(),
                    hit.location.z + 0.15 * boxSize.value.toDouble()
                )
                drawBox(ctx, box)
            }
        }

        for (entity in cachedEntityHits) {
            drawBox(ctx, entity.boundingBox)
        }
    }

    private fun drawBox(ctx: RenderContext, box: AABB) {
        val col = color.value
        Render3D.renderBox(
            ctx,
            box.minX, box.minY, box.minZ,
            box.maxX - box.minX, box.maxY - box.minY,
            outlineColor = col,
            fillColor = Color(col.red, col.green, col.blue, (col.alpha * 0.3).toInt()),
            outline = true, fill = true, phase = !depth.value
        )
    }

    private fun drawPlaneCollision(ctx: RenderContext, hit: BlockHitResult) {
        val size = planeSize.value.toDouble()
        val (minVec, maxVec) = when (hit.direction) {
            Direction.DOWN, Direction.UP ->
                hit.location.add(Vec3(-0.15 * size, -0.02, -0.15 * size)) to hit.location.add(
                    Vec3(
                        0.15 * size,
                        0.02,
                        0.15 * size
                    )
                )
            Direction.NORTH, Direction.SOUTH ->
                hit.location.add(Vec3(-0.15 * size, -0.15 * size, -0.02)) to hit.location.add(
                    Vec3(
                        0.15 * size,
                        0.15 * size,
                        0.02
                    )
                )
            Direction.WEST, Direction.EAST ->
                hit.location.add(Vec3(-0.02, -0.15 * size, -0.15 * size)) to hit.location.add(
                    Vec3(
                        0.02,
                        0.15 * size,
                        0.15 * size
                    )
                )
            else -> return
        }
        val box = AABB(minVec, maxVec)
        val col = color.value
        Render3D.renderBox(
            ctx,
            box.minX, box.minY, box.minZ,
            box.maxX - box.minX, box.maxY - box.minY,
            outlineColor = col,
            fillColor = Color(col.red, col.green, col.blue, (col.alpha * 0.5).toInt()),
            outline = false, fill = true, phase = !depth.value
        )
    }

    private fun calculateTrajectory(yawOffset: Float, isPearl: Boolean, useCharge: Boolean): Pair<List<Vec3>, BlockHitResult?> {
        val player = mc.player ?: return emptyList<Vec3>() to null
        val level = mc.level ?: return emptyList<Vec3>() to null

        val yaw = Math.toRadians(player.yRot.toDouble())
        val x = -cos(yaw) * 0.16
        val z = -sin(yaw) * 0.16
        var pos = player.getEyePosition().add(Vec3(x, -0.1, z))
        var prevPos = pos

        val speed = if (isPearl) 1.5 else if (useCharge) pull() * 3.0 else 1.0
        var motion = getLook(player.yRot + yawOffset, player.xRot).normalize().scale(speed)

        var hitResult: BlockHitResult? = null
        val lines = mutableListOf<Vec3>()

        repeat(range.value) {
            if (hitResult != null) return@repeat
            lines.add(pos)

            if (!isPearl) {
                val scanBox = AABB(prevPos, pos.add(motion)).inflate(1.0)
                val hit = level.getEntities(player, scanBox)
                    .filter { it !is AbstractArrow && it !is ArmorStand && it != player }
                    .mapNotNull { entity ->
                        entity.boundingBox.inflate(entity.pickRadius.toDouble())
                            .clip(prevPos, pos.add(motion))
                            .map { entity to it }
                            .orElse(null)
                    }
                    .minByOrNull { (_, hitPos) -> prevPos.distanceToSqr(hitPos) }

                if (hit != null) {
                    lines.add(hit.second)
                    cachedEntityHits = listOf(hit.first)
                    hitResult = null
                    return@repeat
                }
            }

            val blockHit = level.clip(
                ClipContext(
                    pos,
                    pos.add(motion),
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    player
                )
            )
            if (blockHit.type == HitResult.Type.BLOCK) {
                hitResult = blockHit as BlockHitResult
                lines.add(blockHit.location)
                return@repeat
            }

            if (isPearl) {
                motion = Vec3(motion.x * 0.99, (motion.y - 0.03) * 0.99, motion.z * 0.99)
                pos = pos.add(motion)
            } else {
                pos = pos.add(motion)
                motion = Vec3(motion.x * 0.99, motion.y * 0.99 - 0.05, motion.z * 0.99)
                prevPos = pos
            }
        }
        return lines to hitResult
    }

    private fun getLook(yaw: Float, pitch: Float): Vec3 {
        val f2 = -cos(-pitch * 0.017453292) * 1.0
        return Vec3(
            sin(-yaw * 0.017453292 - Math.PI) * f2,
            sin(-pitch * 0.017453292) * 1.0,
            cos(-yaw * 0.017453292 - Math.PI) * f2
        )
    }

    private fun pull(): Float {
        val t = lastCharge + (charge - lastCharge) * mc.deltaTracker.getGameTimeDeltaPartialTick(true)
        val f = (t / 2f).coerceIn(0f, 1f)
        return (f * f + f * 2f) / 3f
    }
}