package com.github.gameringop.features.impl.misc.sound

import com.github.gameringop.SoTerm
import com.github.gameringop.config.PogObject
import com.github.gameringop.features.Feature
import com.github.gameringop.ui.clickgui.components.impl.ButtonSetting
import com.github.gameringop.ui.gui.SoundManagerScreen

object SoundManager: Feature("Adjust volumes for every sound in the game") {
    var volumes by PogObject("soterm_sounds", mutableMapOf<String, Float>())

    val btn by ButtonSetting("Open SoundManager GUI") {
        SoTerm.screen = SoundManagerScreen()
    }

    @JvmStatic
    fun getMultiplier(id: String): Float {
        if (! enabled) return 1.0f
        return volumes.getOrDefault(id, 1f)
    }
}