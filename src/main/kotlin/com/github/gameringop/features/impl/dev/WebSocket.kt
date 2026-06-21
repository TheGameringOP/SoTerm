package com.github.gameringop.features.impl.dev

import com.github.gameringop.SoTerm
import com.github.gameringop.event.EventBus
import com.github.gameringop.event.impl.DungeonEvent
import com.github.gameringop.event.impl.WebSocketEvent
import com.github.gameringop.event.impl.WorldChangeEvent
import com.github.gameringop.features.Feature
import com.github.gameringop.utils.ChatUtils
import com.github.gameringop.utils.GsonUtils
import com.github.gameringop.utils.ThreadUtils
import com.github.gameringop.utils.dungeons.DungeonListener
import com.github.gameringop.utils.dungeons.map.DungeonInfo
import com.github.gameringop.utils.dungeons.map.core.Room
import com.github.gameringop.utils.dungeons.map.core.RoomType
import com.github.gameringop.utils.location.LocationUtils
import com.github.gameringop.utils.location.LocrawListener
import com.github.gameringop.websocket.PacketRegistry
import com.github.gameringop.websocket.WebSocket.send
import com.github.gameringop.websocket.packets.C2SPacketDungeonStart
import com.google.gson.JsonElement
import com.google.gson.JsonParser

object WebSocket: Feature(name = "WebSocket", toggled = true) {
    override fun toggle() = Unit

    override fun init() {
        register<WebSocketEvent.Connect> {
            SoTerm.logger.debug("WebSocket: Connected Successfully")
            ChatUtils.debug("ws", "[WS] Connected Successfully")
        }

        register<WebSocketEvent.Payload> {
            ChatUtils.debug("ws", "[Payload] Payload: ${event.message}")
            val json = JsonParser.parseString(event.message).takeIf(JsonElement::isJsonObject)?.asJsonObject ?: return@register
            val type = json.get("type").asString.takeUnless(String::isBlank) ?: return@register
            val packetClass = PacketRegistry.getClass(type) ?: return@register
            GsonUtils.gson.fromJson(json, packetClass).handle()
        }

        EventBus.register<DungeonEvent.RunStatedEvent> { sendDungeonInfo() }
        EventBus.register<DungeonEvent.RunEndedEvent> { send(mapOf("type" to "dungeon_end")) }
        EventBus.register<WorldChangeEvent> { send(mapOf("type" to "reset")) }
    }

    fun sendDungeonInfo() = ThreadUtils.scheduledTaskServer(30) ws@{
        if (DungeonListener.dungeonTeammatesNoSelf.isEmpty()) return@ws
        val serverId = LocrawListener.server.ifEmpty { LocationUtils.serverId } ?: return@ws
        val floor = LocationUtils.dungeonFloor ?: return@ws
        val team = DungeonListener.dungeonTeammates.map { it.name }.ifEmpty { return@ws }
        val entrance = (DungeonInfo.dungeonList.find { (it as? Room)?.data?.type == RoomType.ENTRANCE } as? Room)?.getArrayPosition() ?: return@ws

        send(C2SPacketDungeonStart(serverId, floor, team, entrance))
    }
}