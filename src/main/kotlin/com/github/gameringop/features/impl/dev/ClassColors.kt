package com.github.gameringop.features.impl.dev

import com.github.gameringop.features.Feature
import com.github.gameringop.features.annotations.AlwaysActive
import com.github.gameringop.ui.clickgui.components.Setting
import com.github.gameringop.ui.clickgui.components.impl.ButtonSetting
import com.github.gameringop.ui.clickgui.components.impl.ColorCodeSetting
import net.minecraft.ChatFormatting

@AlwaysActive
object ClassColors: Feature("Allows setting custom color for every dungeon class", toggled = true) {
    val archCode by ColorCodeSetting("Archer Code", ChatFormatting.GOLD).section("Colors")
    val bersCode by ColorCodeSetting("Berserk Code", ChatFormatting.DARK_RED)
    val healCode by ColorCodeSetting("Healer Code", ChatFormatting.LIGHT_PURPLE)
    val mageCode by ColorCodeSetting("Mage Code", ChatFormatting.AQUA)
    val tankCode by ColorCodeSetting("Tank Code", ChatFormatting.DARK_GREEN)
    val emptyCode = ColorCodeSetting("Empty Code", ChatFormatting.BLACK)

    private val reset by ButtonSetting("Reset Colors") {
        configSettings.forEach(Setting<*>::reset)
    }

    override fun toggle() {}
}