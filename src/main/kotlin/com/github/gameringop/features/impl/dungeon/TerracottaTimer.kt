package com.github.gameringop.features.impl.dungeon

import com.github.gameringop.event.impl.BlockChangeEvent
import com.github.gameringop.event.impl.ChatMessageEvent
import com.github.gameringop.event.impl.RenderWorldEvent
import com.github.gameringop.event.impl.WorldChangeEvent
import com.github.gameringop.features.Feature
import com.github.gameringop.utils.NumbersUtils.toFixed
import com.github.gameringop.utils.ThreadUtils
import com.github.gameringop.utils.dungeons.DungeonListener
import com.github.gameringop.utils.location.LocationUtils
import com.github.gameringop.utils.render.Render3D
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.FlowerPotBlock
import java.util.concurrent.CopyOnWriteArrayList

object TerracottaTimer: Feature("Displays a timer until Terracottas respawn in F6/M6") {
    private var terracottaSpawns = CopyOnWriteArrayList<Pair<BlockPos, Long>>()

    override fun init() {
        register<WorldChangeEvent> { terracottaSpawns.clear() }

        register<BlockChangeEvent> {
            if (LocationUtils.dungeonFloorNumber != 6 || ! LocationUtils.inBoss) return@register
            if (event.newBlock !is FlowerPotBlock) return@register
            if (terracottaSpawns.any { it.first == event.pos }) return@register
            val time = if (LocationUtils.isMasterMode) 240 else 300
            val terracotta = Pair(event.pos, DungeonListener.currentTime + time)
            ThreadUtils.scheduledTaskServer(time) { terracottaSpawns.remove(terracotta) }
            terracottaSpawns.add(terracotta)
        }

        register<ChatMessageEvent> {
            if (LocationUtils.dungeonFloorNumber != 6 || ! LocationUtils.inBoss) return@register
            if (event.unformattedText == "[BOSS] Sadan: ENOUGH!") ThreadUtils.scheduledTaskServer(10) {
                terracottaSpawns.clear()
            }
        }

        register<RenderWorldEvent> {
            terracottaSpawns.ifEmpty { return@register }.forEach { (pos, time) ->
                val timeLeft = (time - DungeonListener.currentTime) / 20.0
                Render3D.renderString(timeLeft.toFixed(1), pos.center, phase = true, scale = 1.35)
            }
        }
    }
}