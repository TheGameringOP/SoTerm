package com.github.gameringop.commands.impl

import com.github.gameringop.SoTerm
import com.github.gameringop.SoTerm.electionData
import com.github.gameringop.SoTerm.mc
import com.github.gameringop.SoTerm.priceData
import com.github.gameringop.commands.BaseCommand
import com.github.gameringop.commands.CommandNodeBuilder
import com.github.gameringop.config.Config
import com.github.gameringop.utils.ChatUtils
import com.github.gameringop.utils.PlayerUtils
import com.github.gameringop.utils.dungeons.map.utils.ScanUtils
import com.github.gameringop.utils.items.ItemUtils.skyblockId
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object TestCommand: BaseCommand("test") {
    override fun CommandNodeBuilder.build() {
        literal("relative") {
            runs {
                val room = ScanUtils.currentRoom ?: return@runs
                ChatUtils.chat(ScanUtils.getRelativeCoord(PlayerUtils.getSelectionBlock() !!, room.centerPos, room.rotation ?: return@runs))
            }
        }

        literal("gui") {
            runs {
                mc.screen?.onClose()
                //    SoTerm.screen = KitchenSinkScreen()
            }
        }

        literal("config") {
            runs {
                Config.save()
                Config.load()
            }
        }

        literal("mayor") {
            runs {
                ChatUtils.chat(electionData)
            }
        }

        literal("scope") {
            runs {
                SoTerm.scope.launch {
                    delay(1000)
                    println("hi")
                }
            }
        }

        runs {
            ChatUtils.chat("${mc.player?.mainHandItem.skyblockId}: ${priceData[mc.player?.mainHandItem.skyblockId]}")
        }
    }
}