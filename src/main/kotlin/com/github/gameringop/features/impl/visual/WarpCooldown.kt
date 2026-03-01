package com.github.gameringop.features.impl.visual

import com.github.gameringop.event.impl.ChatMessageEvent
import com.github.gameringop.features.Feature
import com.github.gameringop.utils.location.LocationUtils
import com.github.gameringop.utils.render.Render2D
import com.github.gameringop.utils.render.Render2D.height
import com.github.gameringop.utils.render.Render2D.width
import kotlin.math.roundToInt

object WarpCooldown: Feature("Displays on screen how long until you can start another dungeon run.") {
    private val floorEnterRegex = Regex("-+\\s.+ entered.+The Catacombs, Floor [IVX]+!\\s-+")
    private var startTime = System.currentTimeMillis()
    private var onCd = false

    private val warpCooldown = hudElement("WarpCooldown", { onCd }) { ctx, example ->
        val remaining = (30 - (System.currentTimeMillis() - startTime) / 1000.0).roundToInt()
        if (remaining < 0) onCd = false
        val text = "&bWarp Cooldown: &f${if (example) 30 else remaining}s"
        Render2D.drawString(ctx, text, 0, 0)
        return@hudElement text.width().toFloat() to text.height().toFloat()
    }

    override fun init() {
        register<ChatMessageEvent> {
            if (! LocationUtils.onHypixel) return@register
            if (onCd) return@register
            if (! event.unformattedText.matches(floorEnterRegex)) return@register
            startTime = System.currentTimeMillis()
            onCd = true
        }
    }
}