package com.github.gameringop.commands.impl

import com.github.gameringop.SoTerm.debugFlags
import com.github.gameringop.SoTerm.mc
import com.github.gameringop.SoTerm.scope
import com.github.gameringop.SoTerm.screen
import com.github.gameringop.commands.BaseCommand
import com.github.gameringop.commands.CommandNodeBuilder
import com.github.gameringop.event.EventBus
import com.github.gameringop.event.impl.ChatMessageEvent
import com.github.gameringop.event.impl.OPDebugFlagEvent
import com.github.gameringop.features.impl.dungeon.LeapMenu
import com.github.gameringop.ui.clickgui.ClickGuiScreen
import com.github.gameringop.ui.hud.HudEditorScreen
import com.github.gameringop.utils.*
import com.github.gameringop.utils.ChatUtils.addColor
import com.github.gameringop.utils.StringUtils.decodeBase64
import com.github.gameringop.utils.dungeons.DungeonListener
import com.github.gameringop.utils.dungeons.enums.DungeonClass
import com.github.gameringop.utils.items.ItemUtils.skyblockId
import com.github.gameringop.utils.network.WebUtils
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.network.chat.Component

object TsCommand: BaseCommand("ts") {
    private val commands = mapOf(
        "/ts" to "Config GUI",
        "/ts hud" to "HUD editor",
        "/ts discord" to "Opens the link to the Discord server",
        "/ts debug" to "Debug flags",
        "/ts sim" to "Simulate chat message",
        "/ts leaporder" to "Configure custom leap sorting",
        "/ts ping" to "Shows your ping in chat",
        "/ts tps" to "Shows the server's tps in chat",
        "/ts leap <class>" to "Automatically leaps to the selected class",
        "/ts swapmask" to "Equips either Bonzo Mask or Spirit Mask",
        "/ts rodswap" to "Automatically rodswaps for you",
        "/ts leap <class>" to "Automatically leaps to the selected class",
        "/ts swapto <ItemID>" to "Automatically equips the item in the EQ menu"
    )

