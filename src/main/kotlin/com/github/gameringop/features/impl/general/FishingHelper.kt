package com.github.gameringop.features.impl.general

import com.github.gameringop.event.impl.MainThreadPacketReceivedEvent
import com.github.gameringop.event.impl.TickEvent
import com.github.gameringop.event.impl.WorldChangeEvent
import com.github.gameringop.features.Feature
import com.github.gameringop.ui.clickgui.components.impl.SliderSetting
import com.github.gameringop.ui.clickgui.components.impl.ToggleSetting
import com.github.gameringop.utils.ChatUtils.removeFormatting
import com.github.gameringop.utils.PlayerUtils
import com.github.gameringop.utils.ThreadUtils
import com.github.gameringop.utils.location.LocationUtils
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.world.entity.projectile.FishingHook
import net.minecraft.world.item.Items
import java.util.*
import kotlin.jvm.optionals.getOrNull

object FishingHelper: Feature(name = "Fishing Helper", description = "Auto pull on bite and optional auto recast while fishing in Skyblock.") {
    private val autoPull by ToggleSetting("Auto pull", true).section("Auto pull")
    private val pullDelay by SliderSetting("Delay", 1, 0, 5, 1, " ticks").showIf { autoPull.value }
    private val pullVariance by SliderSetting("Delay variance", 0, 0, 3, 1, " ticks").showIf { autoPull.value }

    private val autoRecast by ToggleSetting("Auto recast", false).section("Auto recast")
    private val recastCheck by ToggleSetting("Recast check", true).showIf { autoRecast.value }
        .withDescription("Every 15s, recast only if your line is not already in the water.")
    private val recastDelay by SliderSetting("Recast delay", 8, 0, 20, 1, " ticks")
        .showIf { autoRecast.value }
        .withDescription("Ticks to wait after reeling in before casting again.")
    private val recastVariance by SliderSetting("Delay variance", 0, 0, 5, 1, " ticks").showIf { autoRecast.value }

    private var busy = false
    private var recastTicks = 0
    private var lastBiteAt = 0L
    private var pullCooldownUntil = 0L

    override fun onDisable() {
        resetState()
        super.onDisable()
    }

    override fun init() {
        register<WorldChangeEvent> { resetState() }

        register<MainThreadPacketReceivedEvent.Pre> {
            if (! canDetectBite()) return@register
            val packet = event.packet as? ClientboundSetEntityDataPacket ?: return@register

            val hook = mc.player?.fishing ?: return@register
            if (! packetHasBiteName(packet)) return@register
            if (! isBiteEntity(hook, packet.id)) return@register

            onBiteDetected()
        }

        register<TickEvent.Start> {
            if (! LocationUtils.inSkyblock) return@register

            if (canDetectBite()) {
                detectBiteFromWorld()
            }

            if (! autoRecast.value || ! recastCheck.value || busy) return@register
            if (System.currentTimeMillis() < pullCooldownUntil) return@register

            if (++recastTicks < 15 * 20) return@register
            recastTicks = 0

            tryPeriodicRecast()
        }
    }

    private fun canDetectBite(): Boolean {
        return autoPull.value &&
            LocationUtils.inSkyblock &&
            ! busy &&
            System.currentTimeMillis() >= pullCooldownUntil
    }

    private fun detectBiteFromWorld() {
        val player = mc.player ?: return
        if (mc.screen != null) return

        val hook = player.fishing ?: return
        val level = mc.level ?: return

        val hasBite = level.getEntities(hook, hook.boundingBox.inflate(8.0)) { entity ->
            isBiteName(entity.customName)
        }.isNotEmpty()

        if (hasBite) onBiteDetected()
    }

    private fun packetHasBiteName(packet: ClientboundSetEntityDataPacket): Boolean {
        for (entry in packet.packedItems) {
            val optional = entry.value() as? Optional<*> ?: continue
            val component = optional.getOrNull() as? Component ?: continue
            if (isBiteName(component)) return true
        }
        return false
    }

    private fun isBiteEntity(hook: FishingHook, entityId: Int): Boolean {
        if (entityId == hook.id) return true
        val entity = mc.level?.getEntity(entityId) ?: return false
        return entity.distanceTo(hook) <= 8.0
    }

    private fun isBiteName(component: Component?): Boolean {
        if (component == null) return false
        val text = component.string.removeFormatting()
        return text == "!!!" || text.contains("!!!")
    }

    private fun onBiteDetected() {
        val now = System.currentTimeMillis()
        if (busy || now - lastBiteAt < 250L) return
        lastBiteAt = now
        schedulePull()
    }

    private fun schedulePull() {
        busy = true
        recastTicks = 0

        val pullTicks = (pullDelay.value + randomVariance(pullVariance.value)).coerceAtLeast(0)
        ThreadUtils.scheduledTask(pullTicks) {
            if (! enabled || ! LocationUtils.inSkyblock) {
                finishSequence()
                return@scheduledTask
            }

            useRod()

            if (! autoRecast.value) {
                finishSequence(cooldownMs = 800L)
                return@scheduledTask
            }

            val minRecastTicks = (2 + recastDelay.value + randomVariance(recastVariance.value)).coerceAtLeast(4)
            waitForClearHookThenRecast(0, minRecastTicks)
        }
    }

    private fun waitForClearHookThenRecast(waitedTicks: Int, minRecastTicks: Int) {
        if (! enabled || ! LocationUtils.inSkyblock) {
            finishSequence()
            return
        }

        val hookGone = mc.player?.fishing == null
        if (! hookGone && waitedTicks < 60) {
            ThreadUtils.scheduledTask(1) { waitForClearHookThenRecast(waitedTicks + 1, minRecastTicks) }
            return
        }

        if (waitedTicks < minRecastTicks) {
            ThreadUtils.scheduledTask(1) { waitForClearHookThenRecast(waitedTicks + 1, minRecastTicks) }
            return
        }

        useRod()
        finishSequence(cooldownMs = 1200L)
    }

    private fun tryPeriodicRecast() {
        val player = mc.player ?: return
        if (mc.screen != null) return
        if (! player.mainHandItem.`is`(Items.FISHING_ROD)) return

        val hook = player.fishing
        if (hook != null && hook.hookedIn == null) return

        useRod()
        pullCooldownUntil = System.currentTimeMillis() + 500L
    }

    private fun useRod() {
        ThreadUtils.runOnMcThread { PlayerUtils.rightClick() }
    }

    private fun finishSequence(cooldownMs: Long = 0L) {
        busy = false
        if (cooldownMs > 0L) pullCooldownUntil = System.currentTimeMillis() + cooldownMs
    }

    private fun resetState() {
        busy = false
        recastTicks = 0
        lastBiteAt = 0L
        pullCooldownUntil = 0L
    }

    private fun randomVariance(max: Int): Int = if (max > 0) (0..max).random() else 0
}
