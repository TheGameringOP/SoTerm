package com.github.gameringop.features.impl.floor7

import com.github.gameringop.event.impl.TickEvent
import com.github.gameringop.features.Feature
import com.github.gameringop.ui.clickgui.components.impl.DropdownSetting
import com.github.gameringop.utils.DataDownloader
import com.github.gameringop.utils.WorldUtils
import com.github.gameringop.utils.location.LocationUtils
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks

object WDiorite: Feature("I Love Diorite") {
    private val options = listOf(
        "Dynamic", "Red", "Green", "Yellow", "Purple", "White", "Transparent"
    )

    private val optionStates = mapOf(
        0 to null,
        1 to Blocks.RED_STAINED_GLASS.defaultBlockState(),
        2 to Blocks.GREEN_STAINED_GLASS.defaultBlockState(),
        3 to Blocks.YELLOW_STAINED_GLASS.defaultBlockState(),
        4 to Blocks.PURPLE_STAINED_GLASS.defaultBlockState(),
        5 to Blocks.WHITE_STAINED_GLASS.defaultBlockState(),
        6 to Blocks.GLASS.defaultBlockState()
    )

    private val pillarStates = mapOf(
        "GreenArray" to Blocks.GREEN_STAINED_GLASS.defaultBlockState(),
        "YellowArray" to Blocks.YELLOW_STAINED_GLASS.defaultBlockState(),
        "PurpleArray" to Blocks.PURPLE_STAINED_GLASS.defaultBlockState(),
        "RedArray" to Blocks.RED_STAINED_GLASS.defaultBlockState()
    )

    private val glassColor by DropdownSetting("Glass Color", 0, options)

    private val positions by lazy {
        val data = DataDownloader.loadJson<Map<String, List<Map<String, Double>>>>("iLoveDioriteBlocks.json")
        buildList {
            data.forEach { (pillar, coords) ->
                val dynamicState = pillarStates[pillar] ?: return@forEach
                coords.forEach { coord ->
                    val pos = BlockPos(coord["x"]!!.toInt(), coord["y"]!!.toInt(), coord["z"]!!.toInt())
                    add(pos to dynamicState)
                }
            }
        }
    }

    private val cursor = BlockPos.MutableBlockPos()

    override fun init() {
        register<TickEvent.Start> {
            if (LocationUtils.F7Phase != 2) return@register
            val level = mc.level ?: return@register

            val selectedIndex = glassColor.value
            val fixedState = optionStates[selectedIndex]

            for ((basePos, dynamicState) in positions) {
                val targetState = fixedState ?: dynamicState
                for (yOffset in 0..37) {
                    cursor.set(basePos.x, basePos.y + yOffset, basePos.z)
                    val currentState = level.getBlockState(cursor)
                    if ((currentState.`is`(Blocks.DIORITE) || currentState.`is`(Blocks.POLISHED_DIORITE)) && currentState != targetState) {
                        WorldUtils.setBlockAt(cursor, targetState)
                    }
                }
            }
        }
    }
}