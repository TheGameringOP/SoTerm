package com.github.gameringop.features.impl.floor7

import com.github.gameringop.event.impl.MainThreadPacketReceivedEvent
import com.github.gameringop.event.impl.PacketEvent
import com.github.gameringop.event.impl.TickEvent
import com.github.gameringop.features.Feature
import com.github.gameringop.ui.clickgui.components.impl.DropdownSetting
import com.github.gameringop.ui.clickgui.components.impl.TextInputSetting
import com.github.gameringop.utils.ChatUtils
import com.github.gameringop.utils.ChatUtils.unformattedText
import com.github.gameringop.utils.location.LocationUtils
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket
import net.minecraft.world.item.Items

object MelodyAlert: Feature() {
    private val msg by TextInputSetting("Melody Message", "I ❤ Melody")
    private val mode by DropdownSetting("Progress Mode", 0, listOf("1/4", "25%"))

    private val progressSlots = intArrayOf(25, 34, 43)
    private var isMelodyOpen = false
    private var currentStage = - 1

    override fun init() {
        register<MainThreadPacketReceivedEvent.Post> {
            if (msg.value.isBlank()) return@register
            if (LocationUtils.F7Phase != 3) return@register
            val packet = event.packet as? ClientboundOpenScreenPacket ?: return@register
            if (packet.title.unformattedText != "Click the button on time!") return@register

            ChatUtils.sendPartyMessage(msg.value)
            isMelodyOpen = true
            currentStage = - 1
        }

        register<MainThreadPacketReceivedEvent.Post> {
            if (! isMelodyOpen) return@register
            if (event.packet !is ClientboundContainerClosePacket) return@register
            isMelodyOpen = false
            currentStage = - 1
        }

        register<PacketEvent.Sent> {
            if (! isMelodyOpen) return@register
            if (event.packet !is ServerboundContainerClosePacket) return@register
            isMelodyOpen = false
            currentStage = - 1
        }

        register<TickEvent.Start> {
            if (! isMelodyOpen) return@register
            if (mc.screen == null) {
                isMelodyOpen = false
                return@register
            }

            if (currentStage == 3) return@register
            for (i in progressSlots.indices) {
                if (i <= currentStage) continue

                if (mc.player !!.containerMenu.getSlot(progressSlots[i]).item.`is`(Items.LIME_TERRACOTTA)) {
                    val progress = if (mode.value == 0) "${i + 1}/4" else "${(i + 1) * 25}%"
                    ChatUtils.sendPartyMessage("${msg.value} $progress")
                    currentStage = i
                }
            }
        }
    }
}