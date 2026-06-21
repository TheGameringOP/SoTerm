package com.github.gameringop.features.impl.misc

import com.github.gameringop.event.impl.BlockChangeEvent
import com.github.gameringop.event.impl.TickEvent
import com.github.gameringop.event.impl.WorldChangeEvent
import com.github.gameringop.features.Feature
import com.github.gameringop.ui.clickgui.components.impl.SliderSetting
import com.github.gameringop.utils.ActionUtils.queue
import com.github.gameringop.utils.MathUtils.calcYawPitch
import com.github.gameringop.utils.PlayerUtils.rightClick
import com.github.gameringop.utils.PlayerUtils.rotate
import com.github.gameringop.utils.PlayerUtils.rotateSmoothly
import com.github.gameringop.utils.ThreadUtils
import com.github.gameringop.utils.WorldUtils
import com.github.gameringop.utils.items.ItemUtils.skyblockId
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.monster.Zombie
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.Vec3

object Carnival: Feature("Zombie Shooting Carnival Minigame") {

    private val clickDelay by SliderSetting<Long>("Click Delay", 100, 50, 500, 10)
        .withDescription("Dart tube shooting delay (ms).")

    private val rotationTime by SliderSetting<Long>("Rotation Time", 50, 0, 250, 5)
        .withDescription("Time to interpolate rotations to the target. Set to 0 to snap instantly.")

    private val predictionScale by SliderSetting<Long>("Prediction Scale", 8, 0, 30, 1)
        .withDescription("How far ahead to shoot moving targets. Higher = more lead change.")

    private var lastClick = 0L
    private var activeLampPos: Vec3? = null
    private var isRotating = false

    private data class TargetData(val pos: Vec3, val isBaby: Boolean)

    override fun init() {
        register<TickEvent.Start> {
            val player = mc.player ?: return@register
            if (player.mainHandItem.skyblockId != "CARNIVAL_DART_TUBE") return@register
            if (isRotating) return@register

            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClick < clickDelay.value) return@register

            val target = getTargets().firstOrNull() ?: return@register

            lastClick = currentTime + rotationTime.value
            isRotating = true

            queue(1) {
                try {
                    if (rotationTime.value > 0) {
                        rotateSmoothly(target, rotationTime.value) {
                            rightClick()
                        }
                    } else {
                        val rot = calcYawPitch(target)
                        rotate(rot.yaw, rot.pitch)
                        rightClick()
                    }
                } finally {
                    isRotating = false
                }
            }
        }

        register<BlockChangeEvent> {
            if (event.newBlock != Blocks.REDSTONE_LAMP && event.oldBlock != Blocks.REDSTONE_LAMP) return@register
            val pos = event.pos.immutable()

            ThreadUtils.setTimeout(20) {
                val player = mc.player ?: return@setTimeout
                val state = WorldUtils.getStateAt(pos)
                if (!state.hasProperty(BlockStateProperties.LIT)) return@setTimeout

                val isLit = state.getValue(BlockStateProperties.LIT)
                val posCenter = Vec3(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5)

                if (posCenter.distanceToSqr(player.position()) > 25.0 * 25.0) return@setTimeout

                val toLamp = posCenter.subtract(player.position()).normalize()

                if (player.lookAngle.dot(toLamp) < -0.5) return@setTimeout

                if (isLit) {
                    activeLampPos = posCenter
                } else if (activeLampPos == posCenter) {
                    activeLampPos = null
                }
            }
        }

        register<WorldChangeEvent> {
            activeLampPos = null
            isRotating = false
        }
    }

    private fun getTargets(): List<Vec3> {
        val player = mc.player ?: return emptyList()
        val level = mc.level ?: return emptyList()

        val sortedZombies = level.getEntitiesOfClass(Zombie::class.java, player.boundingBox.inflate(50.0)) { !it.isDeadOrDying }
            .sortedBy { it.distanceToSqr(player) }

        val zombies = sortedZombies.groupBy({ it.getItemBySlot(EquipmentSlot.HEAD).item }) { z ->
            val velX = z.x - z.xOld
            val velZ = z.z - z.zOld

            val distanceFactor = player.distanceTo(z) * 0.1
            val leadMultiplier = predictionScale.value * distanceFactor

            TargetData(
                Vec3(
                    z.x + (velX * leadMultiplier),
                    z.y + z.eyeHeight,
                    z.z + (velZ * leadMultiplier)
                ),
                z.isBaby
            )
        }

        fun MutableList<Vec3>.addTier(item: Item) {
            val tierZombies = zombies[item].orEmpty()
            addAll(tierZombies.filter { !it.isBaby }.map { it.pos })
            addAll(tierZombies.filter { it.isBaby }.map { it.pos })
        }

        return buildList {
            activeLampPos?.let { add(it) }
            addTier(Items.DIAMOND_HELMET)
            addTier(Items.GOLDEN_HELMET)
            addTier(Items.IRON_HELMET)
            addTier(Items.LEATHER_HELMET)
        }
    }
}