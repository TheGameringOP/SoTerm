package com.github.gameringop.features.impl.visual

import com.github.gameringop.event.impl.MainThreadPacketReceivedEvent
import com.github.gameringop.features.Feature
import com.github.gameringop.ui.clickgui.components.getValue
import com.github.gameringop.ui.clickgui.components.impl.ToggleSetting
import com.github.gameringop.ui.clickgui.components.provideDelegate
import com.github.gameringop.ui.clickgui.components.withDescription
import com.github.gameringop.utils.ChatUtils.formattedText
import com.github.gameringop.utils.ChatUtils.removeFormatting
import com.github.gameringop.utils.NumbersUtils
import com.github.gameringop.utils.Utils.containsOneOf
import com.github.gameringop.utils.location.LocationUtils
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.decoration.ArmorStand
import java.util.*
import kotlin.jvm.optionals.getOrNull

object DamageSplash: Feature("Reformat Skyblock's Damage Indicators.") {
    private val uppercase by ToggleSetting("Uppercase Formatting").withDescription("Changes the damage number to uppercase.")
    private val disableinClear by ToggleSetting("Hide in Clear").withDescription("Hides all damage indicators in dungeon clear.")
    private val disableinBoss by ToggleSetting("Hide in Boss").withDescription("Hides all damage indicators in dungeon boss room.")

    private val damageRegex = Regex("[✧✯]?(\\d{1,3}(?:,\\d{3})*[⚔+✧❤♞☄✷ﬗ✯]*)")

    @Suppress("UNCHECKED_CAST")
    override fun init() {
        register<MainThreadPacketReceivedEvent.Pre> {
            if (! LocationUtils.inSkyblock) return@register
            if (event.packet !is ClientboundSetEntityDataPacket) return@register
            val entity = mc.level?.getEntity(event.packet.id) as? ArmorStand ?: return@register
            for (entry in event.packet.packedItems) {
                val value = entry.value() as? Optional<Component> ?: continue
                val content = value.getOrNull() ?: continue
                val rawText = content.formattedText.takeIf { it.contains("§") } ?: continue
                val damageNum = damageRegex.matchEntire(rawText.removeFormatting())?.destructured?.component1() ?: continue

                if ((LocationUtils.inBoss && disableinBoss.value) || (LocationUtils.inDungeon && ! LocationUtils.inBoss && disableinClear.value)) {
                    entity.remove(Entity.RemovalReason.DISCARDED)
                    event.isCanceled = true
                    return@register
                }

                val formattedDamage = if (uppercase.value) NumbersUtils.format(damageNum).uppercase()
                else NumbersUtils.format(damageNum)

                val newNameString = if (rawText.containsOneOf("✧", "✯")) {
                    "§f✧${addRandomColorCodes(formattedDamage)}§f✧"
                }
                else "§3${formattedDamage}"

                val newComponent = Component.literal(newNameString)

                entity.entityData.assignValues(event.packet.packedItems)
                entity.customName = newComponent
                event.isCanceled = true
            }
        }
    }

    private fun addRandomColorCodes(inputString: String): String {
        val colorCodes = listOf("§6", "§c", "§e", "§f")
        val result = StringBuilder()
        var lastColor: String? = null

        for (char in inputString) {
            val availableColors = colorCodes.filter { it != lastColor }
            val randomColor = availableColors.random()
            result.append(randomColor).append(char).append("§r")
            lastColor = randomColor
        }

        return result.toString()
    }
}