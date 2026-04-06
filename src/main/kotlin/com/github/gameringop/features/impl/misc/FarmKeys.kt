package com.github.gameringop.features.impl.misc

import com.github.gameringop.SoTerm
import com.github.gameringop.event.impl.KeyboardEvent
import com.github.gameringop.event.impl.TickEvent
import com.github.gameringop.features.Feature
import com.github.gameringop.ui.clickgui.components.getValue
import com.github.gameringop.ui.clickgui.components.impl.DropdownSetting
import com.github.gameringop.ui.clickgui.components.impl.KeybindSetting
import com.github.gameringop.ui.clickgui.components.impl.SliderSetting
import com.github.gameringop.ui.clickgui.components.impl.ToggleSetting
import com.github.gameringop.ui.clickgui.components.provideDelegate
import com.github.gameringop.ui.clickgui.components.showIf
import com.github.gameringop.utils.ChatUtils
import com.github.gameringop.utils.location.LocationUtils
import com.github.gameringop.utils.location.WorldType
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.KeyMapping
import org.lwjgl.glfw.GLFW

object FarmKeys: Feature("Farm Keys") {

    private val blockBreakKey by KeybindSetting("Block break key", InputConstants.UNKNOWN.value)
    private val jumpKey by KeybindSetting("Jump key", InputConstants.UNKNOWN.value)
    private val previousSensitivity by SliderSetting("Previous sensitivity", 100f, 0f, 200f, 1f)
    private val isAuto by DropdownSetting("Attack Mode", 0, listOf("Hold", "Auto"))
    private val toggleKey by KeybindSetting("Toggle key", InputConstants.UNKNOWN.value)
    private val autoDirectionToggle by ToggleSetting("Auto Direction", false)

    private val alwaysRightKey by KeybindSetting("Always Right Key", InputConstants.UNKNOWN.value).showIf { autoDirectionToggle.value }
    private val alwaysLeftKey by KeybindSetting("Always Left Key", InputConstants.UNKNOWN.value).showIf { autoDirectionToggle.value }
    private val alwaysForwardKey by KeybindSetting("Always Forward Key", InputConstants.UNKNOWN.value).showIf { autoDirectionToggle.value }
    private val alwaysBackwardKey by KeybindSetting("Always Backward Key", InputConstants.UNKNOWN.value).showIf { autoDirectionToggle.value }

    private var autoMoveRight = false
    private var autoMoveLeft = false
    private var autoMoveForward = false
    private var autoMoveBackward = false

    private var active = false
    private var previousAttackToggled = false

    fun isActive(): Boolean = active

    override fun init() {
        register<KeyboardEvent.KeyPressed> {
            if (LocationUtils.world != WorldType.Garden) return@register
            if (event.action != GLFW.GLFW_PRESS) return@register
            if (mc.screen != null) return@register

            if (event.keyEvent.key == toggleKey.value) {
                active = !active

                if (active) {
                    updateKeyBinding(mc.options.keyAttack, blockBreakKey.value)
                    updateKeyBinding(mc.options.keyJump, jumpKey.value)
                    previousAttackToggled = mc.options.toggleAttack().get()
                    mc.options.toggleAttack().set(isAuto.value == 1)
                } else {
                    resetAllMovement()
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

        register<KeyboardEvent.KeyPressed> {
            if (!autoDirectionToggle.value || LocationUtils.world != WorldType.Garden) return@register
            if (event.action != GLFW.GLFW_PRESS || mc.screen != null) return@register

            val key = event.keyEvent.key

            if (autoMoveRight || autoMoveLeft || autoMoveForward || autoMoveBackward) {
                val isMovementKey = mc.options.keyUp.matches(event.keyEvent) ||
                        mc.options.keyDown.matches(event.keyEvent) ||
                        mc.options.keyLeft.matches(event.keyEvent) ||
                        mc.options.keyRight.matches(event.keyEvent)

                if (isMovementKey) {
                    resetAllMovement()
                    if (SoTerm.debugFlags.contains("farm")) ChatUtils.modMessage("§cAuto direction stopped by manual movement")
                    return@register
                }
            }

            when {
                key == alwaysRightKey.value -> {
                    val newState = !autoMoveRight
                    resetAllMovement()
                    autoMoveRight = newState
                    mc.options.keyRight.setDown(autoMoveRight)
                    if (SoTerm.debugFlags.contains("farm")) ChatUtils.modMessage("§eAuto Right: ${if (autoMoveRight) "§aON" else "§cOFF"}")
                    event.isCanceled = true
                }
                key == alwaysLeftKey.value -> {
                    val newState = !autoMoveLeft
                    resetAllMovement()
                    autoMoveLeft = newState
                    mc.options.keyLeft.setDown(autoMoveLeft)
                    if (SoTerm.debugFlags.contains("farm")) ChatUtils.modMessage("§eAuto Left: ${if (autoMoveLeft) "§aON" else "§cOFF"}")
                    event.isCanceled = true
                }
                key == alwaysForwardKey.value -> {
                    val newState = !autoMoveForward
                    resetAllMovement()
                    autoMoveForward = newState
                    mc.options.keyUp.setDown(autoMoveForward)
                    if (SoTerm.debugFlags.contains("farm")) ChatUtils.modMessage("§eAuto Forward: ${if (autoMoveForward) "§aON" else "§cOFF"}")
                    event.isCanceled = true
                }
                key == alwaysBackwardKey.value -> {
                    val newState = !autoMoveBackward
                    resetAllMovement()
                    autoMoveBackward = newState
                    mc.options.keyDown.setDown(autoMoveBackward)
                    if (SoTerm.debugFlags.contains("farm")) ChatUtils.modMessage("§eAuto Backward: ${if (autoMoveBackward) "§aON" else "§cOFF"}")
                    event.isCanceled = true
                }
            }
        }

        register<TickEvent.Start> {
            if (mc.player == null) return@register

            if (!autoDirectionToggle.value || mc.screen != null || LocationUtils.world != WorldType.Garden) {
                if (autoMoveRight || autoMoveLeft || autoMoveForward || autoMoveBackward) {
                    resetAllMovement()
                }
                return@register
            }

            if (autoMoveRight) mc.options.keyRight.setDown(true)
            if (autoMoveLeft) mc.options.keyLeft.setDown(true)
            if (autoMoveForward) mc.options.keyUp.setDown(true)
            if (autoMoveBackward) mc.options.keyDown.setDown(true)
        }
    }

    private fun resetAllMovement() {
        autoMoveRight = false
        autoMoveLeft = false
        autoMoveForward = false
        autoMoveBackward = false
        mc.options.keyRight.setDown(false)
        mc.options.keyLeft.setDown(false)
        mc.options.keyUp.setDown(false)
        mc.options.keyDown.setDown(false)
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