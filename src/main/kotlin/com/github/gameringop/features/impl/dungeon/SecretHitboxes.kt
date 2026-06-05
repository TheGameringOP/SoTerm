package com.github.gameringop.features.impl.dungeon

import com.github.gameringop.features.Feature
import com.github.gameringop.ui.clickgui.components.impl.SliderSetting
import com.github.gameringop.ui.clickgui.components.impl.ToggleSetting
import com.github.gameringop.utils.location.LocationUtils
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock
import net.minecraft.world.level.block.WallSkullBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.AttachFace
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape

object SecretHitboxes : Feature("Changes the hitboxes of secret blocks to be larger.") {

    @JvmStatic val lever by ToggleSetting("Lever", true).section("Levers")
    private val leverSize by SliderSetting("Lever Size", 1.0, 0.0, 1.0, 0.05).showIf { lever.value }

    @JvmStatic val button by ToggleSetting("Button", true).section("Buttons")
    private val buttonSize by SliderSetting("Button Size", 1.0, 0.0, 1.0, 0.05).showIf { button.value }

    @JvmStatic val skull by ToggleSetting("Skulls", true).section("Skulls")
    private val skullSize by SliderSetting("Skull Size", 1.0, 0.0, 1.0, 0.05).showIf { skull.value }

    @JvmStatic val mushroom by ToggleSetting("Mushroom", true).section("Mushrooms")
    private val mushroomSize by SliderSetting("Mushroom Size", 1.0, 0.0, 1.0, 0.05).showIf { mushroom.value }

    private const val BUTTON_THICKNESS = 0.125

    private fun getInflatedShape(face: AttachFace, direction: Direction, size: Double): VoxelShape {
        val min = 0.5 - (size / 2.0)
        val max = 0.5 + (size / 2.0)
        return when (face) {
            AttachFace.CEILING -> Shapes.box(min, 1.0 - size, min, max, 1.0, max)
            AttachFace.FLOOR -> Shapes.box(min, 0.0, min, max, size, max)
            AttachFace.WALL -> when (direction) {
                Direction.NORTH -> Shapes.box(min, min, 1.0 - size, max, max, 1.0)
                Direction.SOUTH -> Shapes.box(min, min, 0.0, max, max, size)
                Direction.WEST -> Shapes.box(1.0 - size, min, min, 1.0, max, max)
                Direction.EAST -> Shapes.box(0.0, min, min, size, max, max)
                else -> Shapes.block()
            }
        }
    }

    private fun getButtonShape(face: AttachFace, direction: Direction, size: Double): VoxelShape {
        val s = size.coerceIn(0.01, 1.0)
        val min = 0.5 - (s / 2.0)
        val max = 0.5 + (s / 2.0)
        val thickness = BUTTON_THICKNESS
        return when (face) {
            AttachFace.CEILING -> Shapes.box(min, 1.0 - thickness, min, max, 1.0, max)
            AttachFace.FLOOR -> Shapes.box(min, 0.0, min, max, thickness, max)
            AttachFace.WALL -> when (direction) {
                Direction.NORTH -> Shapes.box(min, min, 1.0 - thickness, max, max, 1.0)
                Direction.SOUTH -> Shapes.box(min, min, 0.0, max, max, thickness)
                Direction.WEST -> Shapes.box(1.0 - thickness, min, min, 1.0, max, max)
                Direction.EAST -> Shapes.box(0.0, min, min, thickness, max, max)
                else -> Shapes.block()
            }
        }
    }

    @JvmStatic
    fun getButtonShape(state: BlockState): VoxelShape {
        val face = state.getValue(FaceAttachedHorizontalDirectionalBlock.FACE)
        val direction = state.getValue(FaceAttachedHorizontalDirectionalBlock.FACING)
        return getButtonShape(face, direction, buttonSize.value)
    }

    @JvmStatic
    fun getLeverShape(state: BlockState): VoxelShape {
        val face = state.getValue(FaceAttachedHorizontalDirectionalBlock.FACE)
        val direction = state.getValue(FaceAttachedHorizontalDirectionalBlock.FACING)
        return getInflatedShape(face, direction, leverSize.value)
    }

    @JvmStatic
    fun getSkullShape(state: BlockState): VoxelShape {
        return if (state.block is WallSkullBlock) {
            getInflatedShape(AttachFace.WALL, state.getValue(WallSkullBlock.FACING), skullSize.value)
        } else {
            getInflatedShape(AttachFace.FLOOR, Direction.UP, skullSize.value)
        }
    }

    @JvmStatic
    fun getMushroomShape(state: BlockState): VoxelShape {
        return getInflatedShape(AttachFace.FLOOR, Direction.UP, mushroomSize.value)
    }

    private val blackListedLevers = listOf(
        BlockPos(61, 136, 142), BlockPos(60, 136, 142), BlockPos(59, 136, 142),
        BlockPos(62, 135, 142), BlockPos(61, 135, 142), BlockPos(59, 135, 142),
        BlockPos(58, 135, 142), BlockPos(62, 134, 142), BlockPos(61, 134, 142),
        BlockPos(59, 134, 142), BlockPos(58, 134, 142), BlockPos(61, 133, 142),
        BlockPos(60, 133, 142), BlockPos(59, 133, 142)
    )

    @JvmStatic
    fun isValidLever(pos: BlockPos): Boolean {
        if (!enabled || !lever.value) return false
        if (pos in blackListedLevers && LocationUtils.dungeonFloorNumber == 7) return false
        return true
    }
}