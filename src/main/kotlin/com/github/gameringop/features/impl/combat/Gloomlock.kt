package com.github.gameringop.features.impl.combat

import com.github.gameringop.event.impl.PlayerInteractEvent
import com.github.gameringop.features.Feature
import com.github.gameringop.ui.clickgui.components.impl.ToggleSetting
import com.github.gameringop.utils.ActionBarParser
import com.github.gameringop.utils.items.ItemUtils.skyblockId

object Gloomlock: Feature(name = "Gloomlock Helper", description = "Prevents accidental clicks on the Gloomlock Grimoire when it would be wasted.") {
    private val blockLeftClick by ToggleSetting("Block Left Click", false)
    private val blockRightClick by ToggleSetting("Block Right Click", false)

    override fun init() {
        register<PlayerInteractEvent.LEFT_CLICK.AIR> {
            if (!blockLeftClick.value) return@register
            val player = mc.player ?: return@register
            if (player.mainHandItem.skyblockId != "GLOOMLOCK_GRIMOIRE") return@register
            val overflowMana = ActionBarParser.overflowMana
            val healthPercent = player.health / player.maxHealth
            if (overflowMana == 600 || healthPercent < 0.3) {
                event.isCanceled = true
            }
        }

        register<PlayerInteractEvent.LEFT_CLICK.BLOCK> {
            if (!blockLeftClick.value) return@register
            val player = mc.player ?: return@register
            if (player.mainHandItem.skyblockId != "GLOOMLOCK_GRIMOIRE") return@register
            val overflowMana = ActionBarParser.overflowMana
            val healthPercent = player.health / player.maxHealth
            if (overflowMana == 600 || healthPercent < 0.3) {
                event.isCanceled = true
            }
        }

        register<PlayerInteractEvent.RIGHT_CLICK.AIR> {
            if (!blockRightClick.value) return@register
            val player = mc.player ?: return@register
            if (player.mainHandItem.skyblockId != "GLOOMLOCK_GRIMOIRE") return@register
            val healthPercent = player.health / player.maxHealth
            if (healthPercent > 0.8) {
                event.isCanceled = true
            }
        }

        register<PlayerInteractEvent.RIGHT_CLICK.BLOCK> {
            if (!blockRightClick.value) return@register
            val player = mc.player ?: return@register
            if (player.mainHandItem.skyblockId != "GLOOMLOCK_GRIMOIRE") return@register
            val healthPercent = player.health / player.maxHealth
            if (healthPercent > 0.8) {
                event.isCanceled = true
            }
        }
    }
}