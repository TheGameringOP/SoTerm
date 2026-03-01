package com.github.gameringop.utils.render

import com.github.gameringop.SoTerm.mc
import com.mojang.blaze3d.vertex.PoseStack
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext
import net.minecraft.client.Camera
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.shapes.VoxelShape

data class RenderContext(
    val matrixStack: PoseStack?,
    val consumers: MultiBufferSource?,
    val camera: Camera,
    var blockPos: BlockPos? = null,
    var voxelShape: VoxelShape? = null
) {
    companion object {
        fun fromContext(ctx: WorldRenderContext): RenderContext {
            return RenderContext(
                matrixStack = ctx.matrices(),
                camera = mc.gameRenderer.mainCamera,
                consumers = ctx.consumers(),
                blockPos = null,
                voxelShape = null
            )
        }
    }
}