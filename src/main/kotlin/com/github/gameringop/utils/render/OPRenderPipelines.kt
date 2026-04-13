package com.github.gameringop.utils.render

import com.github.gameringop.SoTerm
import com.github.gameringop.utils.render.iris.IrisCompatibility
import com.github.gameringop.utils.render.iris.IrisShaderType
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.DepthTestFunction
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.ResourceLocation

object OPRenderPipelines {
    val LINES_THROUGH_WALLS: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
            .withLocation(id("lines_through_walls"))
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .build()
    )

    val FILLED_THROUGH_WALLS: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(id("filled_through_walls"))
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLE_STRIP)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .build()
    )

    fun init() {
        IrisCompatibility.registerPipeline(LINES_THROUGH_WALLS, IrisShaderType.LINES)
        IrisCompatibility.registerPipeline(FILLED_THROUGH_WALLS, IrisShaderType.BASIC)
    }

    private fun id(path: String) = ResourceLocation.fromNamespaceAndPath(SoTerm.MOD_ID, path)
}