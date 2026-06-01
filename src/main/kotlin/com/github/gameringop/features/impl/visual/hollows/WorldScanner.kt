package com.github.gameringop.features.impl.visual.hollows

import com.github.gameringop.event.impl.RenderWorldEvent
import com.github.gameringop.event.impl.TickEvent
import com.github.gameringop.event.impl.WorldChangeEvent
import com.github.gameringop.features.Feature
import com.github.gameringop.ui.clickgui.components.impl.SliderSetting
import com.github.gameringop.ui.clickgui.components.impl.ToggleSetting
import com.github.gameringop.utils.ChatUtils
import com.github.gameringop.utils.ColorUtils.withAlpha
import com.github.gameringop.utils.location.LocationUtils
import com.github.gameringop.utils.location.WorldType
import com.github.gameringop.utils.render.Render3D
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.SlabBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.EnumProperty
import net.minecraft.world.level.block.state.properties.SlabType
import net.minecraft.world.level.chunk.LevelChunk
import net.minecraft.world.phys.Vec3
import java.awt.Color
import java.util.Collections
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.collections.ArrayDeque

object WorldScanner: Feature(name = "World Scanner", description = "Scans Crystal Hollows chunks for structures and highlights them.") {
    val grottoSettings = structureEsp("Fairy Grotto", Color(255, 85, 255))
    val grottoShowBlockCount by ToggleSetting("Show Number of Blocks", true)
        .showIf { grottoSettings.enable.value }
    val grottoBlockCountBackgroundOpacity by SliderSetting("Text Background Opacity", 0.5f, 0f, 1f, 0.05f)
        .showIf { grottoShowBlockCount.value }

    val sapphireSettings = structureEsp("Sapphire Crystal", Color(85, 255, 255))
    val amberSettings = structureEsp("Amber Crystal", Color(255, 170, 0))
    val amethystSettings = structureEsp("Amethyst Crystal", Color(170, 0, 170))
    val jadeSettings = structureEsp("Jade Crystal", Color(0, 170, 0))
    val topazSettings = structureEsp("Topaz Crystal", Color(255, 255, 85))
    val corleoneSettings = structureEsp("Corleone", Color(85, 255, 85))
    val goldenDragonSettings = structureEsp("Golden Dragon", Color(255, 255, 255))
    val keyGuardianSettings = structureEsp("Key Guardian", Color(170, 0, 170))
    val xalxSettings = structureEsp("Xalx", Color(80, 110, 0))
    val peteSettings = structureEsp("Pete", Color(110, 42, 0))
    val odawaSettings = structureEsp("Odawa", Color(170, 170, 170))
    val wormFishingSettings = structureEsp("Worm Fishing", Color(255, 85, 85))

    private val grottos = Collections.synchronizedList(mutableListOf<Triple<Pair<Int, Int>, BlockPos, Int>>())
    private val structures = Collections.synchronizedList(mutableListOf<Pair<Structure, Triple<Int, Int, Int>>>())
    private val scannedChunks = Collections.synchronizedSet(mutableSetOf<Pair<Int, Int>>())
    private val grottoChunksMap = Collections.synchronizedMap(mutableMapOf<Pair<Int, Int>, Triple<Pair<Int, Int>, BlockPos, Int>>())
    private val pendingChunks = ConcurrentLinkedQueue<LevelChunk>()

    override fun onDisable() {
        clearScanData()
        pendingChunks.clear()
        super.onDisable()
    }

    override fun init() {
        ClientChunkEvents.CHUNK_LOAD.register { _, chunk ->
            if (! enabled || ! inCrystalHollows()) return@register
            val key = chunk.pos.x to chunk.pos.z
            if (key in scannedChunks) return@register
            if (pendingChunks.any { it.pos == chunk.pos }) return@register
            pendingChunks.add(chunk)
        }

        register<WorldChangeEvent> { clearScanData() }

        register<TickEvent.End> {
            if (! enabled || ! inCrystalHollows()) return@register
            val chunk = pendingChunks.poll() ?: return@register
            val key = chunk.pos.x to chunk.pos.z
            if (key in scannedChunks) return@register
            scannedChunks.add(key)
            scanChunk(chunk)
        }

        register<RenderWorldEvent> {
            if (! inCrystalHollows()) return@register
            renderGrottos(event)
            renderStructures(event)
        }
    }

