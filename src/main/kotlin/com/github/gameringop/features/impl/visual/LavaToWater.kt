//package com.github.gameringop.features.impl.visual
//
//import com.github.gameringop.SoTerm.logger
//import com.github.gameringop.features.Feature
//import com.github.gameringop.utils.ThreadUtils
//import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderingRegistry
//import net.fabricmc.fabric.impl.client.rendering.fluid.FluidRenderingRegistryImpl
//import net.minecraft.client.renderer.block.FluidModel
//import net.minecraft.world.level.material.Fluids
//
//object LavaToWater: Feature("Replaces lava with the water texture and water fog (resource-pack aware).") {
//    private var lavaStillModel: FluidModel.Unbaked? = null
//    private var lavaFlowModel: FluidModel.Unbaked? = null
//    private var waterStillModel: FluidModel.Unbaked? = null
//    private var waterFlowModel: FluidModel.Unbaked? = null
//
//    override fun init() {
//        captureModels()
//    }
//
//    override fun onEnable() {
//        super.onEnable()
//        applyAppearance()
//    }
//
//    override fun onDisable() {
//        super.onDisable()
//        restoreAppearance()
//    }
//
//    private fun captureModels() {
//        val models = FluidRenderingRegistryImpl.getUnbakedModels()
//        lavaStillModel = models[Fluids.LAVA]
//        lavaFlowModel = models[Fluids.FLOWING_LAVA]
//        waterStillModel = models[Fluids.WATER]
//        waterFlowModel = models[Fluids.FLOWING_WATER]
//
//        if (lavaStillModel == null || waterStillModel == null) {
//            logger.warn("LavaToWater: fluid models not ready yet, will retry when enabled")
//        }
//    }
//
//    private fun applyAppearance() {
//        if (lavaStillModel == null || waterStillModel == null) captureModels()
//
//        val still = waterStillModel ?: return
//        val flow = waterFlowModel ?: still
//        FluidRenderingRegistry.register(Fluids.LAVA, still)
//        FluidRenderingRegistry.register(Fluids.FLOWING_LAVA, flow)
//        refreshChunks()
//    }
//
//    private fun restoreAppearance() {
//        if (lavaStillModel == null) captureModels()
//
//        val still = lavaStillModel ?: return
//        val flow = lavaFlowModel ?: still
//        FluidRenderingRegistry.register(Fluids.LAVA, still)
//        FluidRenderingRegistry.register(Fluids.FLOWING_LAVA, flow)
//        refreshChunks()
//    }
//
//    private fun refreshChunks() {
//        ThreadUtils.runOnMcThread {
//            mc.levelRenderer.allChanged()
//        }
//    }
//}
