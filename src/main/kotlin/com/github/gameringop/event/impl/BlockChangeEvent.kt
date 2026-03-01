package com.github.gameringop.event.impl

import com.github.gameringop.event.Event
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.state.BlockState

class BlockChangeEvent(val pos: BlockPos, val newState: BlockState, val oldState: BlockState): Event() {
    val newBlock = newState.block
    val oldBlock = oldState.block
}