package com.github.gameringop.utils

import com.github.gameringop.SoTerm.mc
import net.minecraft.Util
import net.minecraft.network.protocol.Packet
import java.awt.Color
import java.net.URI

object Utils {
    val favoriteColor = Color(0, 134, 255)

    fun openDiscordLink() {
        val link = "h*#t#t~p*s:/#/*d*is#c~o~r*d.~g~~*g#*/*h~y*#p*i~x#*e*#l~".remove("#", "~", "*")
        Util.getPlatform().openUri(URI(link))
    }

    fun Packet<*>.send() = mc.connection?.send(this)
}