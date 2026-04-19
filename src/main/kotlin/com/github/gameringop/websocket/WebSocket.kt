package com.github.gameringop.websocket

import com.github.gameringop.SoTerm
import com.github.gameringop.utils.ChatUtils
import com.github.gameringop.utils.JsonUtils
import com.github.gameringop.utils.StringUtils.decodeBase64
import com.github.gameringop.utils.ThreadUtils
import com.google.gson.JsonParser
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object WebSocket {
    val URL = "d3NzOi8vbm9hbW0ub3Jn".decodeBase64()
    val mname = "Tm9hbW1BZGRvbnM=".decodeBase64()
    private val worker = Executors.newSingleThreadScheduledExecutor {
        Thread(it, "${mname}-WebSocket").apply { isDaemon = true }
    }

    fun init() {
        PacketRegistry.init()
        TSSocket.connect()
        ThreadUtils.addShutdownHook { TSSocket.close(1000, "client stopping") }
    }

    fun send(packet: Any) = worker.execute {
        if (! TSSocket.isOpen) return@execute
        val raw = JsonUtils.gsonBuilder.toJson(packet)
        if (SoTerm.debugFlags.contains("ws")) ChatUtils.chat(raw)
        runCatching { TSSocket.send(raw) }
    }

    private object TSSocket: WebSocketClient(URI(URL)) {
        override fun onMessage(message: String) = worker.execute {
            runCatching {
                val json = JsonParser.parseString(message).takeIf { it.isJsonObject }?.asJsonObject ?: return@execute
                val type = json.get("type")?.asString.takeUnless { it.isNullOrBlank() } ?: return@execute
                val packetClass = PacketRegistry.getPacketClass(type) ?: return@execute
                val packet = JsonUtils.gsonBuilder.fromJson(message, packetClass)
                SoTerm.mc.execute { packet.handle() }
            }
        }

        init {
            connectionLostTimeout = 30
            isTcpNoDelay = true
        }

        override fun onClose(code: Int, reason: String, remote: Boolean) = worker.execute { worker.schedule({ reconnect() }, 30, TimeUnit.SECONDS) }
        override fun onOpen(handshakedata: ServerHandshake) = worker.execute { SoTerm.logger.info("WebSocket: Connected Successfully") }
        override fun onError(ex: Exception) = worker.execute { SoTerm.logger.error("WebSocket: transport error", ex) }
    }
}