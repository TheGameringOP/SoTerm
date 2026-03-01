package com.github.gameringop.features.impl.dungeon

import com.github.gameringop.event.impl.KeyboardEvent
import com.github.gameringop.features.Feature
import com.github.gameringop.ui.clickgui.components.getValue
import com.github.gameringop.ui.clickgui.components.impl.KeybindSetting
import com.github.gameringop.ui.clickgui.components.impl.ToggleSetting
import com.github.gameringop.ui.clickgui.components.provideDelegate
import com.github.gameringop.ui.clickgui.components.section
import com.github.gameringop.ui.clickgui.components.showIf
import com.github.gameringop.utils.PlayerUtils.useDungeonClassAbility
import com.github.gameringop.utils.dungeons.DungeonListener
import com.github.gameringop.utils.location.LocationUtils
import org.lwjgl.glfw.GLFW

object AbilityKeybinds: Feature("Allows you do use your dungeon class ult/ability with a keybind.") {
    private val classUltimate by ToggleSetting("Class Ultimate", true)
    private val classAbility by ToggleSetting("Class Ability", true)
    private val ultKeybind by KeybindSetting("Ultimate Keybind").showIf { classUltimate.value }.section("keybinds")
    private val abilityKeybind by KeybindSetting("Ability Keybind").showIf { classAbility.value }

    override fun init() {
        register<KeyboardEvent.KeyPressed> {
            if (! LocationUtils.inDungeon || ! DungeonListener.dungeonStarted) return@register
            if (event.action != GLFW.GLFW_PRESS) return@register
            if (mc.screen != null) return@register

            if (classUltimate.value && ultKeybind.isPressed()) {
                event.isCanceled = true
                return@register useDungeonClassAbility(true)
            }

            if (classAbility.value && abilityKeybind.isPressed()) {
                event.isCanceled = true
                return@register useDungeonClassAbility(false)
            }
        }
    }
}