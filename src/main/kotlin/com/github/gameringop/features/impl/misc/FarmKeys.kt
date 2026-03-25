package com.github.gameringop.features.impl.misc

import com.github.gameringop.SoTerm
import com.github.gameringop.event.impl.KeyboardEvent
import com.github.gameringop.features.Feature
import com.github.gameringop.ui.clickgui.components.getValue
import com.github.gameringop.ui.clickgui.components.provideDelegate
import com.github.gameringop.ui.clickgui.components.impl.DropdownSetting
import com.github.gameringop.ui.clickgui.components.impl.KeybindSetting
import com.github.gameringop.ui.clickgui.components.impl.SliderSetting
import com.github.gameringop.utils.ChatUtils
import com.github.gameringop.utils.location.LocationUtils
import com.github.gameringop.utils.location.WorldType
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.KeyMapping
import net.minecraft.client.Options
import org.lwjgl.glfw.GLFW

object FarmKeys : Feature("Farm Keys") {

    private val blockBreakKey by KeybindSetting("Block break key", InputConstants.UNKNOWN.value)
    private val jumpKey by KeybindSetting("Jump key", InputConstants.UNKNOWN.value)
    private val previousSensitivity by SliderSetting("Previous sensitivity", 100f, 0f, 200f, 1f)
    private val isAuto by DropdownSetting("Attack Mode", 0, listOf("Hold", "Auto"))
    private val toggleKey by KeybindSetting("Toggle key", InputConstants.UNKNOWN.value)

    private var active = false
    private var previousAttackToggled = false

    fun isActive(): Boolean = active

    override fun init() {
        register<KeyboardEvent.KeyPressed> {
            if (LocationUtils.world != WorldType.Garden) return@register
            if (event.action != GLFW.GLFW_PRESS) return@register
            if (mc.screen != null) return@register

            if (SoTerm.debugFlags.contains("farm")) {
                ChatUtils.modMessage("§7Key pressed: ${event.keyEvent.key}, toggleKey value: ${toggleKey.value}")
            }

            if (event.keyEvent.key == toggleKey.value) {
                if (SoTerm.debugFlags.contains("farm")) {
                    ChatUtils.modMessage("§eToggle key detected!")
                }
                
                active = !active
                
                if (SoTerm.debugFlags.contains("farm")) {
                    ChatUtils.modMessage("§eFarm mode toggled: ${if (active) "ON" else "OFF"}")
                }
                
                if (active) {
                    if (SoTerm.debugFlags.contains("farm")) {
                        ChatUtils.modMessage("§aApplying farm keybinds...")
                    }
                    updateKeyBinding(mc.options.keyAttack, blockBreakKey.value)
                    updateKeyBinding(mc.options.keyJump, jumpKey.value)

                    previousAttackToggled = mc.options.toggleAttack().get()
                    
                    val wantToggle = (isAuto.value == 1)
                    mc.options.toggleAttack().set(wantToggle)
                    
                } else {
                    if (SoTerm.debugFlags.contains("farm")) {
                        ChatUtils.modMessage("§cRestoring original keybinds...")
                    }
                    mc.options.keyAttack.setKey(InputConstants.Type.MOUSE.getOrCreate(0))
                    mc.options.keyJump.setKey(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_SPACE))
                    
                    val internalSens = (previousSensitivity.value as Number).toDouble() / 200.0
                    mc.options.sensitivity().set(internalSens)
                    
                    mc.options.toggleAttack().set(previousAttackToggled)
                }
                
                mc.options.save()
                KeyMapping.resetMapping()
                event.isCanceled = true
            }
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
