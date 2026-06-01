package com.github.gameringop.utils.location

import com.github.gameringop.event.EventBus
import com.github.gameringop.event.EventListener
import com.github.gameringop.event.impl.ChatMessageEvent
import com.github.gameringop.event.impl.WorldChangeEvent
import com.google.gson.JsonParser

object LocrawListener {
    var server = ""
    var gameType = ""
    var location = ""
    var map = ""

    fun init() {
        EventBus.register<ChatMessageEvent> {
            if (! LocationUtils.onHypixel) return@register
            parseLocRaw(event.unformattedText)
        }

        EventListener.create<WorldChangeEvent> { reset() }
    }

    private fun parseLocRaw(message: String) {
        if (! message.startsWith("{\"server\":") || ! message.endsWith("}")) return
        val locRaw = JsonParser.parseString(message).getAsJsonObject()
        if (locRaw.has("server")) server = locRaw.get("server").asString
        if (locRaw.has("gametype")) gameType = locRaw.get("gametype").asString
        if (locRaw.has("mode")) location = locRaw.get("mode").asString
        if (locRaw.has("map")) map = locRaw.get("map").asString
    }

    private fun reset() {
        server = ""
        gameType = ""
        location = ""
        map = ""
    }
}