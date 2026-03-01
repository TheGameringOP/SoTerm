package com.github.gameringop.commands.impl

import com.github.gameringop.SoTerm.debugFlags
import com.github.gameringop.SoTerm.scope
import com.github.gameringop.SoTerm.screen
import com.github.gameringop.commands.BaseCommand
import com.github.gameringop.commands.CommandNodeBuilder
import com.github.gameringop.event.EventBus
import com.github.gameringop.event.impl.ChatMessageEvent
import com.github.gameringop.features.impl.dungeon.LeapMenu
import com.github.gameringop.ui.clickgui.ClickGuiScreen
import com.github.gameringop.ui.hud.HudEditorScreen
import com.github.gameringop.utils.*
import com.github.gameringop.utils.ChatUtils.addColor
import com.github.gameringop.utils.dungeons.DungeonListener
import com.github.gameringop.utils.dungeons.enums.DungeonClass
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import kotlinx.coroutines.launch
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
        "/ts leap <class>" to "Automatically leaps to the selected class"
    )

    override fun CommandNodeBuilder.build() {
        runs { screen = ClickGuiScreen }

        literal("ping") {
            runs {
                ChatUtils.modMessage("§aPing: §f${ServerUtils.averagePing}ms")
            }
        }

        literal("discord") {
            runs {
                Utils.openDiscordLink()
            }
        }

        literal("help") {
            runs {
                val helpMenu = StringBuilder("§6§lSoTerm§r\n")
                commands.forEach { (cmd, desc) -> helpMenu.append("§e$cmd §7- $desc\n") }
                ChatUtils.chat(helpMenu.toString().trim())
            }
        }

        literal("hud") {
            runs { screen = HudEditorScreen }
        }

        literal("debug") {
            runs {
                ChatUtils.modMessage("§7Flags: §f${debugFlags.joinToString(", ")}")
            }

            argument("flag", StringArgumentType.word()) {
                runs { ctx ->
                    val flag = StringArgumentType.getString(ctx, "flag")
                    if (debugFlags.remove(flag)) ChatUtils.modMessage("§cRemoved debug flag: §b$flag")
                    else {
                        debugFlags.add(flag)
                        ChatUtils.modMessage("§aAdded debug flag: §b$flag")
                    }
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
            argument("player1", StringArgumentType.word()) {
                suggests(partyMembersSuggestion)
                runs { ctx -> handleLeapOrder(ctx, 1) }

                argument("player2", StringArgumentType.word()) {
                    suggests(partyMembersSuggestion)
                    runs { ctx -> handleLeapOrder(ctx, 2) }

                    argument("player3", StringArgumentType.word()) {
                        suggests(partyMembersSuggestion)
                        runs { ctx -> handleLeapOrder(ctx, 3) }

                        argument("player4", StringArgumentType.word()) {
                            suggests(partyMembersSuggestion)
                            runs { ctx -> handleLeapOrder(ctx, 4) }
                        }
                    }
                }
            }
        }

        literal("leap") {
            argument("class", StringArgumentType.word()) {
                suggests { DungeonClass.entries.map { it.name } }
                runs { ctx ->
                    val clazz = StringArgumentType.getString(ctx, "class")
                    val player = DungeonListener.dungeonTeammatesNoSelf.find { it.clazz.name == clazz } ?: return@runs ChatUtils.modMessage("leap target not found")
                    scope.launch { PlayerUtils.leapAction(player) }
                }
            }
        }
    }

    private val partyMembersSuggestion = { PartyUtils.members.map { it.lowercase() } }

    private fun handleLeapOrder(ctx: CommandContext<FabricClientCommandSource>, count: Int) {
        val validPlayers = mutableListOf<String>()

        for (i in 1 .. count) {
            val inputName = StringArgumentType.getString(ctx, "player$i")
            validPlayers.add(inputName.lowercase())
        }

        LeapMenu.customLeapOrder = validPlayers
        ChatUtils.modMessage("§aCustom leap order set to: §f${validPlayers.joinToString(", ")}")
    }
}

