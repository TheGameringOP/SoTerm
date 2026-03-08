// package com.github.gameringop.mixin;

// import com.github.gameringop.features.impl.visual.AlwaysNameTags;
// import net.minecraft.world.entity.Entity;
// import org.spongepowered.asm.mixin.Mixin;
// import org.spongepowered.asm.mixin.injection.At;
// import org.spongepowered.asm.mixin.injection.Inject;
// import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// @Mixin(Entity.class)
// public abstract class MixinNameTag {

//     @Inject(method = "shouldShowName", at = @At("HEAD"), cancellable = true)
//     private void onShouldShowName(CallbackInfoReturnable<Boolean> cir) {
//         if (AlwaysNameTags.INSTANCE.getEnabled()) { 
//             Entity entity = (Entity) (Object) this;
//             if (entity.hasCustomName()) {
//                 cir.setReturnValue(true);
//             }
//         }
//     }

//     @Inject(method = "isCustomNameVisible", at = @At("HEAD"), cancellable = true)
//     private void onIsCustomNameVisible(CallbackInfoReturnable<Boolean> cir) {
//         if (AlwaysNameTags.INSTANCE.getEnabled()) {
//             Entity entity = (Entity) (Object) this;
//             if (entity.hasCustomName()) {
//                 cir.setReturnValue(true);
//             }
//         }
//     }
// }
