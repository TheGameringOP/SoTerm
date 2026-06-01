package com.github.gameringop.event.impl

import com.github.gameringop.event.Event
import com.github.gameringop.utils.ChatUtils.formattedText
import com.github.gameringop.utils.ChatUtils.unformattedText
import net.minecraft.network.chat.Component

class ChatMessageEvent(val component: Component): Event(cancelable = true) {
    val formattedText by lazy { component.formattedText }
    val unformattedText by lazy { component.unformattedText }
}
