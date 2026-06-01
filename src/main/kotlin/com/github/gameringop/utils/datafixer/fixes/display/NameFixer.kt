package com.github.gameringop.utils.datafixer.fixes.display

import com.github.gameringop.utils.datafixer.DataComponentFixer
import com.github.gameringop.utils.datafixer.LegacyTextFixer
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import kotlin.jvm.optionals.getOrNull

object NameFixer : DataComponentFixer<Component> {
    private const val DISPLAY_TAG = "display"
    private const val TAG = "Name"

    override val type: DataComponentType<Component> = DataComponents.CUSTOM_NAME

    override fun getData(tag: CompoundTag): Component? {
        val display = tag.getCompound(DISPLAY_TAG).getOrNull() ?: return null
        val name = display.getAndRemoveString(TAG) ?: return null
        tag.removeIfEmpty(DISPLAY_TAG)

        return LegacyTextFixer.parse(name)
    }
}