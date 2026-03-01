package com.github.gameringop.features.impl.general

import com.github.gameringop.event.impl.ContainerEvent
import com.github.gameringop.features.Feature
import com.github.gameringop.utils.ChatUtils.unformattedText
import com.github.gameringop.utils.location.LocationUtils
import com.github.gameringop.utils.render.Render2D
import net.minecraft.world.item.Items

object CakeNumbers: Feature("Displays the year of the cake in the New Year Cake Bag.") {
    private val cakeRegex = Regex("New Year Cake \\(Year (\\d+)\\)")

    override fun init() {
        register<ContainerEvent.Render.Slot.Post> {
            if (! LocationUtils.inSkyblock) return@register
            if (! event.slot.item.`is`(Items.CAKE)) return@register
            val year = cakeRegex.find(event.slot.item.hoverName.unformattedText)?.destructured?.component1() ?: return@register
            Render2D.drawCenteredString(event.context, "&b$year", event.slot.x + 8, event.slot.y + 8, scale = 0.8)
        }
    }
}