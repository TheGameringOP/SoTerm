package com.github.gameringop.features.impl.visual

import com.github.gameringop.event.impl.MainThreadPacketReceivedEvent
import com.github.gameringop.event.impl.TickEvent
import com.github.gameringop.features.Feature
import com.github.gameringop.ui.clickgui.components.impl.ToggleSetting
import com.github.gameringop.utils.location.LocationUtils
import com.github.gameringop.utils.location.WorldType
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.monster.Creeper
import java.util.concurrent.ConcurrentHashMap

object Ghosts: Feature(name = "Ghosts", description = "Makes ghost creepers visible in the Dwarven Mines.") {
    private val showGhosts by ToggleSetting("Show Ghosts", true)
    private val GHOST_MIN_HEALTH = 1_000_000.0

    private val trackedGhosts = ConcurrentHashMap.newKeySet<Int>()

    override fun init() {
        register<MainThreadPacketReceivedEvent.Post> {
            if (!showGhosts.value) return@register
            if (LocationUtils.world != WorldType.DwarvenMines) return@register

            val packet = event.packet as? ClientboundAddEntityPacket ?: return@register
            val entity = mc.level?.getEntity(packet.id) as? Creeper ?: return@register
            val maxHealth = entity.getAttributeBaseValue(Attributes.MAX_HEALTH)
            if (maxHealth >= GHOST_MIN_HEALTH) {
                trackedGhosts.add(entity.id)
                entity.isInvisible = false
            }
        }

        register<MainThreadPacketReceivedEvent.Post> {
            if (!showGhosts.value) return@register
            if (LocationUtils.world != WorldType.DwarvenMines) return@register

            val packet = event.packet as? ClientboundSetEntityDataPacket ?: return@register
            val entity = mc.level?.getEntity(packet.id) as? Creeper ?: return@register
            if (!entity.isInvisible) return@register
            val maxHealth = entity.getAttributeBaseValue(Attributes.MAX_HEALTH)
            if (maxHealth >= GHOST_MIN_HEALTH) {
                trackedGhosts.add(entity.id)
                entity.isInvisible = false
            }
        }

        register<TickEvent.Start> {
            if (!showGhosts.value) return@register
            if (LocationUtils.world != WorldType.DwarvenMines) return@register
            val level = mc.level ?: return@register

            for (entity in level.entitiesForRendering()) {
                if (entity is Creeper && entity.isInvisible && !trackedGhosts.contains(entity.id)) {
                    val maxHealth = entity.getAttributeBaseValue(Attributes.MAX_HEALTH)
                    if (maxHealth >= GHOST_MIN_HEALTH) {
                        trackedGhosts.add(entity.id)
                        entity.isInvisible = false
                    }
                }
            }
        }
    }
}