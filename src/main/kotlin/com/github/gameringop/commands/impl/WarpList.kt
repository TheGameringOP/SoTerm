package com.github.gameringop.commands.impl

import com.github.gameringop.commands.BaseCommand
import com.github.gameringop.commands.CommandNodeBuilder
import com.github.gameringop.features.impl.general.WarpShortcuts
import com.github.gameringop.utils.ChatUtils

object WarpList : BaseCommand("warplist") {
    override fun CommandNodeBuilder.build() {
        requires { WarpShortcuts.enabled }
        runs {
            val formatted = WarpShortcuts.warpList.joinToString("\n") { (cmd, loc) ->
                "${cmd.padEnd(WarpShortcuts.warpList.maxOf { it.first.length } + 1)}-> $loc"
            }
            ChatUtils.modMessage("&l&n&bCommand / Location\n&d$formatted")
        }
    }
}
