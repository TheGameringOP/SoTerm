package com.github.gameringop.websocket

import com.github.gameringop.SoTerm
import com.github.gameringop.SoTerm.mc
import com.github.gameringop.utils.JsonUtils
import com.github.gameringop.utils.StringUtils.decodeBase64
import com.github.gameringop.utils.ThreadUtils
import com.github.gameringop.utils.catch
import com.github.gameringop.utils.network.WebUtils
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import java.lang.Runnable
import java.util.concurrent.Executors

object WebSocket {
    val URL = "d3NzOi8vbm9hbW0ub3Jn".decodeBase64()
    val mname = "Tm9hbW1BZGRvbnM=".decodeBase64()

    private val worker = run {
        val threadFactory = fun(it: Runnable) = Thread(it, "${mname}-WebSocket").apply { isDaemon = true }
        CoroutineScope(Executors.newSingleThreadExecutor(threadFactory).asCoroutineDispatcher() + SupervisorJob())
    }

    @Volatile private var session: DefaultClientWebSocketSession? = null
    private var socketJob: Job? = null

    fun init() {
        ThreadUtils.addShutdownHook(::shutdown)
        PacketRegistry.init()
        connect()
    }

    fun send(packet: Any) = worker.launch {
        val socket = session?.takeIf { it.isActive } ?: return@launch
        val raw = JsonUtils.gsonBuilder.toJson(packet)
        socket.send(Frame.Text(raw))
    }

    private fun connect() {
        if (socketJob?.isActive == true) return

        socketJob = worker.launch {
            try {
                WebUtils.client.webSocket(URL) {
                    SoTerm.logger.info("WebSocket: Connected Successfully")
                    session = this

                    for (frame in incoming) if (frame is Frame.Text) handleMessage(frame.readText())
                }
            }
            catch (e: Exception) {
                SoTerm.logger.info("WebSocket: Disconnected", e)
            }
            finally {
                session = null
                socketJob = null
                ThreadUtils.setTimeout(30_000, ::connect)
            }
        }
    }

    private fun handleMessage(message: String) = catch {
        val json = JsonParser.parseString(message).takeIf(JsonElement::isJsonObject)?.asJsonObject ?: return@catch
        val type = json.get("type")?.asString?.takeUnless(String::isBlank) ?: return@catch
        val packetClass = PacketRegistry.getPacketClass(type) ?: return@catch
        val packet = JsonUtils.gsonBuilder.fromJson(message, packetClass)
        mc.submit(packet::handle)
    }

    private fun shutdown() = runBlocking {
        catch { session?.close() }
        catch { socketJob?.cancelAndJoin() }
        worker.cancel()
    }
}