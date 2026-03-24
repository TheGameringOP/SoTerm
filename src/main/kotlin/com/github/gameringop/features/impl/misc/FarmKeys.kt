package com.github.gameringop.features.impl.misc

import com.github.gameringop.features.Feature
import com.github.gameringop.ui.clickgui.components.getValue
import com.github.gameringop.ui.clickgui.components.impl.KeybindSetting
import com.github.gameringop.ui.clickgui.components.impl.SliderSetting
import com.github.gameringop.ui.clickgui.components.provideDelegate
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.KeyMapping

object FarmKeys: Feature("Farm Keys") {

    private val blockBreakKey by KeybindSetting("Block breaking", InputConstants.UNKNOWN.value)
        
    private val jumpKey by KeybindSetting("Jump", InputConstants.UNKNOWN.value)
        
    private var previousSensitivity by SliderSetting("Previous Sensitivity", 100f, 0f, 200f, 1f)

    private var originalAttackKey: InputConstants.Key? = null
    private var originalJumpKey: InputConstants.Key? = null

    override fun onEnable() {
        super.onEnable()
        
        originalAttackKey = mc.options.keyAttack.key
        originalJumpKey = mc.options.keyJump.key
        
        updateKeyBinding(mc.options.keyAttack, blockBreakKey)
        updateKeyBinding(mc.options.keyJump, jumpKey)
        
        mc.options.sensitivity().set(-1.0 / 3.0)
        
        KeyMapping.resetMapping()
    }

    override fun onDisable() {
        super.onDisable()

        val attackRestore = originalAttackKey ?: InputConstants.Type.MOUSE.getOrCreate(0) 
        val jumpRestore = originalJumpKey ?: InputConstants.Type.KEYSYM.getOrCreate(32)
        
        mc.options.keyAttack.setKey(attackRestore)
        mc.options.keyJump.setKey(jumpRestore)
        
        val internalSens = previousSensitivity.toDouble() / 200.0
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
