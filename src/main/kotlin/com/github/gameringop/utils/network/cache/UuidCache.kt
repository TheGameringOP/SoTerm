package com.github.gameringop.utils.network.cache

object UuidCache {
    private val cache = HashMap<String, String>()

    fun addToCache(name: String, uuid: String) {
        val lowercase = name.lowercase()
        if (lowercase in cache) return
        cache[name.lowercase()] = uuid.replace("-", "")
    }

    fun getFromCache(name: String): String? = cache[name.lowercase()]

    fun getNameFromCache(uuid: String): String? {
        return cache.entries.find { it.value == uuid }?.key
    }
}