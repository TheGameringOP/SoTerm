package com.github.gameringop.features.impl.visual

import com.github.gameringop.event.impl.ChatMessageEvent
import com.github.gameringop.event.impl.TickEvent
import com.github.gameringop.event.impl.WorldChangeEvent
import com.github.gameringop.features.Feature
import com.github.gameringop.utils.DataDownloader
import com.github.gameringop.utils.NumbersUtils.toFixed
import com.github.gameringop.utils.dungeons.DungeonListener
import com.github.gameringop.utils.dungeons.map.DungeonInfo
import com.github.gameringop.utils.location.LocationUtils
import com.github.gameringop.utils.render.Render2D
import com.github.gameringop.utils.render.Render2D.width
import java.util.concurrent.ConcurrentHashMap

object RunSplits: Feature("A Splits HUD for Dungeons.") {
    private val runEndRegex = Regex("^\\s*☠ Defeated (.+) in 0?([\\dhms ]+?)\\s*(\\(NEW RECORD!\\))?$")
    private val floorSplits by lazy {
        DataDownloader.loadJson<Map<String, List<Map<String, String?>>>>("runSplits.json").mapValues {
            it.value.map { entryMap ->
                DialogueEntry(
                    entryMap["name"] ?: error("Missing 'name'"),
                    entryMap["start"],
                    entryMap["end"]
                )
            }
        }
    }

    private val currentFloorSplits = ConcurrentHashMap<String, Split>()

    private var currentText: List<String> = emptyList()
    private val exampleText = listOf(
        "§8Wither Doors: §7?",
        "§4Blood Open: ?",
        "§cWatcher Clear: ?",
        "§dPortal: ?",
        "§aBoss Entry: ?"
    )

    private data class Split(var start: Long? = null, var end: Long? = null)
    private data class DialogueEntry(val name: String, val start: String? = null, val end: String? = null) {
        fun startMatches(msg: String) = start == msg || start?.toRegex()?.matches(msg) == true
        fun endMatches(msg: String) = end == msg || end?.toRegex()?.matches(msg) == true
    }

    override fun init() {
        hudElement("Run Splits", shouldDraw = { LocationUtils.inDungeon }) { ctx, example ->
            val text = if (example) exampleText else currentText
            if (text.isEmpty()) return@hudElement 0f to 0f

            var currentY = 0f
            var width = 0f
            text.forEach { line ->
                Render2D.drawString(ctx, line, 0, currentY)
                width = maxOf(width, line.width().toFloat())
                currentY += 9f
            }

            return@hudElement width to currentY
        }

        register<WorldChangeEvent> {
            currentFloorSplits.clear()
        }

        register<TickEvent.Start> {
            if (! LocationUtils.inDungeon) return@register

            val start = DungeonListener.dungeonStartTime ?: 0L
            val blood = DungeonListener.bloodOpenTime
            val watcher = DungeonListener.watcherClearTime
            val boss = DungeonListener.bossEntryTime

            fun formatTime(ms: Long) = "${(ms / 20) / 60}m ${(ms / 20) % 60}s"
            fun formatSecs(ms: Long) = "${ms / 20}s"
            fun formatDec(ms: Long) = "${(ms / 20.0).toFixed(1)}s"

            val bloodOpen = when {
                blood == null && DungeonListener.dungeonStarted -> formatTime(DungeonListener.currentTime - start)
                blood != null -> formatTime(blood - start)
                else -> "?"
            }

            val watcherClear = when {
                watcher != null && blood != null -> formatSecs(watcher - blood)
                blood != null -> formatSecs(DungeonListener.currentTime - blood)
                else -> "?"
            }

            val portalTime = when {
                watcher != null && boss == null -> formatDec(DungeonListener.currentTime - watcher)
                watcher != null && boss != null -> formatDec(boss - watcher)
                else -> "?"
            }

            val bossEntry = if (DungeonListener.dungeonStarted) formatTime((boss ?: DungeonListener.currentTime) - start) else "?"

            val splitLines = mutableListOf<String>()

            currentFloorSplits.forEach { (name, split) ->
                val text = when {
                    split.start != null && split.end != null -> {
                        val duration = ((split.end !! - split.start !!) / 20.0).toFixed(2)
                        "$name: ${duration}s§r"
                    }

                    split.start != null && split.end == null -> {
                        val live = ((DungeonListener.currentTime - split.start !!) / 20.0).toFixed(2)
                        "$name: ${live}s§r"
                    }

                    else -> return@forEach
                }
                splitLines.add(text)
            }

            val clearInfo = listOf(
                "§8Wither Doors: §7${DungeonInfo.witherDoors}",
                "§4Blood Open: $bloodOpen",
                "§cWatcher Clear: $watcherClear",
                "§dPortal: $portalTime",
                "§aBoss Entry: $bossEntry"
            )

            currentText = if (splitLines.isEmpty()) clearInfo
            else clearInfo + "-----------------------" + splitLines
        }

        register<ChatMessageEvent> {
            if (! LocationUtils.inDungeon) return@register

            val msg = event.unformattedText.trim()
            val floor = LocationUtils.dungeonFloor ?: return@register
            val currentSplits = floorSplits[floor] ?: floorSplits[floor.replace("M", "F")] ?: return@register

            currentSplits.forEachIndexed { i, entry ->
                val split = currentFloorSplits.getOrPut(entry.name) { Split() }

                if (entry.startMatches(msg) || (i > 0 && currentSplits[i - 1].endMatches(msg))) {
                    split.start = DungeonListener.currentTime
                }

                if (entry.endMatches(msg) || (entry.end == null && runEndRegex.matches(msg))) {
                    split.end = DungeonListener.currentTime
                }

                if (split.start != null || split.end != null) {
                    currentFloorSplits[entry.name] = split
                }
            }
        }
    }
}