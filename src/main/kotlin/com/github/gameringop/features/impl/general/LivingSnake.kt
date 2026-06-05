package com.github.gameringop.features.impl.general

import com.github.gameringop.SoTerm
import com.github.gameringop.event.impl.BlockChangeEvent
import com.github.gameringop.event.impl.PlayerInteractEvent
import com.github.gameringop.event.impl.TickEvent
import com.github.gameringop.event.impl.WorldChangeEvent
import com.github.gameringop.features.Feature
import com.github.gameringop.ui.clickgui.components.impl.SliderSetting
import com.github.gameringop.utils.ChatUtils
import com.github.gameringop.utils.PlayerUtils
import com.github.gameringop.utils.PlayerUtils.findHotbarSlot
import com.github.gameringop.utils.PlayerUtils.swapToSlot
import com.github.gameringop.utils.items.ItemUtils.skyblockId
import com.github.gameringop.utils.location.LocationUtils
import com.github.gameringop.utils.location.WorldType
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import kotlin.random.Random

object LivingSnake: Feature("Swaps to Frozen Water Pungi after each mined snake segment, then swaps back to your pickaxe.") {
    private const val PICKAXE_ID = "SELF_RECURSIVE_PICKAXE"
    private const val PUNGI_ID = "FROZEN_WATER_PUNGI"
    private const val TARGET_TIMEOUT_MS = 750L

    private val minHoldTime by SliderSetting("Min Hold Time (ms)", 250, 100, 1000, 25)
        .withDescription("Minimum time to hold right click with the Frozen Water Pungi.")
        .section("Timing")

    private val maxHoldTime by SliderSetting("Max Hold Time (ms)", 350, 100, 1000, 25)
        .withDescription("Maximum time to hold right click with the Frozen Water Pungi. Randomised each time.")

    private val triggerCooldown by SliderSetting("Trigger Cooldown (ms)", 250, 0, 1000, 25)
        .withDescription("Minimum time between snake stun cycles.")

    private var lastTargetedPos: BlockPos? = null
    private var lastTargetedAt = 0L
    private var lastTriggerAt = 0L

    private var pendingPungiSlot: Int? = null
    private var returnSlot: Int? = null
    private var holdingUntil = 0L

    override fun onDisable() {
        releaseUse()
        resetState()
        super.onDisable()
    }

    override fun init() {
        register<WorldChangeEvent> {
            releaseUse()
            resetState()
        }

        register<PlayerInteractEvent.LEFT_CLICK.BLOCK> {
            if (LocationUtils.world != WorldType.Rift) return@register
            if (mc.player?.mainHandItem?.skyblockId != PICKAXE_ID) return@register
            if (!isSnakeSegment(mc.level?.getBlockState(event.pos) ?: return@register)) return@register

            lastTargetedPos = event.pos
            lastTargetedAt = System.currentTimeMillis()
        }

        register<BlockChangeEvent> {
            if (LocationUtils.world != WorldType.Rift) return@register
            if (!event.newState.isAir) return@register
            if (!isSnakeSegment(event.oldState)) return@register

            val player = mc.player ?: return@register
            if (player.mainHandItem.skyblockId != PICKAXE_ID) return@register

            val now = System.currentTimeMillis()
            if (now - lastTargetedAt > TARGET_TIMEOUT_MS) return@register
            if (event.pos != lastTargetedPos) return@register
            if (now - lastTriggerAt < triggerCooldown.value.toLong()) return@register
            if (pendingPungiSlot != null || holdingUntil > now) return@register

            val pungiSlot = findHotbarSlot { it.skyblockId == PUNGI_ID } ?: return@register

            returnSlot = player.inventory.selectedSlot
            pendingPungiSlot = pungiSlot
            lastTriggerAt = now
        }

        register<TickEvent.Start> {
            val now = System.currentTimeMillis()

            pendingPungiSlot?.let { slot ->
                swapToSlot(slot)
                mc.options.keyUse.isDown = true
                PlayerUtils.rightClick()

                val min = minHoldTime.value.toLong()
                val max = maxHoldTime.value.toLong()
                val hold = if (min >= max) min else Random.nextLong(min, max + 1)
                holdingUntil = now + hold

                if (SoTerm.debugFlags.contains("snake")) {
                    ChatUtils.modMessage("§7[Snake] §aHolding right click for §e${hold}ms")
                }

                pendingPungiSlot = null
                return@register
            }

            if (holdingUntil <= 0L) return@register

            if (now < holdingUntil) {
                mc.options.keyUse.isDown = true
                return@register
            }

            releaseUse()
            returnSlot?.let(::swapToSlot)
            resetState()
        }
    }

    private fun isSnakeSegment(state: BlockState): Boolean {
        return state.`is`(Blocks.LAPIS_BLOCK) || state.`is`(Blocks.LIGHT_BLUE_STAINED_GLASS)
    }

    private fun releaseUse() {
        mc.options.keyUse.isDown = false
    }

    private fun resetState() {
        pendingPungiSlot = null
        returnSlot = null
        holdingUntil = 0L
        lastTargetedPos = null
        lastTargetedAt = 0L
    }
}