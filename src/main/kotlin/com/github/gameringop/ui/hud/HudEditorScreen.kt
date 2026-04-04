package com.github.gameringop.ui.hud

import com.github.gameringop.config.Config
import com.github.gameringop.features.FeatureManager
import com.github.gameringop.ui.utils.Resolution
import com.github.gameringop.ui.utils.componnents.UIButton
import com.github.gameringop.utils.render.Render2D
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW
import java.awt.Color

object HudEditorScreen: Screen(Component.literal("HudEditor")) {
    val enabledElements get() = FeatureManager.hudElements.filter { it.toggle }

    override fun init() {
        super.init()

        val btnWidth = 100
        val btnHeight = 20

        addRenderableWidget(UIButton(
            width / 2 - btnWidth / 2,
            height - 100,
            btnWidth,
            btnHeight,
            "§cReset HUD"
        ) {
            FeatureManager.hudElements.forEach { element ->
                element.x = 20f
                element.y = 20f
                element.scale = 1f
            }
        })
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        Resolution.refresh()
        Resolution.push(context)

        val mX = Resolution.getMouseX(mouseX)
        val mY = Resolution.getMouseY(mouseY)
        val midX = Resolution.width / 2

        enabledElements.forEach { it.drawEditor(context, mX, mY) }

        val element = enabledElements.find { it.isDragging }
        Render2D.drawCenteredString(context, element?.name.orEmpty(), midX, 10f, Color.WHITE, 1.2f)
        Render2D.drawCenteredString(context, "ESC to Save and Exit", midX, Resolution.height - 20f, Color.GRAY, shadow = false)

        Resolution.pop(context)

        super.render(context, mouseX, mouseY, delta)
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        val hoveredElement = enabledElements.find { it.isHovered(Resolution.getMouseX(), Resolution.getMouseY()) }
        
        if (hoveredElement != null) {
            val screenWidth = Resolution.width
            val screenHeight = Resolution.height
            val scaledW = hoveredElement.width * hoveredElement.scale
            val scaledH = hoveredElement.height * hoveredElement.scale
            val centeredOffset = if (hoveredElement.centered) scaledW / 2f else 0f
            
            when (event.key) {
                GLFW.GLFW_KEY_LEFT -> {
                    hoveredElement.x = 0f + centeredOffset
                    return true
                }
                GLFW.GLFW_KEY_RIGHT -> {
                    hoveredElement.x = screenWidth - scaledW + centeredOffset
                    return true
                }
                GLFW.GLFW_KEY_UP -> {
                    hoveredElement.y = 0f
                    return true
                }
                GLFW.GLFW_KEY_DOWN -> {
                    hoveredElement.x = screenWidth / 2f
                    return true
                }
            }
        }
        
        return super.keyPressed(event)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontal: Double, vertical: Double): Boolean {
        val mX = Resolution.getMouseX(mouseX)
        val mY = Resolution.getMouseY(mouseY)
        
        val hoveredElement = enabledElements.find { it.isHovered(mX, mY) }
        
        if (hoveredElement != null) {
            val increment = (vertical * 0.1).toFloat()
            hoveredElement.scale = (hoveredElement.scale + increment).coerceIn(0.5f, 5.0f)
            return true
        }
        
        enabledElements.forEach { element ->
            if (element.isDragging) {
                val increment = (vertical * 0.1).toFloat()
                element.scale = (element.scale + increment).coerceIn(0.5f, 5.0f)
                return true
            }
        }
        
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical)
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, bl: Boolean): Boolean {
        if (super.mouseClicked(mouseButtonEvent, bl)) return true

        val mX = Resolution.getMouseX(mouseButtonEvent.x)
        val mY = Resolution.getMouseY(mouseButtonEvent.y)

        if (mouseButtonEvent.button() == 0) {
            enabledElements.forEach {
                it.startDragging(mX, mY)
                if (it.isDragging) return true
            }
        }

        return false
    }

    override fun mouseDragged(mouseButtonEvent: MouseButtonEvent, deltaX: Double, deltaY: Double): Boolean {
        val mX = Resolution.getMouseX(mouseButtonEvent.x)
        val mY = Resolution.getMouseY(mouseButtonEvent.y)
        
        val draggingElement = enabledElements.find { it.isDragging }
        if (draggingElement != null) {
            val screenWidth = Resolution.width
            val screenHeight = Resolution.height
            val scaledW = draggingElement.width * draggingElement.scale
            val scaledH = draggingElement.height * draggingElement.scale
            val centeredOffset = if (draggingElement.centered) scaledW / 2f else 0f
            
            draggingElement.x = (draggingElement.x + deltaX.toFloat()).coerceIn(centeredOffset, screenWidth - scaledW + centeredOffset)
            draggingElement.y = (draggingElement.y + deltaY.toFloat()).coerceIn(0f, screenHeight - scaledH)
            return true
        }
        
        return super.mouseDragged(mouseButtonEvent, deltaX, deltaY)
    }

    override fun mouseReleased(mouseButtonEvent: MouseButtonEvent): Boolean {
        enabledElements.forEach { it.isDragging = false }
        return super.mouseReleased(mouseButtonEvent)
    }

    override fun onClose() {
        Config.save()
        super.onClose()
    }
}
