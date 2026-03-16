package com.github.gameringop.features.impl.misc

import com.github.gameringop.SoTerm
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
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Style
import java.awt.Color

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
        totalDiamonds = 0L
        totalSeconds = 0
        ChatUtils.modMessage("§aDiamond stats reset!")
    }.showIf { showProfit.value }

    private var totalDiamonds = 0L
    private var totalSeconds = 0
    private var isInDwarvenMines = false

    private val sackRegex = Regex("""\[Sacks\] \+([\d,]+) items.*\(Last (\d+)s\.\)""")
    
    private val diamondRegex = Regex("""([+-])\s*([\d,]+)\s+.*?(Enchanted Diamond|Diamond)""")

    private val hud by hudElement(
        name = "Diamond Profit",
        enabled = { LocationUtils.inSkyblock },
        shouldDraw = { showProfit.value && isInDwarvenMines }
    ) { ctx, demo ->
        val multiplier = 8
        if (demo) {
            Render2D.drawString(ctx, "§a${totalText.value} §f128,000", 0, 0, textColor.value)
            Render2D.drawString(ctx, "§a${hourlyText.value} §f6,400", 0, 10, textColor.value)
        } else {
            val coins = totalDiamonds * multiplier
            val totalVal = numFormat(coins)
            val hrRate = if (totalSeconds > 0) numFormat((coins / totalSeconds) * 3600) else "0"

            Render2D.drawString(ctx, "§a${totalText.value} §f$totalVal", 0, 0, textColor.value)
            Render2D.drawString(ctx, "§a${hourlyText.value} §f$hrRate", 0, 10, textColor.value)
        }
        200f to 30f
    }

    override fun init() {
        register<TickEvent.Server> {
            isInDwarvenMines = LocationUtils.world == WorldType.DwarvenMines
        }

        register<ChatMessageEvent> {
            if (!isInDwarvenMines) return@register
            
            val msg = event.unformattedText
            val match = sackRegex.find(msg) ?: return@register
            
            totalSeconds += match.groupValues[2].toIntOrNull() ?: 0

            val diamondsFound = extractDiamonds(event)
            totalDiamonds += diamondsFound
            if (totalDiamonds < 0) totalDiamonds = 0
        }

        register<WorldChangeEvent> { isInDwarvenMines = false }
    }
    
    private fun extractDiamonds(event: ChatMessageEvent): Int {
        var total = 0
        val isDebug = SoTerm.debugFlags.contains("diamonds")

        val detailedHoverText = event.component?.siblings?.firstNotNullOfOrNull { sibling ->
            val hoverEvent = sibling.style.hoverEvent ?: return@firstNotNullOfOrNull null
            
            val hoverComponent = when (hoverEvent.action()) {
                HoverEvent.Action.SHOW_TEXT -> (hoverEvent as HoverEvent.ShowText).value
                else -> null
            } ?: return@firstNotNullOfOrNull null
            
            val text = hoverComponent.string
            if (text.contains("Added") || text.contains("Removed")) text else null
        } ?: return 0

        if (isDebug) ChatUtils.modMessage("§6[Debug] Found Sack Hover Data")
        
        detailedHoverText.split("\n").forEach { line ->
            val cleanLine = line.replace(Regex("§."), "").trim()
            val m = diamondRegex.find(cleanLine) ?: return@forEach
            
            val sign = if (m.groupValues[1] == "-") -1 else 1
            val amt = m.groupValues[2].replace(",", "").toIntOrNull() ?: 0
            val type = m.groupValues[3]
            
            val baseValue = if (type == "Enchanted Diamond") amt * 160 else amt
            total += (baseValue * sign)
            
            if (isDebug) {
                ChatUtils.modMessage("§b[Debug] ${if(sign > 0) "+" else "-"}$baseValue Diamonds ($type)")
            }
        }
        
        return total
    }

    private fun numFormat(num: Long): String = 
        num.toString().replace(Regex("""\B(?=(\d{3})+(?!\d))"""), ",")
}
