package com.github.gameringop.event.impl

import com.github.gameringop.event.Event
import net.minecraft.network.protocol.Packet

abstract class PacketEvent: Event(cancelable = true) {
    class Sent(val packet: Packet<*>): PacketEvent()
    class Received(val packet: Packet<*>): PacketEvent()
}