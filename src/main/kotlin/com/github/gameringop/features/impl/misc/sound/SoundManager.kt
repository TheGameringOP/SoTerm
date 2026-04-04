package com.github.gameringop.features.impl.misc.sound

import com.github.gameringop.SoTerm
import com.github.gameringop.config.PogObject
import com.github.gameringop.features.Feature
import com.github.gameringop.ui.clickgui.components.getValue
import com.github.gameringop.ui.clickgui.components.impl.ButtonSetting
import com.github.gameringop.ui.clickgui.components.provideDelegate
import com.github.gameringop.ui.gui.SoundManagerScreen

object SoundManager: Feature("Adjust volumes for every sound in the game") {
    val volumes = PogObject("SoTerm_sounds", mutableMapOf<String, Float>())

    val btn by ButtonSetting("Open SoundManager GUI") {
        SoTerm.screen = SoundManagerScreen()
    }

    @JvmStatic
    fun getMultiplier(id: String): Float {
        if (! enabled) return 1.0f
        return volumes.getData().getOrDefault(id, 1f)
    }
}