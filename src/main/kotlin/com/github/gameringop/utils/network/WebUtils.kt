package com.github.gameringop.utils.network

import com.github.gameringop.utils.JsonUtils
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*

object WebUtils {
    val client = HttpClient(OkHttp) {
        install(WebSockets) { pingIntervalMillis = 30_000 }
        install(UserAgent) { agent = "User" }
        install(HttpTimeout) {
            connectTimeoutMillis = 10_000
            requestTimeoutMillis = 10_000
            socketTimeoutMillis = 10_000
        }
        install(ContentNegotiation) {
            json(JsonUtils.json)
        }
        expectSuccess = false
    }

    suspend fun get(url: String) = runCatching { client.get(url) }
    suspend inline fun <reified T> getAs(url: String) = get(url).mapCatching {
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