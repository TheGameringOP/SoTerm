package com.github.gameringop.features.impl.dungeon

import com.github.gameringop.event.impl.CheckEntityRenderEvent
import com.github.gameringop.features.Feature
import com.github.gameringop.ui.clickgui.components.getValue
import com.github.gameringop.ui.clickgui.components.impl.ToggleSetting
import com.github.gameringop.ui.clickgui.components.provideDelegate
import com.github.gameringop.utils.DataDownloader
import com.github.gameringop.utils.location.LocationUtils
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.monster.EnderMan
import net.minecraft.world.entity.monster.Giant

object HiddenMobs: Feature("Reveals invisible mobs in dungeons.") {
    private val watcherMobs by lazy { DataDownloader.loadJson<List<String>>("watcherMobsNames.json") }

    private val showFels by ToggleSetting("Show Fels")
    private val showSa by ToggleSetting("Show Shadow Assassins")
    private val showStealthy by ToggleSetting("Show Stealthy")

    override fun init() {
        register<CheckEntityRenderEvent> {
            if (! showFels.value && ! showSa.value && ! showStealthy.value) return@register
            if (! LocationUtils.inDungeon) return@register
            if (! event.entity.isInvisible) return@register

            val name = event.entity.name.string.trim()
            val isFel = event.entity is EnderMan && showFels.value && name == "Dinnerbone"
            val isSA = event.entity is AbstractClientPlayer && showSa.value && name.contains("Shadow Assassin")
            val isWatcherMob = event.entity is AbstractClientPlayer && showStealthy.value && watcherMobs.any { name == it }
            val isGiant = event.entity is Giant && showStealthy.value && ! event.entity.getItemBySlot(EquipmentSlot.FEET).isEmpty

            if (isFel || isSA || isWatcherMob || isGiant) event.entity.isInvisible = false
        }
    }
}