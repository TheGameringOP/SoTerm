package com.github.gameringop.utils.network

import com.github.gameringop.SoTerm
import com.github.gameringop.utils.ChatUtils
import com.github.gameringop.utils.JsonUtils
import com.github.gameringop.utils.StringUtils.decodeBase64
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.compression.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.websocket.*
import java.util.zip.Deflater

object WebUtils {
    private val AGENT = "Tm9hbW1BZGRvbnMvMS4xLjkgKCtodHRwczovL25vYW1tLm9yZyk=".decodeBase64()

    val client = HttpClient(CIO) {
        install(WebSockets) {
            pingIntervalMillis = 30_000

            extensions {
                install(WebSocketDeflateExtension) {
                    compressionLevel = Deflater.BEST_SPEED
                    compressIfBiggerThan(bytes = 1024)
                    compressIf { frame -> frame is Frame.Text }
                }
            }
        }
        install(UserAgent) { agent = AGENT }
        install(HttpTimeout) { connectTimeoutMillis = 10_000 }
        install(ContentNegotiation) { json(JsonUtils.json) }
        install(ContentEncoding) {
            gzip(1.0F)
            deflate(0.9F)
        }

        expectSuccess = false
    }

    suspend fun get(url: String) = runCatching { client.get(url) }
    suspend inline fun <reified T> getAs(url: String) = get(url).mapCatching {
        if (SoTerm.debugFlags.contains("request")) {
            ChatUtils.modMessage("Request: $url")
        }
        if (it.status.value !in 200 .. 299) error("HTTP ${it.status.value}-${it.status.description}: ${it.bodyAsText()}")
        it.body<T>()
    }

    suspend fun post(url: String, body: Any) = runCatching {
        client.post(url) {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
    }
}