    private fun inCrystalHollows() = LocationUtils.inSkyblock && LocationUtils.world == WorldType.CrystalHollows

    private fun clearScanData() {
        grottos.clear()
        structures.clear()
        scannedChunks.clear()
        grottoChunksMap.clear()
        pendingChunks.clear()
    }

    private fun renderGrottos(event: RenderWorldEvent) {
        if (! grottoSettings.enable.value) return

        val config = grottoSettings
        val textScale = textScale(config.displayScale.value)

        for (grotto in grottos.toList()) {
            val blockPos = grotto.second
            val center = Vec3.atCenterOf(blockPos)
            val color = config.color.value

            renderAtBlock(event, blockPos, color, config.highlightStyle.value)

            if (config.tracer.value) {
                Render3D.renderTracer(event.ctx, center, color, 2f)
            }

            if (config.displayName.value) {
                Render3D.renderString(
                    "Fairy Grotto",
                    center.add(0.0, 10.0, 0.0),
                    color,
                    textScale,
                    phase = true,
                )
            }

            if (grottoShowBlockCount.value) {
                Render3D.renderString(
                    grotto.third.toString(),
                    center,
                    color,
                    textScale,
                    phase = true,
                )
            }
        }
    }

    private fun renderStructures(event: RenderWorldEvent) {
        for ((structure, pos) in structures.toList()) {
            val config = structure.config
            if (! config.enable.value) continue

            val blockPos = BlockPos(pos.first, pos.second, pos.third)
            val center = Vec3.atCenterOf(blockPos)
            val color = config.color.value

            renderAtBlock(event, blockPos, color, config.highlightStyle.value)

            if (config.tracer.value) {
                Render3D.renderTracer(event.ctx, center, color, 2f)
            }

            if (config.displayName.value) {
                Render3D.renderString(
                    structure.displayName,
                    center,
                    color,
                    textScale(config.displayScale.value),
                    phase = true,
                )
            }
        }
    }

    private fun renderAtBlock(event: RenderWorldEvent, pos: BlockPos, color: Color, style: Int) {
        val outline = style == 0 || style == 2
        val fill = style == 1 || style == 2
        val fillColor = if (fill) color.withAlpha((color.alpha * 0.35f).toInt().coerceIn(0, 255)) else color

        Render3D.renderBlock(
            ctx = event.ctx,
            pos = pos,
            outlineColor = color,
            fillColor = fillColor,
            outline = outline,
            fill = fill,
            phase = true,
        )
    }

    private fun textScale(scale: Float) = 0.75f + scale * 2.25f

    private fun matchesBlockProperty(
        state: BlockState,
        property: EnumProperty<*>,
        expected: Comparable<*>,
    ): Boolean {
        return when {
            property === SlabBlock.TYPE && expected is SlabType ->
                state.hasProperty(SlabBlock.TYPE) && state.getValue(SlabBlock.TYPE) == expected

            else -> false
        }
    }

    private fun scanStructure(chunk: LevelChunk, structure: Structure, x: Int, y: Int, z: Int): Boolean {
        if (structure == Structure.FAIRY_GROTTO) return false

        val worldX = chunk.pos.x * 16 + x
        val worldZ = chunk.pos.z * 16 + z

        if (structure == Structure.WORM_FISHING && (worldX < 513 || y < 80 || worldZ < 513)) return false

        val worldPos = BlockPos(worldX, y, worldZ)
        if (! structure.quarter.testPredicate(worldPos)) return false

        val blockPos = BlockPos.MutableBlockPos()
        for (structureY in structure.blocks.indices) {
            blockPos.set(x, y + structureY, z)
            val (block, enumProperty, expectedValue) = structure.blocks[structureY]
            if (block == null) continue

            val worldState = chunk.getBlockState(blockPos)
            if (! worldState.`is`(block)) return false

            if (enumProperty != null && expectedValue != null) {
                if (! matchesBlockProperty(worldState, enumProperty, expectedValue)) return false
            }
        }

        return true
    }

