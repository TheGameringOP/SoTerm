package com.github.gameringop.features.impl.general

import com.github.gameringop.event.EventPriority
import com.github.gameringop.event.impl.PlayerInteractEvent
import com.github.gameringop.features.Feature
import net.minecraft.world.item.Items

object CancelInteract: Feature("Disables Hypixel's stupid Ender Pearl throw block when you are looking at a wall/floor/ceiling.") {
    override fun init() {
        register<PlayerInteractEvent.RIGHT_CLICK.BLOCK>(EventPriority.LOWEST) {
            event.item?.takeIf { it.`is`(Items.ENDER_PEARL) }?.let { mc.hitResult = null }
        }
    }
}
