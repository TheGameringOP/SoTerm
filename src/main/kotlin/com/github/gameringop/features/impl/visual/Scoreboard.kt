package com.github.gameringop.features.impl.visual

import com.github.gameringop.event.impl.MainThreadPacketReceivedEvent
import com.github.gameringop.features.Feature
import com.github.gameringop.features.impl.dev.ClickGui
import com.github.gameringop.mixin.IPlayerTabOverlay
import com.github.gameringop.ui.clickgui.components.Style
import com.github.gameringop.ui.clickgui.components.getValue
import com.github.gameringop.ui.clickgui.components.impl.ToggleSetting
import com.github.gameringop.ui.clickgui.components.provideDelegate
import com.github.gameringop.ui.clickgui.components.withDescription
import com.github.gameringop.ui.hud.HudElement
import com.github.gameringop.utils.ChatUtils.formattedText
import com.github.gameringop.utils.location.LocationUtils
import com.github.gameringop.utils.render.Render2D
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.*
import net.minecraft.world.scores.DisplaySlot
import net.minecraft.world.scores.Objective
import net.minecraft.world.scores.PlayerTeam
import java.awt.Color
import net.minecraft.world.scores.Scoreboard as MCScoreboard

object Scoreboard: Feature("Draws a custom scoreboard instead of the vanilla one.") {
    private val hideServerId by ToggleSetting("Hide Server ID").withDescription("Hides the 'm151AM' text from the scoreboard")

    private var needsUpdate = true
    private val cachedLines = mutableListOf<String>()
    private var cachedTitle = ""
    private var cachedW = 0f
    private var cachedH = 0f

    @Suppress("RemoveRedundantQualifierName")
    private val hud = object: HudElement() {
        override val name = "Scoreboard"
        override val toggle get() = Scoreboard.enabled

        override fun draw(ctx: GuiGraphics, example: Boolean): Pair<Float, Float> {
            val scoreboard = mc.level?.scoreboard ?: return 0f to 0f
            val objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR) ?: return 0f to 0f

            if (needsUpdate) updateCache(scoreboard, objective)
            if (cachedLines.isEmpty()) return 0f to 0f

            val boxWidth = cachedW.toDouble()
            val boxHeight = cachedH.toDouble()
            val xOffset = - boxWidth - 5
            val yOffset = - (boxHeight / 2)
            val padding = 8

            Render2D.drawRect(ctx, xOffset, yOffset, boxWidth, boxHeight, Color(15, 15, 15, 190))
            Render2D.drawRect(ctx, xOffset, yOffset, boxWidth, 2.0, Style.accentColor)
            Render2D.drawRect(ctx, xOffset - 1, yOffset - 1, boxWidth + 2.0, boxHeight + 2.0, Color(255, 255, 255, 20))

            Render2D.drawCenteredString(ctx, cachedTitle, (xOffset + boxWidth / 2).toFloat(), (yOffset + padding).toFloat(), shadow = false)

            val startY = yOffset + padding + mc.font.lineHeight + 4
            val lineHeights = mc.font.lineHeight + 2

            cachedLines.forEachIndexed { index, text ->
                Render2D.drawString(ctx, text, (xOffset + padding).toFloat(), (startY + (index * lineHeights)).toFloat())
            }

            return cachedW to cachedH
        }

        override fun isHovered(mx: Int, my: Int): Boolean {
            if (cachedW == 0f) return false
            val visualWidth = cachedW * scale
            val visualHeight = cachedH * scale
            return mx >= x - visualWidth - 5 && mx <= x && my >= y - (visualHeight / 2) && my <= y + (visualHeight / 2)
        }

