package com.github.gameringop.mixin;

import com.github.gameringop.event.EventBus;
import com.github.gameringop.event.impl.MouseClickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MixinMouseHandler {
    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
    private void onMouseButton(long l, MouseButtonInfo mouseButtonInfo, int i, CallbackInfo ci) {
        if (l != minecraft.getWindow().handle()) return;
        if (EventBus.post(new MouseClickEvent(mouseButtonInfo.button(), i, mouseButtonInfo.modifiers()))) {
            ci.cancel();
        }
    }
}