    override fun CommandNodeBuilder.build() {
        runs { screen = ClickGuiScreen }

        literal("ping") {
            runs {
                ChatUtils.modMessage("§aPing: §f${ServerUtils.averagePing}ms")
            }
        }

        literal("tps") {
            runs {
                ChatUtils.modMessage("§aTPS: §f${ServerUtils.tps}")
            }
        }

        literal("discord") {
            runs {
                Utils.openDiscordLink()
            }
        }

        literal("hud") {
            runs { screen = HudEditorScreen }
        }

        literal("ping") {
            runs {
                ChatUtils.modMessage("§aPing: §f${ServerUtils.averagePing}ms")
            }
        }

        literal("debug") {
            runs {
                ChatUtils.modMessage("§7Flags: §f${debugFlags.joinToString(", ")}")
            }

            argument("flag", StringArgumentType.word()) {
                runs { ctx ->
                    val flag = StringArgumentType.getString(ctx, "flag")
                    val event: OPDebugFlagEvent
                    if (debugFlags.remove(flag)) {
                        ChatUtils.modMessage("§cRemoved debug flag: §b$flag")
                        event = OPDebugFlagEvent.Remove(flag)
                    }
                    else {
                        debugFlags.add(flag)
                        ChatUtils.modMessage("§aAdded debug flag: §b$flag")
                        event = OPDebugFlagEvent.Add(flag)
                    }

                    EventBus.post(event)
                }
            }
        }

        literal("sim") {
            runs {
                ChatUtils.modMessage("§cInvalid Usage: §f/na sim <message>")
            }

            argument("message", StringArgumentType.greedyString()) {
                runs { ctx ->
                    val msg = StringArgumentType.getString(ctx, "message").addColor()
                    ChatUtils.modMessage(msg)
                    EventBus.post(ChatMessageEvent(Component.literal(msg)))
                }
            }
        }

        literal("leaporder") {
            argument("sorting", StringArgumentType.word()) {
                suggests { listOf("name", "class") }

                argument("player1", StringArgumentType.word()) {
                    suggests(partyMembersSuggestion)
                    runs { ctx -> setLeapOrder(ctx, 1) }

                    argument("player2", StringArgumentType.word()) {
                        suggests(partyMembersSuggestion)
                        runs { ctx -> setLeapOrder(ctx, 2) }

                        argument("player3", StringArgumentType.word()) {
                            suggests(partyMembersSuggestion)
                            runs { ctx -> setLeapOrder(ctx, 3) }

                            argument("player4", StringArgumentType.word()) {
                                suggests(partyMembersSuggestion)
                                runs { ctx -> setLeapOrder(ctx, 4) }
                            }
                        }
                    }
                }
            }

        }



        literal("rtca") {
            runs { sendRtca() }
            argument("name", StringArgumentType.word()) {
                runs {
                    sendRtca(StringArgumentType.getString(it, "name"))
                }
            }
        }

        literal("swapmask") {
            runs {
                scope.launch {
                    PlayerUtils.changeMaskAction()
                }
            }
        }

        literal("rodswap") {
            runs {
                scope.launch {
                    PlayerUtils.rodSwap()
                }
            }
        }

        literal("swapto") {
            runs { ChatUtils.modMessage("missing skyblock id argument. /na swapto <ItemID>") }
            argument("skyblock id", StringArgumentType.word()) {
                runs {
                    scope.launch {
                        val inv = mc.player?.inventory?.nonEquipmentItems ?: return@launch
                        val item = StringArgumentType.getString(it, "skyblock id")
                        if (inv.none { it.skyblockId == item }) return@launch ChatUtils.modMessage("$item not found in inventory")
                        PlayerUtils.quickSwapAction(item)
                    }
                }
            }
        }

        literal("leap") {
            argument("class", StringArgumentType.word()) {
                suggests { DungeonClass.entries.filterNot { it == DungeonClass.Empty }.map { it.name } }
                runs { ctx ->
                    val clazz = StringArgumentType.getString(ctx, "class")
                    val player = DungeonListener.dungeonTeammatesNoSelf.find { it.clazz.name == clazz } ?: return@runs ChatUtils.modMessage("leap target not found")
                    scope.launch { PlayerUtils.leapAction(player) }
                }
            }
        }
    }


    private val partyMembersSuggestion = { PartyUtils.members.map { it.lowercase() } }

    private fun setLeapOrder(ctx: CommandContext<FabricClientCommandSource>, count: Int) {
        val sortingType = StringArgumentType.getString(ctx, "sorting").lowercase()
        if (sortingType != "name" && sortingType != "class") return ChatUtils.modMessage("§cInvalid sorting type! Use 'name' or 'class'")

        val validPlayers = mutableListOf<String>()
        for (i in 1 .. count) {
            val inputName = StringArgumentType.getString(ctx, "player$i")
            validPlayers.add(inputName.lowercase())
        }

        LeapMenu.customLeapOrder = validPlayers
        LeapMenu.customLeapType = sortingType
        ChatUtils.modMessage("§aCustom leap order set to: §f$sortingType §awith players: §f${validPlayers.joinToString(", ")}")
    }

    private fun sendRtca(name: String = mc.user.name) = scope.launch {
        WebUtils.getAs<RtcaData>("aHR0cHM6Ly9hcGkubm9hbW0ub3JnL2h5cGl4ZWwvcnRjYS8=".decodeBase64() + name).onSuccess {
            ChatUtils.modMessage("${it.name} is ${it.runs} M7 runs away from ca50 (${formatClassRuns(it.classes)})")
        }.onFailure {
            ChatUtils.modMessage("An error occurred meow! (${it.message})")
            it.printStackTrace()
        }
    }

    private fun formatClassRuns(runs: Map<String, Int>): String {
        return runs.filterValues { it > 0 }.entries.joinToString(" | ") { (name, runs) ->
            "${name.take(4).uppercaseFirst()} $runs"
        }
    }

    @Serializable
    private data class RtcaData(val name: String, val runs: Int, val classes: Map<String, Int>)
}