package com.github.gameringop.utils

import com.github.gameringop.event.EventBus
import com.github.gameringop.event.impl.*
import com.github.gameringop.utils.StringUtils.stripped
import net.minecraft.client.Minecraft
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.decoration.ArmorStand

object SlayerUtils {
    var currentBoss: Entity? = null

    fun init() {
        EventBus.register<WorldChangeEvent> {
            currentBoss = null
            EventBus.post(SlayerEvent.Reset.Any)
        }

        EventBus.register<TickEvent.End> {
            val mc = Minecraft.getInstance()
            val world = mc.level ?: return@register
            val player = mc.player ?: return@register

            if (currentBoss == null) {
                val nearby = world.getEntities(player, player.boundingBox.inflate(20.0)) {
                    it is ArmorStand && it.customName?.string?.stripped()?.let { name ->
                        name.contains("❤") && (name.contains("Lv") || name.contains("Boss"))
                    } ?: false
                }

                val boss = nearby.firstOrNull()
                if (boss != null) {
                    val info = SlayerInfo(boss)
                    if (info.owned) {
                        currentBoss = boss
                        EventBus.post(SlayerEvent.Boss.Spawn(boss, info))
                    }
                }
            }
        }

        EventBus.register<ChatMessageEvent> {
            if (event.unformattedText.contains("SLAYER BOSS SLAIN!")) {
                currentBoss?.let { boss ->
                    EventBus.post(SlayerEvent.Boss.Death(boss, SlayerInfo(boss)))
                }
                currentBoss = null
            }
        }
    }
}