        override fun drawBackground(ctx: GuiGraphics, mx: Int, my: Int) {
            if (cachedW == 0f) return
            val scaledW = cachedW * scale
            val scaledH = cachedH * scale
            val drawX = x - scaledW - 5
            val drawY = y - (scaledH / 2)

            val hovered = mx >= drawX && mx <= drawX + scaledW && my >= drawY && my <= drawY + scaledH
            val borderColor = if (isDragging || hovered) Style.accentColor else Color(255, 255, 255, 40)

            Render2D.drawRect(ctx, drawX.toDouble(), drawY.toDouble(), scaledW.toDouble(), scaledH.toDouble(), Color(10, 10, 10, 150))
            Render2D.drawRect(ctx, drawX.toDouble(), drawY.toDouble(), scaledW.toDouble(), 1.0, borderColor)
            Render2D.drawRect(ctx, drawX.toDouble(), (drawY + scaledH - 1).toDouble(), scaledW.toDouble(), 1.0, borderColor)
        }
    }

    override fun init() {
        hud.x = 200f
        hud.y = 200f
        hudElements.add(hud)

        register<MainThreadPacketReceivedEvent.Post> {
            if (
                event.packet is ClientboundSetScorePacket || event.packet is ClientboundSetObjectivePacket ||
                event.packet is ClientboundSetDisplayObjectivePacket || event.packet is ClientboundResetScorePacket ||
                event.packet is ClientboundSetPlayerTeamPacket
            ) {
                needsUpdate = true
            }
        }
    }

    private fun updateCache(scoreboard: MCScoreboard, objective: Objective) {
        cachedLines.clear()
        cachedTitle = objective.displayName.formattedText

        val scores = scoreboard.listPlayerScores(objective).sortedByDescending { it.value }.take(15)

        if (scores.isEmpty()) {
            cachedW = 0f
            cachedH = 0f
            needsUpdate = false
            return
        }

        val font = mc.font
        var maxW = font.width(cachedTitle).toFloat()

        scores.forEachIndexed { index, score ->
            val name = score.ownerName().string
            val team = scoreboard.getPlayersTeam(name)
            var line = PlayerTeam.formatNameForTeam(team, Component.literal(name)).formattedText

            if (LocationUtils.inSkyblock && hideServerId.value && index == 0) {
                line = "§7Date: " + line.substringBefore(" §8")
            }

            cachedLines.add(line)
            maxW = maxOf(maxW, font.width(line).toFloat())
        }

        val padding = 8f
        val lineHeights = font.lineHeight + 2
        cachedW = maxW + (padding * 2)
        cachedH = (cachedLines.size * lineHeights) + font.lineHeight + (padding * 2f)
        needsUpdate = false
    }

    @JvmStatic
    fun drawTablist(graphics: GuiGraphics) {
        val connection = mc.player?.connection ?: return
        val players = connection.listedOnlinePlayers.sortedWith(compareBy({ it.team?.name }, { it.profile.name }))
        if (players.isEmpty()) return

        val tablist = mc.gui.tabList as IPlayerTabOverlay
        val font = mc.font

        val playersPerColumn = 20
        val maxColumns = 4
        val headSize = 8
        val entrySpacing = 1
        val columnGap = 12
        val padding = 8

        val columns = players.chunked(playersPerColumn).take(maxColumns)

        val columnWidths = columns.map { column ->
            column.maxOf { player ->
                val nameText = player.tabListDisplayName?.formattedText ?: player.profile.name
                with(Render2D) { nameText.width() }
            }
        }

        val totalGridWidth = columnWidths.sum() + (columns.size * (headSize + 3)) + ((columns.size - 1) * columnGap)

        val headerLines = tablist.header?.formattedText?.split("\n") ?: emptyList()
        val footerLines = tablist.footer?.formattedText?.split("\n") ?: emptyList()
        var maxExtraWidth = 0
        (headerLines + footerLines).forEach {
            maxExtraWidth = maxOf(maxExtraWidth, with(Render2D) { it.width() })
        }

        val boxWidth = maxOf(totalGridWidth, minOf(maxExtraWidth, 400)) + (padding * 2)
        val rowHeight = font.lineHeight + entrySpacing
        val gridHeight = (columns.maxOfOrNull { it.size } ?: 0) * rowHeight
        val headerHeight = headerLines.size * 9
        val footerHeight = footerLines.size * 9
        val boxHeight = headerHeight + gridHeight + footerHeight + (padding * 2) + 5

        val x = (graphics.guiWidth() / 2) - (boxWidth / 2)
        val y = 10

        Render2D.drawRect(graphics, x, y, boxWidth, boxHeight, Color(15, 15, 15, 200))
        Render2D.drawRect(graphics, x, y, boxWidth, 2, ClickGui.accsentColor.value)
        Render2D.drawRect(graphics, x - 1, y - 1, boxWidth + 2, boxHeight + 2, Color(255, 255, 255, 15))

        var currentY = y + padding

        headerLines.forEach { line ->
            Render2D.drawCenteredString(graphics, line, x + boxWidth / 2, currentY, Color.WHITE, shadow = true)
            currentY += 9
        }
        currentY += 4

        var currentColumnX = x + (boxWidth - totalGridWidth) / 2

        columns.forEachIndexed { colIndex, column ->
            val thisColumnWidth = columnWidths[colIndex]

            column.forEachIndexed { rowIndex, player ->
                val entryY = currentY + (rowIndex * rowHeight)

                Render2D.drawPlayerHead(graphics, currentColumnX, entryY, headSize, player.skin.body.id())

                val name = player.tabListDisplayName?.formattedText ?: player.profile.name
                Render2D.drawString(graphics, name, currentColumnX + headSize + 3, entryY, Color.WHITE)
            }

            currentColumnX += (headSize + 3 + thisColumnWidth + columnGap)
        }

        currentY += gridHeight + 6

        footerLines.forEach { line ->
            Render2D.drawCenteredString(graphics, line, x + boxWidth / 2, currentY, Color.WHITE, shadow = true)
            currentY += 9
        }
    }
}