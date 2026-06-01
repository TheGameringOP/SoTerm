package com.github.gameringop.utils.datafixer

import com.github.gameringop.utils.datafixer.base.BaseItem
import com.github.gameringop.utils.datafixer.fixes.*
import com.github.gameringop.utils.datafixer.fixes.display.ColorFixer
import com.github.gameringop.utils.datafixer.fixes.display.LoreFixer
import com.github.gameringop.utils.datafixer.fixes.display.NameFixer
import com.github.gameringop.utils.datafixer.utils.getStringOrNull
import com.github.gameringop.utils.datafixer.utils.holder
import com.github.gameringop.utils.datafixer.utils.toJson
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtUtils
import net.minecraft.nbt.Tag
import net.minecraft.world.item.ItemStack

object LegacyDataFixer {
    private val fixers = listOf(
        HideFlagsFixer,
        SkullTextureFixer,
        LoreFixer,
        NameFixer,
        ColorFixer,
        UnbreakableFixer,
        EnchantGlintFixer,
        WrittenBookFixer,
        BannerItemFixer,
        ExtraAttributesFixer,
        FireworkExplosionFixer,
        ItemModelFix,
        RemoveFixer("overrideMeta"),
        RemoveFixer("AttributeModifiers"),
    )

    fun fromTag(tag: Tag): ItemStack? {
        if (tag !is CompoundTag) {
            return ItemStack.EMPTY
        }

        if (tag.isEmpty) return ItemStack.EMPTY

        val base = BaseItem.getBase(tag)

        if (base == null) {
            MeowddingItemDfu.error(
                "Base item not found for ${tag.getStringOrNull("id")} (${tag.getStringOrNull("Damage")})\n${
                    NbtUtils.prettyPrint(
                        tag
                    )
                }"
            )
            return null
        }

        val (item, count, builder) = base

        tag.getCompound("tag").ifPresent { tag ->
            fixers.forEach {
                if (!it.canApply(item)) return@forEach
                it.apply(builder, tag)
            }
        }

        val stack = ItemStack(item.holder, count, builder.build())

        if (MeowddingItemDfu.logErrors && !tag.isEmpty && !tag.getCompoundOrEmpty("tag").isEmpty) {
            MeowddingItemDfu.warn(
                """
            Item tag is not empty after applying fixers for ${stack.get(DataComponents.CUSTOM_DATA)?.copyTag()?.getString("id")}:
            ${NbtUtils.prettyPrint(tag)}
            ${stack.toJson(ItemStack.CODEC)}
            """.trimIndent(),
            )
        }

        return stack
    }
}