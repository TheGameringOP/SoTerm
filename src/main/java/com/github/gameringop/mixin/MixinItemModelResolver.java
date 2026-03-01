package com.github.gameringop.mixin;

import com.github.gameringop.features.impl.visual.RevertAxes;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Objects;

@Mixin(ItemModelResolver.class)
public class MixinItemModelResolver {
    @ModifyVariable(method = "updateForLiving", at = @At("HEAD"), argsOnly = true)
    private ItemStack revertAxe(ItemStack original) {
        if (original == null || original.isEmpty()) return original;
        ItemStack replacement = RevertAxes.shouldReplace(original);
        return Objects.requireNonNullElse(replacement, original);
    }
}