package com.github.gameringop.features.impl.misc

import com.github.gameringop.features.Feature
import com.github.gameringop.ui.clickgui.components.getValue
import com.github.gameringop.ui.clickgui.components.provideDelegate
import com.github.gameringop.ui.clickgui.components.impl.KeybindSetting
import com.github.gameringop.ui.clickgui.components.impl.SliderSetting
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.KeyMapping
import org.lwjgl.glfw.GLFW

object FarmKeys : Feature("Helps you farm") {

    private val blockBreakKey by KeybindSetting("Block breaking", InputConstants.UNKNOWN.value)
    private val jumpKey by KeybindSetting("Jump", InputConstants.UNKNOWN.value)
    private val previousSensitivity by SliderSetting("Previous Sensitivity", 100f, 0f, 200f, 1f)
    private val toggleKey by KeybindSetting("Toggle Key", InputConstants.UNKNOWN.value)

    override fun init() {
    }

    override fun onEnable() {
        val breakSetting = getSettingByName("Block breaking") as? KeybindSetting
        val jumpSetting = getSettingByName("Jump") as? KeybindSetting

        breakSetting?.let { updateKeyBinding(mc.options.keyAttack, it) }
        jumpSetting?.let { updateKeyBinding(mc.options.keyJump, it) }

        mc.options.sensitivity().set(-1.0 / 3.0)
        
        KeyMapping.resetMapping()
    }

    override fun onDisable() {
        mc.options.keyAttack.setKey(InputConstants.Type.MOUSE.getOrCreate(0))
        mc.options.keyJump.setKey(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_SPACE))

        val internalSens = (previousSensitivity as Number).toDouble() / 200.0
        mc.options.sensitivity().set(internalSens)

        KeyMapping.resetMapping()
    }

    private fun updateKeyBinding(keyMapping: KeyMapping, customBind: KeybindSetting) {
        if (customBind.value == InputConstants.UNKNOWN.value) return
        
        keyMapping.setDown(false)
        
        val newKey = if (customBind.isMouse) {
            InputConstants.Type.MOUSE.getOrCreate(customBind.value)
        } else {
            InputConstants.Type.KEYSYM.getOrCreate(customBind.value)
        }
        
        keyMapping.setKey(newKey)
    }
}
