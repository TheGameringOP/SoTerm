package com.github.gameringop.features.impl.dungeon

import com.github.gameringop.event.impl.DungeonEvent
import com.github.gameringop.features.Feature
import com.github.gameringop.ui.clickgui.components.getValue
import com.github.gameringop.ui.clickgui.components.impl.ToggleSetting
import com.github.gameringop.ui.clickgui.components.provideDelegate
import com.github.gameringop.utils.ChatUtils
import com.github.gameringop.utils.Utils.equalsOneOf
import com.github.gameringop.utils.dungeons.DungeonListener
import com.github.gameringop.utils.dungeons.map.core.RoomState
import com.github.gameringop.utils.dungeons.map.core.RoomType
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.sounds.SoundEvents

object RoomAlerts: Feature("Alerts when certain stuff happens in your current room") {
    private val clear by ToggleSetting("Cleared", true)
    private val secrets by ToggleSetting("Secrets Done", true)

    override fun init() {
        register<DungeonEvent.RoomEvent.onStateChange> {
            if (! event.room.data.type.equalsOneOf(RoomType.NORMAL, RoomType.PUZZLE, RoomType.RARE, RoomType.TRAP)) return@register
            if (event.room.data.type == RoomType.PUZZLE && event.room.name != "Blaze") return@register
            if (DungeonListener.thePlayer !in event.roomPlayers) return@register

            when (event.newState) {
                RoomState.CLEARED -> if (clear.value) {
                    alert((if (event.room.data.secrets == 0) "&a" else "") + "Cleared")
                }

                RoomState.GREEN -> if (secrets.value && event.room.data.secrets > 0) {
                    alert("&aSecrets Done!")
                }

                else -> return@register
            }
        }
    }

    private fun alert(msg: String) {
        mc.soundManager.play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING, 1f))
        ChatUtils.showTitle(msg)
    }
}