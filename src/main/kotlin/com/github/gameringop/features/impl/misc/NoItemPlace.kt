package com.github.gameringop.features.impl.misc

import com.github.gameringop.features.Feature
import com.github.gameringop.utils.ChatUtils.unformattedText
import com.github.gameringop.utils.Utils.endsWithOneOf
import com.github.gameringop.utils.Utils.equalsOneOf
import com.github.gameringop.utils.Utils.startsWithOneOf
import com.github.gameringop.utils.dungeons.enums.WitherRelic
import com.github.gameringop.utils.items.ItemUtils.skyblockId
import com.github.gameringop.utils.location.LocationUtils
import net.minecraft.world.item.context.BlockPlaceContext

/**
 * @see com.github.gameringop.mixin.MixinBlockItem
 */
object NoItemPlace: Feature("Stops you from placing skull blocks/items.") {

    @JvmStatic
    fun placeHook(context: BlockPlaceContext): Boolean {
        if (! enabled) return false
        val item = context.player?.mainHandItem ?: return false
        val name = item.hoverName.unformattedText
        val id = item.skyblockId

        return when {
            LocationUtils.F7Phase == 5 && WitherRelic.fromName(name) != null -> true

            id.startsWithOneOf("ABIPHONE") -> true

            id.endsWithOneOf(
                "_TUBA",
                "_POWER_ORB",
                "_POCKET_BLACK_HOLE"
            ) -> true

            else -> id.equalsOneOf(
                "BOUQUET_OF_LIES",
                "FLOWER_OF_TRUTH",
                "BAT_WAND",
                "STARRED_BAT_WAND",
                "INFINITE_SPIRIT_LEAP",
                "ROYAL_PIGEON",
                "ARROW_SWAPPER",
                "JINGLE_BELLS",
                "FIRE_FREEZE_STAFF",
                "UMBERELLA",
            )
        }
    }
}