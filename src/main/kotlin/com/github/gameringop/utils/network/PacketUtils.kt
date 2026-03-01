package com.github.gameringop.utils.network

import com.github.gameringop.SoTerm.mc
import net.minecraft.network.protocol.Packet

object PacketUtils {
    fun Packet<*>.send() = mc.connection?.send(this)
}