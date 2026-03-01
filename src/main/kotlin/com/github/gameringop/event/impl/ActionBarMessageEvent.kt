package com.github.gameringop.event.impl

import com.github.gameringop.event.Event
import com.github.gameringop.utils.ChatUtils.formattedText
import com.github.gameringop.utils.ChatUtils.unformattedText
import net.minecraft.network.chat.Component

class ActionBarMessageEvent(val component: Component): Event(cancelable = true) {
    inline val formattedText: String get() = component.formattedText
    inline val unformattedText: String get() = component.unformattedText

    var message = formattedText
}