package com.github.gameringop.features.impl.combat

import com.github.gameringop.event.impl.RenderWorldEvent
import com.github.gameringop.event.impl.SlayerEvent
import com.github.gameringop.event.impl.TickEvent
import com.github.gameringop.features.Feature
import com.github.gameringop.ui.clickgui.components.impl.ToggleSetting
import com.github.gameringop.utils.NumbersUtils.toFixed
import com.github.gameringop.utils.StringUtils.stripped
import com.github.gameringop.utils.items.ItemUtils.skyblockId
import com.github.gameringop.utils.render.Render2D
import com.github.gameringop.utils.render.Render2D.width
import net.minecraft.world.entity.decoration.ArmorStand

object VengeanceTimer: Feature("Shows the time until your vengeance damage should activate.") {
    private val showHud by ToggleSetting("Show HUD", true)
    private val compact by ToggleSetting("Compact Display", false)
    private val useTicks by ToggleSetting("Use Ticks", true)

    private val abilityIds = listOf("HEARTFIRE_DAGGER", "BURSTFIRE_DAGGER", "FIREDUST_DAGGER")
    private var isCounting = false
    private var ticksLeft = 120

    override fun init() {
        hudElement(
            name = "Vengeance Timer HUD",
            enabled = { showHud.value },
            shouldDraw = { isCounting },
            centered = true
        ) { ctx, example ->
            val value = when {
                example && useTicks.value -> "120"
                example -> "6.0"
                useTicks.value -> "$ticksLeft"
                else -> (ticksLeft / 20.0).toFixed(1)
            }

            val suffix = if (!useTicks.value && !example) "s" else ""
            val text = if (compact.value) "$value$suffix" else "§cVengeance Timer: §f$value$suffix"

            Render2D.drawCenteredString(ctx, text, 0, 0)
            return@hudElement text.width().toFloat() to 9f
        }

        register<TickEvent.End> {
            if (isCounting) {
                if (ticksLeft > 0) ticksLeft-- else reset()
            }
        }

        register<RenderWorldEvent> {
            if (isCounting) return@register
            val world = mc.level ?: return@register
            val player = mc.player ?: return@register

            val ashen = world.getEntities(player, player.boundingBox.inflate(10.0)) {
                it is ArmorStand && it.customName?.string?.stripped()?.contains("ASHEN ♨7") == true
            }

            if (ashen.isNotEmpty()) {
                val heldId = player.mainHandItem.skyblockId
                if (heldId in abilityIds) isCounting = true
            }
        }

        register<SlayerEvent.Boss.Death> { reset() }
        register<SlayerEvent.Reset.Any> { reset() }
    }

    private fun reset() {
        isCounting = false
        ticksLeft = 120
    }
}