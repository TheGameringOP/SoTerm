package com.github.gameringop.ui.clickgui.modmenu

import com.github.gameringop.ui.clickgui.ClickGuiScreen
import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi

class ModMenuIntegration : ModMenuApi {
    override fun getModConfigScreenFactory(): ConfigScreenFactory<*> {
        return ConfigScreenFactory { parent -> ClickGuiScreen }
    }
}