package com.github.gameringop.utils.render

import com.github.gameringop.SoTerm.mc
import com.mojang.blaze3d.vertex.PoseStack
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext
import net.minecraft.client.Camera
import net.minecraft.client.renderer.MultiBufferSource

data class RenderContext(val matrixStack: PoseStack, val consumers: MultiBufferSource, val camera: Camera) {
    companion object {
        fun fromContext(ctx: WorldRenderContext): RenderContext {
            return RenderContext(ctx.matrices(), ctx.consumers(), mc.gameRenderer.mainCamera)
        }
    }
}
