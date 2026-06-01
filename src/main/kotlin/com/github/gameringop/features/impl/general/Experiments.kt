package com.github.gameringop.features.impl.general

import com.github.gameringop.event.impl.ContainerEvent
import com.github.gameringop.event.impl.MainThreadPacketReceivedEvent
import com.github.gameringop.event.impl.TickEvent
import com.github.gameringop.features.Feature
import com.github.gameringop.ui.clickgui.components.impl.SliderSetting
import com.github.gameringop.ui.clickgui.components.impl.ToggleSetting
import com.github.gameringop.utils.GuiUtils
import com.github.gameringop.utils.items.ItemUtils.hasGlint
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import java.util.concurrent.ConcurrentHashMap

object Experiments: Feature(name = "Experiments", description = "Solves Chronomatron and Ultrasequencer experiments.") {
    private val clickDelay by SliderSetting("Click Delay", 200, 100, 1000, 10)
    private val delayVariety by SliderSetting("Delay Variety", 50, 0, 1000, 10)
    private val autoClose by ToggleSetting("Auto Close", true)
    private val serumCount by SliderSetting("Serum Count", 0, 0, 3, 1)
    private val getMaxXp by ToggleSetting("Get Max XP", false)

    private var currentHandler: ExperimentHandler? = null
    private var lastClickTime = 0L

    fun resetTimer() {
        lastClickTime = System.currentTimeMillis()
    }

    override fun init() {
        register<ContainerEvent.Open> {
            val title = event.screen.title.string
            currentHandler = when {
                title.startsWith("Chronomatron (") -> ChronomatronHandler()
                title.startsWith("Ultrasequencer (") -> UltrasequencerHandler()
                else -> null
            }
        }

        register<ContainerEvent.MouseClick> {
            if (currentHandler != null && mc.screen is AbstractContainerScreen<*>) {
                event.isCanceled = true
            }
        }

        register<MainThreadPacketReceivedEvent.Post> {
            if (currentHandler == null) return@register
            val packet = event.packet as? ClientboundContainerSetSlotPacket ?: return@register

            if (packet.containerId == mc.player?.containerMenu?.containerId) {
                currentHandler?.onSlotUpdate(packet.slot, packet.item)
            }
        }

        register<TickEvent.Start> {
            val handler = currentHandler ?: return@register
            handler.onTick()

            val now = System.currentTimeMillis()
            if (now - lastClickTime < getDelay()) return@register

            handler.nextClick()?.let { nextSlot ->
                GuiUtils.clickSlot(nextSlot, GuiUtils.ButtonType.MIDDLE)
                lastClickTime = now
            }

            if (autoClose.value && handler.shouldClose()) {
                mc.player?.closeContainer()
                currentHandler = null
            }
        }
    }

    private fun getDelay(): Long {
        val variety = if (delayVariety.value > 0) (0..delayVariety.value).random() else 0
        return clickDelay.value.toLong() + variety
    }

    private abstract class ExperimentHandler {
        var clicks = 0
        abstract fun onSlotUpdate(slotIndex: Int, stack: ItemStack)
        open fun onTick() {}
        abstract fun nextClick(): Int?
        abstract fun shouldClose(): Boolean
    }

    private class ChronomatronHandler : ExperimentHandler() {
        private val order = mutableListOf<Int>()
        private var hasData = false
        private var lastAddedSlot = -1
        private var closeNow = false

        override fun onSlotUpdate(slotIndex: Int, stack: ItemStack) {
            val slots = mc.player?.containerMenu?.slots ?: return
            if (slots.size <= 49) return
            val centerItem = slots[49].item.item

            if (centerItem == Items.GLOWSTONE && lastAddedSlot != -1) {
                val lastSlotStack = slots[lastAddedSlot].item
                if (!lastSlotStack.hasGlint()) {
                    val target = if (getMaxXp.value) 15 else 11 - serumCount.value
                    closeNow = order.size > target
                    hasData = false
                    return
                }
            }
            if (hasData || centerItem != Items.CLOCK) return

            val glintedSlot = slots.find { it.index in 10..43 && it.item.hasGlint() } ?: return

            order.add(glintedSlot.index)
            lastAddedSlot = glintedSlot.index
            hasData = true
            clicks = 0
            resetTimer()
        }

        override fun nextClick(): Int? {
            if (hasData && clicks < order.size) {
                return order[clicks++]
            }
            return null
        }

        override fun shouldClose(): Boolean {
            return closeNow && clicks >= order.size
        }
    }

    private class UltrasequencerHandler : ExperimentHandler() {
        private val order = ConcurrentHashMap<Int, Int>()
        private var isPlayerTurn = false

        override fun onTick() {
            val slots = mc.player?.containerMenu?.slots ?: return
            if (slots.size <= 49) return
            val centerItem = slots[49].item.item

            val wasPlayerTurn = isPlayerTurn
            isPlayerTurn = (centerItem == Items.CLOCK)

            if (!wasPlayerTurn && isPlayerTurn) {
                clicks = 0
                resetTimer()
            }

            if (wasPlayerTurn && !isPlayerTurn) {
                order.clear()
            }

            if (!isPlayerTurn && centerItem == Items.GLOWSTONE) {
                for (slot in slots) {
                    if (slot.index in 9..44) {
                        val num = slot.item.count
                        if (num > 0) {
                            val name = slot.item.hoverName.string.replace(Regex("§."), "")
                            if (name.matches(Regex("\\d+"))) {
                                order[num - 1] = slot.index
                            }
                        }
                    }
                }
            }
        }

        override fun onSlotUpdate(slotIndex: Int, stack: ItemStack) {}

        override fun nextClick(): Int? {
            if (isPlayerTurn && clicks < order.size && order.containsKey(clicks)) {
                return order[clicks++]
            }
            return null
        }

        override fun shouldClose(): Boolean {
            val target = if (getMaxXp.value) 20 else 9 - serumCount.value
            return isPlayerTurn && clicks >= order.size && order.size >= target
        }
    }
}