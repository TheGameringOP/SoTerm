package com.github.gameringop.utils.network.cache

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import kotlinx.serialization.json.JsonObject
import java.util.concurrent.TimeUnit

object ProfileCache {
    private val cache: Cache<String, JsonObject> = CacheBuilder.newBuilder()
        .maximumSize(100).expireAfterAccess(5, TimeUnit.MINUTES).build()

    fun addToCache(name: String, profile: JsonObject) {
        val lowercase = name.lowercase()
        cache.put(lowercase, profile)
    }

    fun getFromCache(name: String) = cache.getIfPresent(name.lowercase())
}