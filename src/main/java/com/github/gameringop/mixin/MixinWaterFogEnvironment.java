//package com.github.gameringop.mixin;
//
//import com.github.gameringop.features.impl.visual.LavaToWater;
//import net.minecraft.client.renderer.fog.environment.WaterFogEnvironment;
//import net.minecraft.world.entity.Entity;
//import net.minecraft.world.level.material.FogType;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//
//@Mixin(WaterFogEnvironment.class)
//public abstract class MixinWaterFogEnvironment {
//    @Inject(method = "isApplicable", at = @At("HEAD"), cancellable = true)
//    private void soterm$lavaUsesWaterFog(FogType fogType, Entity entity, CallbackInfoReturnable<Boolean> cir) {
//        if (LavaToWater.INSTANCE.enabled && fogType == FogType.LAVA) {
//            cir.setReturnValue(true);
//        }
//    }
//}