    private fun getAllNearbyGrottoChunks(x: Int, z: Int): List<Triple<Pair<Int, Int>, BlockPos, Int>> {
        val result = mutableListOf<Triple<Pair<Int, Int>, BlockPos, Int>>()
        val visited = mutableSetOf<Pair<Int, Int>>()
        val queue = ArrayDeque<Pair<Int, Int>>()
        queue.add(x to z)

        while (queue.isNotEmpty()) {
            val (cx, cz) = queue.removeFirst()
            val key = cx to cz
            if (! visited.add(key)) continue

            val current = grottoChunksMap[key] ?: continue
            result.add(current)

            for (dx in -1..1) {
                for (dz in -1..1) {
                    if (dx == 0 && dz == 0) continue
                    queue.add(cx + dx to cz + dz)
                }
            }
        }

        return result
    }

    private fun scanChunk(chunk: LevelChunk) {
        val structuresToScan = Structure.entries.filter { structure ->
            structure.config.enable.value && structures.none { it.first == structure }
        }

        val worldPos = BlockPos.MutableBlockPos()
        val chunkPos = BlockPos.MutableBlockPos()
        val chunkJasperBlocks = mutableListOf<BlockPos>()
        val chunkX = chunk.pos.x
        val chunkZ = chunk.pos.z
        val foundStructure = structures.mapTo(mutableSetOf()) { it.first }

        for (x in 0..15) {
            for (z in 0..15) {
                for (y in 0..169) {
                    val worldX = chunkX * 16 + x
                    val worldZ = chunkZ * 16 + z
                    worldPos.set(worldX, y, worldZ)

                    for (structureToScan in structuresToScan) {
                        if (structureToScan in foundStructure) continue
                        if (! scanStructure(chunk, structureToScan, x, y, z)) continue

                        foundStructure.add(structureToScan)

                        val coords = Triple(
                            worldX + structureToScan.offsetX,
                            y + structureToScan.offsetY,
                            worldZ + structureToScan.offsetZ,
                        )
                        structures.add(structureToScan to coords)
                        notifyFound(structureToScan, coords)
                    }

                    if (! grottoSettings.enable.value) continue

                    chunkPos.set(x, y, z)
                    val state = chunk.getBlockState(chunkPos)
                    if (! state.`is`(Blocks.MAGENTA_STAINED_GLASS_PANE) && ! state.`is`(Blocks.MAGENTA_STAINED_GLASS)) continue

                    worldPos.set(worldX, y, worldZ)
                    if (! CrystalHollowsQuarter.NUCLEUS.testPredicate(worldPos)) {
                        chunkJasperBlocks.add(worldPos.immutable())
                    }
                }
            }
        }

        if (chunkJasperBlocks.isEmpty()) return

        val size = chunkJasperBlocks.size
        val center = BlockPos(
            chunkJasperBlocks.sumOf { it.x } / size,
            chunkJasperBlocks.sumOf { it.y } / size,
            chunkJasperBlocks.sumOf { it.z } / size,
        )

        if (CrystalHollowsQuarter.NUCLEUS.testPredicate(center)) return

        grottoChunksMap[chunkX to chunkZ] = Triple(chunkX to chunkZ, center, chunkJasperBlocks.size)

        val cluster = getAllNearbyGrottoChunks(chunkX, chunkZ)
        if (cluster.isEmpty()) return

        val merged = BlockPos(
            cluster.sumOf { it.second.x } / cluster.size,
            cluster.sumOf { it.second.y } / cluster.size,
            cluster.sumOf { it.second.z } / cluster.size,
        )

        val numGrottos = grottos.size

        grottos.removeIf { grotto ->
            cluster.any { it.first == grotto.first }
        }

        grottos.add(Triple(chunkX to chunkZ, merged, cluster.sumOf { it.third }))

        if (grottoSettings.showNotification.value && numGrottos != grottos.size) {
            ChatUtils.showTitle("${Structure.FAIRY_GROTTO.displayName} Found")
        }
        if (grottoSettings.sendCoordsInChat.value && numGrottos != grottos.size) {
            ChatUtils.modMessage("Fairy Grotto found at x: ${merged.x}, y: ${merged.y}, z: ${merged.z}")
        }
    }

    private fun notifyFound(structure: Structure, pos: Triple<Int, Int, Int>) {
        if (structure.config.showNotification.value) {
            ChatUtils.showTitle("${structure.displayName} Found")
        }
        if (structure.config.sendCoordsInChat.value) {
            ChatUtils.modMessage("${structure.displayName} found at x: ${pos.first}, y: ${pos.second}, z: ${pos.third}")
        }
    }
}
