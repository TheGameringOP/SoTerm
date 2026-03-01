package com.github.gameringop.config

import com.google.gson.JsonElement

internal interface Savable {
    fun write(): JsonElement
    fun read(element: JsonElement?)
}