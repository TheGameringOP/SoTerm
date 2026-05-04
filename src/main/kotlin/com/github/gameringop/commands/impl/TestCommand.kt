package com.github.gameringop.commands.impl

import com.github.gameringop.SoTerm
import com.github.gameringop.SoTerm.electionData
import com.github.gameringop.SoTerm.mc
import com.github.gameringop.SoTerm.priceData
import com.github.gameringop.commands.BaseCommand
import com.github.gameringop.commands.CommandNodeBuilder
import com.github.gameringop.config.Config
import com.github.gameringop.features.impl.dev.HypixelAPI
import com.github.gameringop.utils.ChatUtils
import com.github.gameringop.utils.PlayerUtils
import com.github.gameringop.utils.dungeons.map.utils.ScanUtils
import com.github.gameringop.utils.items.ItemUtils.skyblockId
import com.github.gameringop.utils.network.ApiUtils
import com.mojang.brigadier.arguments.StringArgumentType
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

        literal("api") {
            literal("spirit") {
                argument("user", StringArgumentType.greedyString()) {
                    runs {
                        val user = StringArgumentType.getString(it, "user")
                        SoTerm.scope.launch {
                            val hasSpirit = HypixelAPI.fetchSpiritPet(user)
                            if (hasSpirit) {
                                ChatUtils.chat("§a$user has a Legendary Spirit pet! §7(§6Spirit§7)")
                            } else {
                                ChatUtils.chat("§c$user does NOT have a Legendary Spirit pet")
                            }
                        }
                        ChatUtils.chat("§eChecking Spirit pet for $user...")
                    }
                }
            }
            literal("secrets") {
                argument("user", StringArgumentType.greedyString()) {
                    runs {
                        val user = StringArgumentType.getString(it, "user")
                        SoTerm.scope.launch {
                            val secrets = HypixelAPI.fetchTotalSecrets(user)
                            if (secrets >= 0) {
                                ChatUtils.chat("§a$user has §e$secrets §atotal dungeon secrets")
                            } else {
                                ChatUtils.chat("§cFailed to fetch secrets for $user")
                            }
                        }
                        ChatUtils.chat("§eFetching secrets for $user...")
                    }
                }
            }
            literal("cataxp") {
                argument("user", StringArgumentType.greedyString()) {
                    runs {
                        val user = StringArgumentType.getString(it, "user")
                        SoTerm.scope.launch {
                            val xp = HypixelAPI.fetchCatacombsExperience(user)
                            if (xp >= 0) {
                                ChatUtils.chat("§a$user has §e${xp.toLong()} §acatacombs experience")
                            } else {
                                ChatUtils.chat("§cFailed to fetch catacombs XP for $user")
                            }
                        }
                        ChatUtils.chat("§eFetching catacombs XP for $user...")
                    }
                }
            }
            literal("catalvl") {
                argument("user", StringArgumentType.greedyString()) {
                    runs {
                        val user = StringArgumentType.getString(it, "user")
                        SoTerm.scope.launch {
                            val xp = HypixelAPI.fetchCatacombsExperience(user)
                            if (xp >= 0) {
                                val level = ApiUtils.getCatacombsLevel(xp)
                                ChatUtils.chat("§a$user has catacombs level §e$level")
                            } else {
                                ChatUtils.chat("§cFailed to fetch catacombs level for $user")
                            }
                        }
                        ChatUtils.chat("§eFetching catacombs level for $user...")
                    }
                }
            }

            literal("mp") {
                argument("user", StringArgumentType.greedyString()) {
                    runs {
                        val user = StringArgumentType.getString(it, "user")
                        SoTerm.scope.launch {
                            val mp = HypixelAPI.fetchMagicalPower(user)
                            if (mp >= 0) {
                                ChatUtils.chat("§a$user has §e$mp §amagical power")
                            } else {
                                ChatUtils.chat("§cFailed to fetch magical power for $user")
                            }
                        }
                        ChatUtils.chat("§eFetching magical power for $user...")
                    }
                }
            }

            literal("fastest") {
                argument("user", StringArgumentType.word()) {
                    argument("floor", StringArgumentType.word()) {
                        runs {
                            val user = StringArgumentType.getString(it, "user")
                            val floor = StringArgumentType.getString(it, "floor")
                            SoTerm.scope.launch {
                                val timeMs = HypixelAPI.fetchFastestTime(user, floor)
                                if (timeMs > 0) {
                                    val formatted = formatTime(timeMs)
                                    ChatUtils.chat("§a$user's fastest $floor time: §e$formatted")
                                } else {
                                    ChatUtils.chat("§cNo time found for $user on $floor")
                                }
                            }
                            ChatUtils.chat("§eFetching fastest time for $user on $floor...")
                        }
                    }
                }
            }

            literal("totalruns") {
                argument("user", StringArgumentType.greedyString()) {
                    runs {
                        val user = StringArgumentType.getString(it, "user")
                        SoTerm.scope.launch {
                            val runs = HypixelAPI.fetchTotalRuns(user)
                            if (runs >= 0) {
                                ChatUtils.chat("§a$user has §e$runs §atotal dungeon runs")
                            } else {
                                ChatUtils.chat("§cFailed to fetch total runs for $user")
                            }
                        }
                        ChatUtils.chat("§eFetching total runs for $user...")
                    }
                }
            }

            literal("savg") {
                argument("user", StringArgumentType.greedyString()) {
                    runs {
                        val user = StringArgumentType.getString(it, "user")
                        SoTerm.scope.launch {
                            val secrets = HypixelAPI.fetchTotalSecrets(user)
                            val runs = HypixelAPI.fetchTotalRuns(user)
                            if (secrets >= 0 && runs > 0) {
                                val avg = secrets.toDouble() / runs
                                val avgFormatted = "%.2f".format(avg)
                                ChatUtils.chat("§a$user has §e$avgFormatted §asecret average")
                            } else if (runs == 0) {
                                ChatUtils.chat("§c$user has no dungeon runs recorded")
                            } else {
                                ChatUtils.chat("§cFailed to fetch secret average for $user")
                            }
                        }
                        ChatUtils.chat("§eFetching secret average for $user...")
                    }
                }
            }

            literal("uuid") {
                argument("user", StringArgumentType.greedyString()) {
                    runs {
                        val user = StringArgumentType.getString(it, "user")
                        SoTerm.scope.launch {
                            val uuid = HypixelAPI.fetchUUID(user)
                            if (uuid != null) {
                                ChatUtils.chat("§a$user's UUID: §e$uuid")
                            } else {
                                ChatUtils.chat("§cFailed to fetch UUID for $user")
                            }
                        }
                        ChatUtils.chat("§eFetching UUID for $user...")
                    }
                }
            }

            literal("requests") {
                runs {
                    SoTerm.scope.launch {
                        val count = HypixelAPI.getRequestCount()
                        ChatUtils.chat("§aAPI requests in last 5 minutes: §e$count")
                    }
                }
            }
        }

        runs {
            ChatUtils.chat("${mc.player?.mainHandItem?.skyblockId}: ${priceData[mc.player?.mainHandItem?.skyblockId]}")
        }
    }

    private fun formatTime(milliseconds: Long): String {
        val totalSecs = milliseconds / 1000
        val m = (totalSecs % 3600) / 60
        val s = (totalSecs % 60).toString().padStart(2, '0')
        return "$m:$s"
    }
}