package com.github.gameringop.websocket

import com.github.gameringop.SoTerm
import com.github.gameringop.SoTerm.mc
import com.github.gameringop.event.EventBus
import com.github.gameringop.event.impl.WebSocketEvent
import com.github.gameringop.utils.ChatUtils
import com.github.gameringop.utils.GsonUtils
import com.github.gameringop.utils.StringUtils.decodeBase64
import com.github.gameringop.utils.ThreadUtils
import com.github.gameringop.utils.catch
import com.github.gameringop.utils.network.WebUtils
import io.ktor.client.plugins.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import java.lang.Runnable
import java.util.*
import java.util.concurrent.Executors

object WebSocket {
    val URL = "d3NzOi8vbm9hbW0ub3Jn".decodeBase64()
    val mname = "Tm9hbW1BZGRvbnM=".decodeBase64()

    private val recentInbound = ArrayDeque<String>()

    private val worker = run {
        val threadFactory = fun(it: Runnable) = Thread(it, "${mname}-WebSocket").apply { isDaemon = true }
        CoroutineScope(Executors.newSingleThreadExecutor(threadFactory).asCoroutineDispatcher() + SupervisorJob())
    }

    @Volatile private var session: DefaultClientWebSocketSession? = null
    private var socketJob: Job? = null

    fun isConnected() = session?.isActive == true

    fun getRecentInbound(): List<String> = synchronized(recentInbound) { recentInbound.toList() }

    fun clearRecentInbound() = synchronized(recentInbound) { recentInbound.clear() }

    fun init() {
        ThreadUtils.addShutdownHook(::shutdown)
        PacketRegistry.init()
        connect()
    }

    fun send(packet: Any) = worker.launch {
        val socket = session?.takeIf { it.isActive } ?: return@launch
        val json = GsonUtils.gson.toJsonTree(packet).asJsonObject
        val type = PacketRegistry.getType(packet)
        if (type != null) json.addProperty("type", type)
        socket.send(Frame.Text(json.toString()))
        ChatUtils.debug("ws", "[WS] sending $json")
    }

    fun sendRaw(json: String) = worker.launch {
        val socket = session?.takeIf { it.isActive } ?: return@launch
        socket.send(Frame.Text(json))
        ChatUtils.debug("ws", "[WS] sending $json")
    }

    fun reconnect() {
        worker.launch {
            catch { session?.close() }
            session = null
            socketJob?.cancel()
            socketJob = null
            connect()
        }
    }

    private fun connect() {
        if (socketJob?.isActive == true) return

        socketJob = worker.launch {
            try {
                WebUtils.client.webSocket(URL, {
                    timeout {
                        requestTimeoutMillis = 60_000
                        socketTimeoutMillis = 60_000
                    }
                }) {
                    mc.submit { EventBus.post(WebSocketEvent.Connect) }
                    session = this

                    for (frame in incoming) if (frame is Frame.Text) {
                        EventBus.post(WebSocketEvent.Payload(frame.readText()))
                        recordInbound(frame.readText())
                    }
                }
            }
            catch (e: Exception) {
                ChatUtils.debug("ws", "[WS] disconnected")
                SoTerm.logger.info("WebSocket: Disconnected", e)
                mc.submit { EventBus.post(WebSocketEvent.Disconnect) }
            }
            finally {
                session = null
                socketJob = null
                ThreadUtils.setTimeout(30_000, ::connect)
            }
        }
    }

    private fun recordInbound(text: String) = synchronized(recentInbound) {
        if (recentInbound.size >= 32) recentInbound.removeFirst()
        recentInbound.addLast(text)
    }

    private fun shutdown() = runBlocking {
        catch { session?.close() }
        catch { session?.cancel() }
        catch { socketJob?.cancelAndJoin() }
        worker.cancel()
    }
}