package com.github.gameringop.utils

import java.nio.charset.StandardCharsets
import java.util.*

object StringUtils {

    fun String.encodeBase64(): String {
        val bytes = this.toByteArray(StandardCharsets.UTF_8)
        return Base64.getEncoder().encodeToString(bytes)
    }

    fun String.decodeBase64(): String {
        val decodedBytes = Base64.getDecoder().decode(this)
        return String(decodedBytes, StandardCharsets.UTF_8)
    }

    fun String.stripped(): String {
        return this.replace(Regex("(?i)[§&][0-9A-FK-OR]"), "")
    }
}