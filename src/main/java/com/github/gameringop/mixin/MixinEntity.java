package com.github.gameringop.mixin;

import com.github.gameringop.event.EventBus;
import com.github.gameringop.event.impl.CheckEntityGlowEvent;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class MixinEntity {
    @Unique private int customGlowColor = 0xFFFFFF;
    @Unique private boolean glowForced = false;

    @Inject(method = "isCurrentlyGlowing", at = @At("HEAD"), cancellable = true)
    private void onIsCurrentlyGlowing(CallbackInfoReturnable<Boolean> cir) {
        Entity entity = (Entity) (Object) this;

        CheckEntityGlowEvent event = new CheckEntityGlowEvent(entity);
        EventBus.post(event);

        this.glowForced = event.getShouldGlow();
        this.customGlowColor = event.getColor().getRGB();

        if (this.glowForced) cir.setReturnValue(true);
    }

    @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
    private void onGetTeamColorValue(CallbackInfoReturnable<Integer> cir) {
        if (this.glowForced) cir.setReturnValue(this.customGlowColor);
    }
}