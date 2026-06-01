package com.github.gameringop.mixin;

import com.github.gameringop.features.impl.floor7.terminals.TerminalListener;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.github.gameringop.SoTerm.mc;

@Pseudo
@Mixin(targets = "mezz.jei.fabric.startup.EventRegistration", remap = false)
public abstract class MixinJei {
    @Dynamic
    @Inject(method = "registerScreenEvents", at = @At("HEAD"), cancellable = true)
    public void cancelEventsInTerm(CallbackInfo ci) {
        var screen = mc.screen instanceof ContainerScreen ? ((ContainerScreen) mc.screen) : null;
        if (screen == null) return;
        if (TerminalListener.inTerm) ci.cancel();
    }
}