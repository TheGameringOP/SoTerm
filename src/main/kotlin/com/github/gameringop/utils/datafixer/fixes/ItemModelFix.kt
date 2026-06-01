package com.github.gameringop.utils.datafixer.fixes

import com.github.gameringop.utils.datafixer.DataComponentFixer
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import java.util.concurrent.ConcurrentHashMap

object ItemModelFix : DataComponentFixer<ResourceLocation> {
    private val cache = ConcurrentHashMap<String, ResourceLocation>()
    override val type: DataComponentType<ResourceLocation> = DataComponents.ITEM_MODEL

    override fun getData(tag: CompoundTag) = tag.getAndRemoveString("ItemModel")?.let {
        cache.getOrPut(it) { ResourceLocation.tryParse(it) }
    }
}