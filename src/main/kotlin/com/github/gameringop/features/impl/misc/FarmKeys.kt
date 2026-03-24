package com.github.gameringop.features.impl.misc

import com.github.gameringop.event.impl.TickEvent
import com.github.gameringop.features.Feature
import com.github.gameringop.ui.clickgui.components.getValue
import com.github.gameringop.ui.clickgui.components.provideDelegate
import com.github.gameringop.ui.clickgui.components.impl.KeybindSetting
import com.github.gameringop.ui.clickgui.components.impl.SliderSetting
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.KeyMapping
import org.lwjgl.glfw.GLFW

object FarmKeys : Feature("Farm Keys") {

    val blockBreakKey by KeybindSetting("Block break key", InputConstants.UNKNOWN.value)
    val jumpKey by KeybindSetting("Jump key", InputConstants.UNKNOWN.value)
    val previousSensitivity by SliderSetting("Previous sensitivity", 100f, 0f, 200f, 1f)
    val toggleKey by KeybindSetting("Toggle key", InputConstants.UNKNOWN.value)

    private var active = false
    private var wasPressed = false

    override fun init() {
        register<TickEvent.Client> {
            val isPressed = toggleKey.isPressed()

            if (isPressed && !wasPressed) {
                active = !active
                
                if (active) {
                    updateKeyBinding(mc.options.keyAttack, blockBreakKey)
                    updateKeyBinding(mc.options.keyJump, jumpKey)
                    mc.options.sensitivity().set(-1.0 / 3.0)
                } else {
                    mc.options.keyAttack.setKey(InputConstants.Type.MOUSE.getOrCreate(0))
                    mc.options.keyJump.setKey(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_SPACE))
                    
                    val internalSens = (previousSensitivity as Number).toDouble() / 200.0
                    mc.options.sensitivity().set(internalSens)
                }
                
                KeyMapping.resetMapping()
            }
            
            wasPressed = isPressed
        }
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
