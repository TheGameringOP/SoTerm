package com.github.gameringop.commands.impl

import com.github.gameringop.SoTerm
import com.github.gameringop.commands.BaseCommand
import com.github.gameringop.commands.CommandNodeBuilder
import com.github.gameringop.utils.ChatUtils
import com.github.gameringop.utils.ChatUtils.addColor
import com.github.gameringop.websocket.WebSocket
import com.github.gameringop.websocket.packets.S2CPacketChat
import com.mojang.brigadier.arguments.StringArgumentType

object WebSocketTest: BaseCommand("ws") {
    override fun CommandNodeBuilder.build() {
        literal("chat") {
            argument("message", StringArgumentType.greedyString()) {
                runs {
                    val message = StringArgumentType.getString(it, "message").addColor()
                    WebSocket.send(S2CPacketChat("§d${SoTerm.mc.user.name}: §r$message").apply(S2CPacketChat::handle))
                }
            }

            runs {
                ChatUtils.modMessage("/ws chat <message>")
            }
        }

        literal("users") {
                WebSocket.send(mapOf("type" to "check_users"))
            }
        }
}