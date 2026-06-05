package com.github.gameringop.features.impl.combat

import com.github.gameringop.event.impl.*
import com.github.gameringop.features.Feature
import com.github.gameringop.ui.clickgui.components.impl.MultiCheckboxSetting
import com.github.gameringop.ui.clickgui.components.impl.SliderSetting
import com.github.gameringop.ui.clickgui.components.impl.ToggleSetting
import com.github.gameringop.utils.ActionBarParser
import com.github.gameringop.utils.ChatUtils.removeFormatting
import com.github.gameringop.utils.PlayerUtils
import com.github.gameringop.utils.SlayerUtils
import com.github.gameringop.utils.StringUtils.stripped
import com.github.gameringop.utils.items.ItemUtils.lore
import com.github.gameringop.utils.items.ItemUtils.skyblockId
import com.github.gameringop.utils.location.LocationUtils
import com.github.gameringop.utils.location.WorldType
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.phys.EntityHitResult

object Soulcry: Feature(name = "Soulcry", description = "Automatically uses the Soulcry ability on Voidgloom slayer katanas.") {
    private val checkMana by ToggleSetting("Check mana", true)
    private val checkHitbox by ToggleSetting("Check boss hitbox", true)
    private val minDelay by SliderSetting("Min delay", 1, 0, 5, 1, " ticks")
    private val maxDelay by SliderSetting("Max delay", 3, 0, 5, 1, " ticks")
    private val detectType by MultiCheckboxSetting("Detection type", mutableMapOf("Tick based" to true, "Attack based" to false))
    private val otherBosses by ToggleSetting("Work on other's bosses", false).showIf { detectType.value["Attack based"] == true }

    private val katanaIds = setOf("VOIDEDGE_KATANA", "VORPAL_KATANA", "ATOMSPLIT_KATANA")
    private var tick = -1

    override fun init() {
        register<WorldChangeEvent> { reset() }
        register<SlayerEvent.Boss.Death> { reset() }
        register<SlayerEvent.Reset.Any> { reset() }

        register<TickEvent.Start> {
            if (detectType.value["Tick based"] != true) return@register
            if (! onEnd()) return@register reset()

            val bossStand = SlayerUtils.currentBoss ?: return@register reset()
            val info = SlayerInfo(bossStand)
            if (! isVoidgloomSlayer(info)) return@register reset()
            if (mc.screen != null) return@register reset()

            val item = mc.player?.mainHandItem ?: return@register reset()
            if (! isKatana(item)) return@register reset()
            if (checkHitbox.value && ! isLookingAtBoss(bossStand)) return@register reset()
            if (! hasEnoughMana(item)) return@register reset()

            if (tick == -1) {
                val max = maxDelay.value.coerceAtLeast(minDelay.value)
                tick = (minDelay.value..max).random()
                return@register
            }

            if (tick-- > 0) return@register

            PlayerUtils.rightClick()
            reset()
        }

        register<PlayerInteractEvent.LEFT_CLICK.ENTITY> {
            if (detectType.value["Attack based"] != true) return@register
            if (! onEnd()) return@register

            val info = findSlayerInfo(event.entity) ?: return@register
            if (! isVoidgloomSlayer(info)) return@register
            if (! info.owned && ! otherBosses.value) return@register

            val item = mc.player?.mainHandItem ?: return@register
            if (! isKatana(item)) return@register
            if (! hasEnoughMana(item)) return@register

            PlayerUtils.rightClick()
        }
    }

    private fun onEnd() = LocationUtils.inSkyblock && LocationUtils.world == WorldType.End

    private fun isKatana(item: ItemStack): Boolean {
        return item.`is`(Items.DIAMOND_SWORD) && item.skyblockId in katanaIds
    }

    private fun hasEnoughMana(item: ItemStack): Boolean {
        if (! checkMana.value) return true
        val required = if (item.hasUltimateWise()) 100 else 200
        return ActionBarParser.currentMana + ActionBarParser.overflowMana >= required
    }

    private fun isVoidgloomSlayer(info: SlayerInfo): Boolean {
        val type = info.typeAndTierName ?: return false
        return type.contains("Voidgloom", ignoreCase = true) || type.contains("Enderman", ignoreCase = true)
    }

    private fun isLookingAtBoss(bossStand: Entity): Boolean {
        val hit = mc.hitResult as? EntityHitResult ?: return false
        val mob = getSlayerBossMob(bossStand) ?: return false
        return hit.entity == mob
    }

    private fun getSlayerBossMob(bossStand: Entity): Entity? {
        val level = mc.level ?: return null
        return level.getEntities(bossStand, bossStand.boundingBox.inflate(4.0)) { entity ->
            entity !is ArmorStand && entity.isAlive && entity != mc.player
        }.minByOrNull { it.distanceToSqr(bossStand) }
    }

    private fun findSlayerInfo(entity: Entity): SlayerInfo? {
        if (entity is ArmorStand && isBossStand(entity)) return SlayerInfo(entity)

        val level = mc.level ?: return null
        val stand = level.getEntities(entity, entity.boundingBox.inflate(3.0)) { it is ArmorStand && isBossStand(it) }
            .minByOrNull { it.distanceToSqr(entity) }
            ?: return null

        return SlayerInfo(stand)
    }

    private fun isBossStand(entity: Entity): Boolean {
        val name = entity.customName?.string?.stripped() ?: return false
        return name.contains("❤") && (name.contains("Lv") || name.contains("Boss"))
    }

    private fun ItemStack.hasUltimateWise(): Boolean {
        return lore.any { it.removeFormatting().contains("ultimate wise", ignoreCase = true) }
    }

    private fun reset() {
        tick = -1
    }
}
