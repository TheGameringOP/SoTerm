package com.github.gameringop.commands.impl

import com.github.gameringop.commands.BaseCommand
import com.github.gameringop.commands.CommandNodeBuilder
import com.github.gameringop.utils.ChatUtils

object DungeonCommand: BaseCommand("d") {
    override fun CommandNodeBuilder.build() {
        runs {
            ChatUtils.sendCommand("warp dungeon_hub")
        }
    }
}