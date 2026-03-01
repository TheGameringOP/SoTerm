package com.github.gameringop.features.impl.dungeon.waypoints

import com.github.gameringop.SoTerm
import com.github.gameringop.SoTerm.MOD_NAME
import com.github.gameringop.event.impl.DungeonEvent
import com.github.gameringop.event.impl.RenderWorldEvent
import com.github.gameringop.event.impl.WorldChangeEvent
import com.github.gameringop.features.Feature
import com.github.gameringop.ui.clickgui.components.getValue
import com.github.gameringop.ui.clickgui.components.impl.ToggleSetting
import com.github.gameringop.ui.clickgui.components.provideDelegate
import com.github.gameringop.utils.ChatUtils
import com.github.gameringop.utils.JsonUtils
import com.github.gameringop.utils.dungeons.map.utils.ScanUtils
import com.github.gameringop.utils.location.LocationUtils
import com.github.gameringop.utils.render.Render3D
import com.google.gson.reflect.TypeToken
import net.minecraft.core.BlockPos
import java.awt.Color
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.util.concurrent.CopyOnWriteArrayList

object DungeonWaypoints: Feature("Add a custom waypoint with /ndw add while looking at a block") {
    val secretWaypoints by ToggleSetting("Secret Waypoints")

    data class DungeonWaypoint(val pos: BlockPos, val color: Color, val filled: Boolean, val outline: Boolean, val phase: Boolean)

    private val configFile = File("config/$MOD_NAME/dungeonWaypoints.json")
    val waypoints: MutableMap<String, List<DungeonWaypoint>> = mutableMapOf()
    val currentRoomWaypoints = CopyOnWriteArrayList<DungeonWaypoint>()

    override fun init() {
        loadConfig()

        register<DungeonEvent.RoomEvent.onEnter> {
            SecretsWaypoints.onRoomEnter(event.room)
            currentRoomWaypoints.clear()

            val roomName = event.room.name
            val roomRotation = event.room.rotation ?: return@register
            val roomCorner = event.room.corner ?: return@register

            waypoints[roomName]?.map { wp ->
                wp.copy(pos = ScanUtils.getRealCoord(wp.pos, roomCorner, 360 - roomRotation))
            }?.let { currentRoomWaypoints.addAll(it) }
        }

        register<DungeonEvent.SecretEvent> {
            SecretsWaypoints.onSecret(event)
        }

        register<RenderWorldEvent> {
            SecretsWaypoints.onRenderWorld(event.ctx)
            val waypoints = if (LocationUtils.inBoss) {
                currentRoomWaypoints.clear()
                waypoints["B${LocationUtils.dungeonFloorNumber}"].orEmpty()
            }
            else currentRoomWaypoints

            if (waypoints.isEmpty()) return@register
            for (wp in waypoints) {
                Render3D.renderBlock(
                    event.ctx, wp.pos, wp.color,
                    outline = wp.outline, fill = wp.filled, phase = wp.phase
                )
            }
        }

        register<WorldChangeEvent> {
            SecretsWaypoints.clear()
            currentRoomWaypoints.clear()
        }
    }

    private fun loadConfig() {
        if (! configFile.exists()) return

        runCatching {
            FileReader(configFile).use { reader ->
                val type = object: TypeToken<MutableMap<String, List<DungeonWaypoint>>>() {}.type
                val loadedData = JsonUtils.gsonBuilder.fromJson<MutableMap<String, List<DungeonWaypoint>>>(reader, type)

                if (loadedData != null) {
                    waypoints.clear()
                    waypoints.putAll(loadedData)
                    SoTerm.logger.info("${this.javaClass.simpleName} Config loaded: ${waypoints.size} rooms.")
                }
            }
        }.onFailure {
            SoTerm.logger.error("${this.javaClass.simpleName} Failed to load config!", it)
        }
    }

    fun saveConfig() {
        runCatching {
            configFile.parentFile?.mkdirs()
            FileWriter(configFile).use { writer ->
                JsonUtils.gsonBuilder.toJson(waypoints, writer)
            }
            SoTerm.logger.info("${this.javaClass.simpleName} Config saved.")
        }.onFailure {
            SoTerm.logger.error("${this.javaClass.simpleName} Failed to save config!", it)
        }
    }

    fun saveWaypoint(absPos: BlockPos, relPos: BlockPos, roomName: String, color: Color, filled: Boolean, outline: Boolean, phase: Boolean) {
        val newWaypoint = DungeonWaypoint(relPos, color, filled, outline, phase)
        val absWaypoint = newWaypoint.copy(pos = absPos)

        waypoints.compute(roomName) { _, list ->
            val mutableList = list?.toMutableList() ?: mutableListOf()
            val replaced = mutableList.removeIf { it.pos == relPos }
            mutableList.add(newWaypoint)

            if (replaced) ChatUtils.modMessage("§e$roomName: Waypoint updated at ${absPos.toShortString()}.")
            else ChatUtils.modMessage("§a$roomName: Waypoint added at ${absPos.toShortString()}.")

            mutableList
        }

        saveConfig()

        currentRoomWaypoints.removeIf { it.pos == absPos }
        currentRoomWaypoints.add(absWaypoint)
    }
}