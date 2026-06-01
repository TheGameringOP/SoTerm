package com.github.gameringop.event.impl

import com.github.gameringop.utils.StringUtils.stripped
import net.minecraft.client.Minecraft
import net.minecraft.world.entity.Entity

data class SlayerInfo(val entity: Entity) {
    private val mc = Minecraft.getInstance()
    private val level = mc.level

    val owner: String? by lazy {
        level?.getEntity(entity.id + 3)?.customName?.string?.stripped()?.substringAfterLast(":")?.trim()
    }

    val typeAndTierName: String? by lazy {
        level?.getEntity(entity.id + 1)?.customName?.string?.stripped()
    }

    val owned: Boolean
        get() = owner == mc.player?.name?.string
}