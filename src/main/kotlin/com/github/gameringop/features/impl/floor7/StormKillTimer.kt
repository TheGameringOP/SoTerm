package com.github.gameringop.features.impl.floor7

import com.github.gameringop.event.impl.ChatMessageEvent
import com.github.gameringop.event.impl.TickEvent
import com.github.gameringop.event.impl.WorldChangeEvent
import com.github.gameringop.features.Feature
import com.github.gameringop.ui.clickgui.components.getValue
import com.github.gameringop.ui.clickgui.components.impl.DropdownSetting
import com.github.gameringop.ui.clickgui.components.provideDelegate
import com.github.gameringop.ui.clickgui.components.section
import com.github.gameringop.utils.NumbersUtils.toFixed
import com.github.gameringop.utils.dungeons.DungeonListener
import com.github.gameringop.utils.dungeons.enums.DungeonClass
import com.github.gameringop.utils.location.LocationUtils
import com.github.gameringop.utils.render.Render2D
import com.github.gameringop.utils.render.Render2D.width

object StormKillTimer: Feature("Display when to release lb in py","Storm Kill Timer") {
    private val timerUnit by DropdownSetting("Timer Unit", 0, listOf("Seconds", "Ticks")).section("Timings")

    private val storeMessages = setOf("[BOSS] Storm: ENERGY HEED MY CALL!", "[BOSS] Storm: THUNDER LET ME BE YOUR CATALYST!")
    private var tickCounter = 0
    private var countTicks = false

    override fun init() {
        hudElement("Storm Kill Timer", centered = true, shouldDraw = { LocationUtils.inDungeon }) { ctx, example ->
            if (! example && ! countTicks) return@hudElement 0f to 0f
            val timeLeft = if (example) 142 else 142 - tickCounter
            val color = when {
                timeLeft > 95 -> "&a"
                timeLeft >= 47 -> "&6"
                else -> "&c"
            }
            val displayTime = when (timerUnit.value) {
                0 -> "$color${(timeLeft / 20.0).toFixed(2)}s"
                else -> "$color${timeLeft}t"
            }
            Render2D.drawCenteredString(ctx, displayTime, 0, 0)
            return@hudElement displayTime.width().toFloat() to 9f
        }

        register<ChatMessageEvent> {
            if (LocationUtils.dungeonFloorNumber != 7) return@register
            if (! LocationUtils.inBoss) return@register
            if (DungeonListener.thePlayer?.clazz != DungeonClass.Archer) return@register
            if (event.unformattedText !in storeMessages) return@register
            countTicks = true
        }

        register<TickEvent.Server> {
            if (countTicks) {
                tickCounter ++
                if (tickCounter > 142) {
                    tickCounter = 0
                    countTicks = false
                }
            }
        }

        register<WorldChangeEvent> {
            countTicks = false
            tickCounter = 0
        }
    }
}