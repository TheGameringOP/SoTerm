package com.github.gameringop.utils

import java.util.Base64

object StringUtils {
    fun String.encodeBase64(): String {
        return Base64.getEncoder().encodeToString(this.toByteArray())
    }

    fun String.decodeBase64(): String {
        return String(Base64.getDecoder().decode(this))
    }
}