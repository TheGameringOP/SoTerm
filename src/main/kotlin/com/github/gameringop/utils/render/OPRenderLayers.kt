package com.github.gameringop.utils.render


import net.minecraft.client.renderer.rendertype.RenderSetup
import net.minecraft.client.renderer.rendertype.RenderType

object OPRenderLayers {
    val FILLED = RenderType.create("op_filled", RenderSetup.builder(OPRenderPipelines.FILLED).createRenderSetup())
    val FILLED_THROUGH_WALLS = RenderType.create("op_filled_through_walls", RenderSetup.builder(OPRenderPipelines.FILLED_THROUGH_WALLS).createRenderSetup())

    val CIRCLE_FILLED = RenderType.create("op_circle_filled", RenderSetup.builder(OPRenderPipelines.CIRCLE_FILLED).createRenderSetup())
    val CIRCLE_FILLED_THROUGH_WALLS = RenderType.create("op_circle_filled_through_walls", RenderSetup.builder(OPRenderPipelines.CIRCLE_FILLED_THROUGH_WALLS).createRenderSetup())

    val LINES = RenderType.create("op_lines", RenderSetup.builder(OPRenderPipelines.LINES).createRenderSetup())
    val LINES_THROUGH_WALLS = RenderType.create("op_lines_through_walls", RenderSetup.builder(OPRenderPipelines.LINES_THROUGH_WALLS).createRenderSetup())
}