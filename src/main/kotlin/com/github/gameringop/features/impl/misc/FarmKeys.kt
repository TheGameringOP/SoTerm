package com.github.gameringop.features.impl.misc

import com.github.gameringop.features.Feature
import com.github.gameringop.ui.clickgui.components.getValue
import com.github.gameringop.ui.clickgui.components.provideDelegate
import com.github.gameringop.ui.clickgui.components.impl.KeybindSetting
import com.github.gameringop.ui.clickgui.components.impl.SliderSetting
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.KeyMapping
import org.lwjgl.glfw.GLFW

object FarmKeys : Feature("Farm Keys") {

    val blockBreakKey by KeybindSetting("Block breaking", InputConstants.UNKNOWN.value)
    val jumpKey by KeybindSetting("Jump", InputConstants.UNKNOWN.value)
    val previousSensitivity by SliderSetting("Previous Sensitivity", 100f, 0f, 200f, 1f)
    val toggleKey by KeybindSetting("Toggle Key", InputConstants.UNKNOWN.value)

    override fun init() {
        register<TickEvent.Server> {
        }
    }

    override fun onEnable() {
        updateKeyBinding(mc.options.keyAttack, blockBreakKey.value)
        updateKeyBinding(mc.options.keyJump, jumpKey.value)

        mc.options.sensitivity().set(-1.0 / 3.0)
        KeyMapping.resetMapping()
    }

    override fun onDisable() {
        mc.options.keyAttack.setKey(InputConstants.Type.MOUSE.getOrCreate(0))
        mc.options.keyJump.setKey(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_SPACE))

        val internalSens = (previousSensitivity.value as Number).toDouble() / 200.0
        mc.options.sensitivity().set(internalSens)

        KeyMapping.resetMapping()
    }

    private fun updateKeyBinding(keyMapping: KeyMapping, bindValue: Int) {
        if (bindValue == InputConstants.UNKNOWN.value) return
        
        keyMapping.setDown(false)

        val newKey = if (bindValue < 8) {
            InputConstants.Type.MOUSE.getOrCreate(bindValue)
        } else {
            InputConstants.Type.KEYSYM.getOrCreate(bindValue)
        }
        
        keyMapping.setKey(newKey)
    }
}
