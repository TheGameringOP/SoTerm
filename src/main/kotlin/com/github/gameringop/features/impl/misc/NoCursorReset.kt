package com.github.gameringop.features.impl.misc

/*
object NoCursorReset: Feature("Prevents the mouse cursor from resetting when you open a new gui.") {
    private var last = 0L
    private var mx = 0.0
    private var my = 0.0

    override fun init() {
        register<ContainerEvent.Open> {
            if (mx == .0 && my == .0) return@register
            if (System.currentTimeMillis() - last > 5000) return@register
            if (LocationUtils.inDungeon && event.screen.title.string == "Spirit Leap") return@register
            GLFW.glfwSetCursorPos(mc.window.handle(), mx, my)
            mx = .0
            my = .0
        }

        register<MainThreadPacketRecivedEvent.Pre> {
            if (event.packet is ClientboundOpenScreenPacket || event.packet is ClientboundContainerClosePacket) {
                if (mx != .0 && my != .0) return@register
                val handler = mc.mouseHandler as IMouseHandler
                last = System.currentTimeMillis()
                mx = handler.xPos
                my = handler.yPos
            }
        }
    }
}*/