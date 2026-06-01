package com.github.gameringop.features.impl.misc

import com.github.gameringop.features.Feature
import com.github.gameringop.ui.clickgui.components.impl.ToggleSetting

object NameTagTweaks: Feature(name = "Nametag Tweaks") {
    @JvmStatic
    val disableNametagBackground by ToggleSetting("Hide Nametag Background").withDescription("Disable Nametag's black background.")

    @JvmStatic
    val addNameTagTextShadow by ToggleSetting("Shadowed Nametag").withDescription("Adds a text shadow to the nametag label.")
}