package com.github.gameringop.event.impl

import com.github.gameringop.event.Event
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack

class ContainerFullyOpenedEvent(
    val title: Component,
    val winId: Int,
    val slotCount: Int,
    val items: HashMap<Int, ItemStack>
): Event(false)