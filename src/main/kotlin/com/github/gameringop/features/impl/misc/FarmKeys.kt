package com.github.gameringop.features.impl.misc

import com.github.gameringop.features.Feature
import com.github.gameringop.mixin.IKeyMapping
import com.github.gameringop.ui.clickgui.components.impl.KeybindSetting
import com.github.gameringop.ui.clickgui.components.impl.SliderSetting
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.KeyMapping
import com.github.gameringop.ui.clickgui.components.getValue
import com.github.gameringop.ui.clickgui.components.provideDelegate

object FarmKeys : Feature("Farm Keys") {

    private val blockBreakKey by KeybindSetting("Block breaking", InputConstants.UNKNOWN.value).also { configSettings.add(it) }
    private val jumpKey by KeybindSetting("Jump", InputConstants.UNKNOWN.value).also { configSettings.add(it) }
    private val previousSensitivity by SliderSetting("Previous Sensitivity", 100f, 0f, 200f, 1f).also { configSettings.add(it) }

    private var originalAttackKey: InputConstants.Key? = null
    private var originalJumpKey: InputConstants.Key? = null

    override fun onEnable() {
        val attackMapping = mc.options.keyAttack as IKeyMapping
        val jumpMapping = mc.options.keyJump as IKeyMapping
        
        originalAttackKey = attackMapping.key
        originalJumpKey = jumpMapping.key
        
        val breakSetting = getSettingByName("Block breaking") as? KeybindSetting
        val jumpSetting = getSettingByName("Jump") as? KeybindSetting
        
        breakSetting?.let { updateKeyBinding(mc.options.keyAttack, it) }
        jumpSetting?.let { updateKeyBinding(mc.options.keyJump, it) }
        
        mc.options.sensitivity().set(-1.0 / 3.0)
        
        KeyMapping.resetMapping()
    }

    override fun onDisable() {
        val attackRestore = originalAttackKey ?: InputConstants.Type.MOUSE.getOrCreate(0) 
        val jumpRestore = originalJumpKey ?: InputConstants.Type.KEYSYM.getOrCreate(GLFW_KEY_SPACE)
        
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
