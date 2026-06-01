package com.github.gameringop.features.impl.combat

import com.github.gameringop.event.impl.PlayerInteractEvent
import com.github.gameringop.features.Feature
import com.github.gameringop.ui.clickgui.components.impl.SliderSetting
import com.github.gameringop.utils.PlayerUtils
import com.github.gameringop.utils.ThreadUtils
import com.github.gameringop.utils.items.ItemUtils.skyblockId
import com.github.gameringop.utils.location.LocationUtils
import com.github.gameringop.utils.location.WorldType
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items

object DaggerSwapper: Feature(name = "Dagger Swapper", description = "High-speed dagger swapping with 30-tick retry logic.") {
    private val swapDelay by SliderSetting("Slot Swap Delay", 1, 0, 10, 1)
    private val safetyBuffer by SliderSetting("Global Lock (ms)", 400.0, 100.0, 1000.0, 10.0)

    private var lastActionTime = 0L
    private var isExecuting = false

    private var pendingAttunement: Attunement? = null
    private var pendingTimestamp = 0L

    private enum class Attunement(val display: String, val daggerIds: Set<String>, val expectedItem: Item) {
        ASHEN("ASHEN ♨", setOf("HEARTFIRE_DAGGER", "BURSTFIRE_DAGGER", "FIREDUST_DAGGER"), Items.STONE_SWORD),
        AURIC("AURIC ♨", setOf("HEARTFIRE_DAGGER", "BURSTFIRE_DAGGER", "FIREDUST_DAGGER"), Items.GOLDEN_SWORD),
        CRYSTAL("CRYSTAL ♨", setOf("HEARTMAW_DAGGER", "BURSTMAW_DAGGER", "MAWDUST_DAGGER"), Items.DIAMOND_SWORD),
        SPIRIT("SPIRIT ♨", setOf("HEARTMAW_DAGGER", "BURSTMAW_DAGGER", "MAWDUST_DAGGER"), Items.IRON_SWORD);

        companion object {
            fun fromName(text: String): Attunement? = entries.find { text.contains(it.display, ignoreCase = true) }
        }
    }

    override fun init() {
        register<PlayerInteractEvent.LEFT_CLICK.ENTITY> {
            if (!enabled || isExecuting || LocationUtils.world != WorldType.CrimsonIsle) return@register

            val now = System.currentTimeMillis()

            if (now - lastActionTime < safetyBuffer.value) return@register

            val target = event.entity
            val entities = mc.level?.getEntities(target, target.boundingBox.inflate(2.5)) ?: return@register

            for (entity in entities) {
                if (entity !is ArmorStand) continue
                val name = entity.customName?.string ?: continue
                if ("♨" !in name) continue

                val found = Attunement.fromName(name) ?: continue

                if (isCorrectItemHeld(found)) {
                    pendingAttunement = null
                    return@register
                }

                if (found == pendingAttunement) {
                    val timeSinceAttempt = now - pendingTimestamp

                    if (timeSinceAttempt < 1500) {
                        return@register
                    }
                }

                executeSwap(found)
                break
            }
        }
    }

    private fun isCorrectItemHeld(attunement: Attunement): Boolean {
        val player = mc.player ?: return false
        val heldStack = player.inventory.getItem(player.inventory.selectedSlot)
        return heldStack.skyblockId.uppercase() in attunement.daggerIds && heldStack.item == attunement.expectedItem
    }

    private fun executeSwap(attunement: Attunement) {
        val player = mc.player ?: return

        isExecuting = true
        lastActionTime = System.currentTimeMillis()

        pendingAttunement = attunement
        pendingTimestamp = lastActionTime

        try {
            val currentSlot = player.inventory.selectedSlot
            val heldStack = player.inventory.getItem(currentSlot)
            val heldId = heldStack.skyblockId.uppercase()

            if (heldId in attunement.daggerIds) {
                PlayerUtils.rightClick()

                ThreadUtils.scheduledTask(6) {
                    isExecuting = false
                }
                return
            }

            var targetSlot = -1
            for (i in 0..8) {
                if (player.inventory.getItem(i).skyblockId.uppercase() in attunement.daggerIds) {
                    targetSlot = i
                    break
                }
            }

            if (targetSlot != -1) {
                PlayerUtils.swapToSlot(targetSlot)

                val tickDelay = swapDelay.value.coerceAtLeast(1)

                ThreadUtils.scheduledTask(tickDelay) {
                    val newHeld = mc.player?.inventory?.getItem(targetSlot) ?: run {
                        isExecuting = false
                        return@scheduledTask
                    }

                    if (newHeld.item != attunement.expectedItem) {
                        PlayerUtils.rightClick()
                    }

                    ThreadUtils.scheduledTask(6) {
                        isExecuting = false
                    }
                }
            } else {
                isExecuting = false
                pendingAttunement = null
            }
        } catch (e: Exception) {
            isExecuting = false
            pendingAttunement = null
        }
    }
}