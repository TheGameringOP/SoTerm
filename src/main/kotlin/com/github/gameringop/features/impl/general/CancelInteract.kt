package com.github.gameringop.features.impl.general

import com.github.gameringop.event.impl.PacketEvent
import com.github.gameringop.features.Feature
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket
import net.minecraft.world.item.Items

object CancelInteract: Feature("Disables Hypixel's stupid Ender Pearl throw block when you are looking at a wall/floor/ceiling.") {
    override fun init() {
        register<PacketEvent.Sent> {
            if (event.packet !is ServerboundUseItemOnPacket) return@register
            val itemStack = mc.player?.getItemInHand(event.packet.hand) ?: return@register
            if (itemStack.item != Items.ENDER_PEARL) return@register
            event.isCanceled = true
        }
    }
}