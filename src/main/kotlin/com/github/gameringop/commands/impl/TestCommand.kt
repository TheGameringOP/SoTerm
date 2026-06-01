package com.github.gameringop.commands.impl

import com.github.gameringop.SoTerm
import com.github.gameringop.SoTerm.mc
import com.github.gameringop.commands.BaseCommand
import com.github.gameringop.commands.CommandNodeBuilder
import com.github.gameringop.config.Config
import com.github.gameringop.features.impl.dev.HypixelAPI
import com.github.gameringop.init.NetworkLoop
import com.github.gameringop.init.NetworkLoop.electionData
import com.github.gameringop.init.NetworkLoop.priceData
import com.github.gameringop.utils.ChatUtils
import com.github.gameringop.utils.ChatUtils.addColor
import com.github.gameringop.utils.PlayerUtils
import com.github.gameringop.utils.dungeons.map.utils.ScanUtils
import com.github.gameringop.utils.items.ItemUtils.idToNameMap
import com.github.gameringop.utils.network.ApiUtils
import com.github.gameringop.websocket.PacketRegistry
import com.github.gameringop.websocket.WebSocket
import com.github.gameringop.websocket.packets.S2CPacketChat
import com.google.gson.JsonParser
import com.mojang.brigadier.arguments.IntegerArgumentType
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

            literal("mayor") {
                literal("refresh") {
                    runs {
                        ChatUtils.chat("§eRefreshing mayor data...")
                        SoTerm.scope.launch {
                            NetworkLoop.refreshMayor()
                                .onSuccess {
                                    printMayorPerks()
                                    ChatUtils.chat("§aMayor cache updated.")
                                }
                                .onFailure { ChatUtils.chat("§cMayor refresh failed: ${it.message}") }
                        }
                    }
                }

                runs { printMayorPerks() }
            }

            literal("lowestbin") {
                literal("refresh") {
                    runs {
                        ChatUtils.chat("§eRefreshing lowest bins...")
                        SoTerm.scope.launch {
                            NetworkLoop.refreshLowestBins()
                                .onSuccess { ChatUtils.chat("§aLowest bins updated. §7(${priceData.size} prices cached)") }
                                .onFailure { ChatUtils.chat("§cLowest bin refresh failed: ${it.message}") }
                        }
                    }
                }

                argument("item", StringArgumentType.word()) {
                    suggests { priceData.keys }
                    runs {
                        printPrice(StringArgumentType.getString(it, "item").uppercase())
                    }
                }

                runs {
                    ChatUtils.chat("§e${priceData.size} prices cached. §7/test api lowestbin <ITEM_ID> §8| §7refresh")
                }
            }

            literal("bazaar") {
                literal("refresh") {
                    runs {
                        ChatUtils.chat("§eRefreshing bazaar prices...")
                        SoTerm.scope.launch {
                            NetworkLoop.refreshBazaar()
                                .onSuccess { ChatUtils.chat("§aBazaar prices updated. §7(${priceData.size} prices cached)") }
                                .onFailure { ChatUtils.chat("§cBazaar refresh failed: ${it.message}") }
                        }
                    }
                }

                runs { ChatUtils.chat("§7/test api bazaar refresh") }
            }
        }

        literal("ws") {
            literal("status") {
                runs {
                    val connected = WebSocket.isConnected()
                    ChatUtils.chat("§eWebSocket: ${if (connected) "§aconnected" else "§cdisconnected"}")
                    ChatUtils.chat("§7Inbound buffer: §f${WebSocket.getRecentInbound().size} messages")
                }
            }

            literal("reconnect") {
                runs {
                    WebSocket.reconnect()
                    ChatUtils.chat("§eWebSocket reconnecting...")
                }
            }

            literal("clear") {
                runs {
                    WebSocket.clearRecentInbound()
                    ChatUtils.chat("§aCleared inbound message buffer.")
                }
            }

            literal("types") {
                runs {
                    ChatUtils.chat("§6Registered packet types:")
                    PacketRegistry.packets.keys.sorted().forEach { ChatUtils.chat(" §7- §f$it") }
                }
            }

            literal("listen") {
                argument("count", IntegerArgumentType.integer(1, 32)) {
                    runs {
                        printInbound(IntegerArgumentType.getInteger(it, "count"))
                    }
                }

                runs { printInbound(5) }
            }

            literal("chat") {
                argument("message", StringArgumentType.greedyString()) {
                    runs {
                        val message = StringArgumentType.getString(it, "message").addColor()
                        WebSocket.send(S2CPacketChat("§d${mc.user.name}: §r$message"))
                        ChatUtils.chat("§aSent WS chat packet.")
                    }
                }

                runs { ChatUtils.chat("§7/test ws chat <message>") }
            }

            literal("users") {
                runs {
                    WebSocket.send(mapOf("type" to "check_users"))
                    ChatUtils.chat("§aSent check_users. §7Use §f/test ws listen §7for the response.")
                }
            }

            literal("reset") {
                runs {
                    WebSocket.send(mapOf("type" to "reset"))
                    ChatUtils.chat("§aSent reset packet.")
                }
            }

            literal("raw") {
                argument("json", StringArgumentType.greedyString()) {
                    runs {
                        val json = StringArgumentType.getString(it, "json")
                        if (! WebSocket.isConnected()) {
                            ChatUtils.chat("§cWebSocket is not connected.")
                            return@runs
                        }
                        runCatching { JsonParser.parseString(json) }
                            .onFailure {
                                ChatUtils.chat("§cInvalid JSON: ${it.message}")
                                return@runs
                            }
                        WebSocket.sendRaw(json)
                        ChatUtils.chat("§aSent raw JSON.")
                    }
                }

                runs { ChatUtils.chat("§7/test ws raw {\"type\":\"check_users\"}") }
            }

            runs {
                ChatUtils.chat("§6WebSocket test commands:")
                ChatUtils.chat(" §fstatus §7- connection info")
                ChatUtils.chat(" §freconnect §7| §fclear §7| §ftypes §7| §flisten [n]")
                ChatUtils.chat(" §fchat <msg> §7| §fusers §7| §freset §7| §fraw <json>")
            }
        }

        runs {
            ChatUtils.chat("§7/test api §8| §7/test ws §8| §7/test mayor")
        }
    }

    private fun printMayorPerks() {
        val mayor = electionData.mayor
        if (mayor.name.isBlank()) {
            ChatUtils.chat("§cNo mayor data cached. §7Try §e/test api mayor refresh")
            return
        }

        ChatUtils.chat("§6§lMayor: §f${mayor.name}")
        if (mayor.perks.isEmpty()) ChatUtils.chat(" §7(no perks listed)")
        mayor.perks.forEachIndexed { index, perk ->
            ChatUtils.chat(" §e${index + 1}. §f${perk.name} §8- §7${perk.description}")
        }

        electionData.minister?.takeIf { it.name.isNotBlank() }?.let { minister ->
            ChatUtils.chat("§6§lMinister: §f${minister.name}")
            ChatUtils.chat(" §e• §f${minister.perk.name} §8- §7${minister.perk.description}")
        }
    }

    private fun printPrice(itemId: String) {
        val price = priceData[itemId]
        if (price == null) {
            ChatUtils.chat("§cNo cached price for §f$itemId§c.")
            ChatUtils.chat("§7Try §e/test api lowestbin refresh §7or §e/test api bazaar refresh")
            return
        }

        val name = idToNameMap[itemId] ?: itemId
        ChatUtils.chat("§a$name §8($itemId)§a: §6${formatCoins(price)} coins")
    }

    private fun printInbound(count: Int) {
        val messages = WebSocket.getRecentInbound()
        if (messages.isEmpty()) {
            ChatUtils.chat("§7No inbound messages yet.")
            return
        }

        ChatUtils.chat("§6Last ${count.coerceAtMost(messages.size)} inbound message(s):")
        messages.takeLast(count).forEachIndexed { index, message ->
            val preview = message.replace("\n", " ").take(200)
            ChatUtils.chat(" §8${index + 1}. §f$preview")
        }
    }

    private fun formatCoins(amount: Long): String {
        return when {
            amount >= 1_000_000 -> "%.2fM".format(amount / 1_000_000.0)
            amount >= 1_000 -> "%.1fK".format(amount / 1_000.0)
            else -> amount.toString()
        }
    }

    private fun formatTime(milliseconds: Long): String {
        val totalSecs = milliseconds / 1000
        val m = (totalSecs % 3600) / 60
        val s = (totalSecs % 60).toString().padStart(2, '0')
        return "$m:$s"
    }
}