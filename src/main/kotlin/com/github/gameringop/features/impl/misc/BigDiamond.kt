package com.github.gameringop.features.impl.misc

import com.github.gameringop.SoTerm
import com.github.gameringop.SoTerm.mc
import com.github.gameringop.event.impl.ChatMessageEvent
import com.github.gameringop.event.impl.TickEvent
import com.github.gameringop.event.impl.WorldChangeEvent
import com.github.gameringop.features.Feature
import com.github.gameringop.ui.clickgui.components.getValue
import com.github.gameringop.ui.clickgui.components.impl.ButtonSetting
import com.github.gameringop.ui.clickgui.components.impl.ColorSetting
import com.github.gameringop.ui.clickgui.components.impl.TextInputSetting
import com.github.gameringop.ui.clickgui.components.impl.ToggleSetting
import com.github.gameringop.ui.clickgui.components.provideDelegate
import com.github.gameringop.ui.clickgui.components.section
import com.github.gameringop.ui.clickgui.components.showIf
import com.github.gameringop.ui.clickgui.components.withDescription
import com.github.gameringop.ui.hud.HudElement
import com.github.gameringop.ui.hud.getValue
import com.github.gameringop.ui.hud.provideDelegate
import com.github.gameringop.utils.ChatUtils
import com.github.gameringop.utils.location.LocationUtils
import com.github.gameringop.utils.location.WorldType
import com.github.gameringop.utils.render.Render2D
import com.github.gameringop.utils.render.Render2D.width
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Style
import java.awt.Color
import java.util.*

object BigDiamond : Feature("Diamond Profit Tracker for Dwarven Mines") {
    
    private val showProfit by ToggleSetting("Show Profit", true).section("Display")
    private val totalText by TextInputSetting("Total Text", "\$total >")
        .withDescription("Text before total diamonds count")
        .showIf { showProfit.value }
    private val hourlyText by TextInputSetting("Hourly Text", "\$hr >")
        .withDescription("Text before hourly rate")
        .showIf { showProfit.value }
    private val textColor by ColorSetting("Text Color", Color.WHITE).showIf { showProfit.value }
    
    private val resetButton by ButtonSetting("Reset Stats", false) {
        resetStats()
        ChatUtils.modMessage("§aDiamond stats reset!")
    }.showIf { showProfit.value }
    
    private var totalDiamonds = 0L
    private var totalSeconds = 0
    private var isInDwarvenMines = false
    
    private val sackRegex = Regex("\\[Sacks] \\+(\\d+) items. \\(Last (\\d+)s.\\)")
    private val diamondRegex = Regex("\\+([\\d,]+) (Enchanted Diamond|Diamond) \\(")
    
    private val hud by hudElement(
        name = "Diamond Profit",
        enabled = { LocationUtils.inSkyblock },
        shouldDraw = { showProfit.value && isInDwarvenMines }
    ) { ctx, demo ->
        if (demo) {
            Render2D.drawString(ctx, "§a${totalText.value} §f16,000", 0, 0, textColor.value)
            Render2D.drawString(ctx, "§a${hourlyText.value} §f800", 0, 10, textColor.value)
        } else {
            val totalValue = formatNumber(totalDiamonds)
            val hourlyRate = if (totalSeconds > 0) {
                formatNumber(((totalDiamonds) / totalSeconds) * 3600)
            } else "0"
            
            Render2D.drawString(ctx, "§a${totalText.value} §f$totalValue", 0, 0, textColor.value)
            Render2D.drawString(ctx, "§a${hourlyText.value} §f$hourlyRate", 0, 10, textColor.value)
        }
        0f to 20f
    }
    
    override fun init() {
        register<TickEvent.Server> {
            val inMines = LocationUtils.world == WorldType.DwarvenMines
            if (inMines && !isInDwarvenMines) {
                isInDwarvenMines = true
            } else if (!inMines && isInDwarvenMines) {
                isInDwarvenMines = false
            }
        }
        
        register<ChatMessageEvent> {
            if (!isInDwarvenMines) return@register
            
            val message = event.unformattedText
            val match = sackRegex.find(message) ?: return@register
            val seconds = match.groupValues[2].toIntOrNull() ?: return@register
            
            totalSeconds += seconds
            
            val diamonds = extractDiamondsFromHover(event)
            if (diamonds > 0) {
                totalDiamonds += diamonds
            }
        }
        
        register<WorldChangeEvent> {
            isInDwarvenMines = false
        }
    }
    
    private fun extractDiamondsFromHover(event: ChatMessageEvent): Int {
        var total = 0
        
        event.component?.let { component ->
            component.visit({ style: Style, text: String ->
                val hover = style.hoverEvent
                
                if (hover != null && hover.action() == HoverEvent.Action.SHOW_TEXT) {
                    val content = hover.contents as? net.minecraft.network.chat.Component
                    val hoverText = content?.string ?: return@visit Optional.empty<String>()
                    
                    if (hoverText.contains("Diamond")) {
                        val lines = hoverText.split("\n")
                        lines.forEach { line ->
                            val match = diamondRegex.find(line)
                            if (match != null) {
                                val amount = match.groupValues[1].replace(",", "").toIntOrNull() ?: 0
                                val isEnchanted = line.contains("Enchanted Diamond")
                                
                                total += if (isEnchanted) amount * 160 else amount
                            }
                        }
                    }
                }
                Optional.empty<String>()
            }, Style.EMPTY)
        }
        
        return total
    }
    
    private fun resetStats() {
        totalDiamonds = 0
        totalSeconds = 0
    }
    
    private fun formatNumber(num: Long): String {
        return num.toString().replace(Regex("\\B(?=(\\d{3})+(?!\\d))"), ",")
    }
}
