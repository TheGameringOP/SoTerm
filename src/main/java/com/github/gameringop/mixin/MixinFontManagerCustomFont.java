package com.github.gameringop.mixin;

import com.github.gameringop.features.impl.dev.CustomFont;
import net.minecraft.client.gui.font.FontManager;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.Map;

@Mixin(FontManager.class)
public class MixinFontManagerCustomFont {
    @Shadow private Map<Identifier, FontSet> fontSets;

    @Inject(method = "apply", at = @At("TAIL"))
    private void soterm$applyCustomFont(CallbackInfo ci) {CustomFont.applyToFontSets(this.fontSets);}
}