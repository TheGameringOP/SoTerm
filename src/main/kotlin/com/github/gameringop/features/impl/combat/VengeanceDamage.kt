//package com.github.gameringop.features.impl.combat
//
//import com.github.gameringop.event.impl.RenderWorldEvent
//import com.github.gameringop.event.impl.SlayerEvent
//import com.github.gameringop.features.Feature
//import com.github.gameringop.ui.clickgui.components.getValue
//import com.github.gameringop.ui.clickgui.components.impl.ToggleSetting
//import com.github.gameringop.ui.clickgui.components.provideDelegate
//import com.github.gameringop.utils.ChatUtils
//import com.github.gameringop.utils.StringUtils.stripped
//import com.github.gameringop.utils.render.Render2D
//import com.github.gameringop.utils.render.Render2D.width
//import net.minecraft.world.entity.decoration.ArmorStand
//
//object VengeanceDamage: Feature("Tracks your vengeance damage.") {
//    private val showHud by ToggleSetting("Show HUD", true)
//    private val showInTitle by ToggleSetting("Show in Title", true)
//    private val showInChat by ToggleSetting("Show in Chat", false)
//    private val abbreviate by ToggleSetting("Abbreviate damage", true)
//
//    private val vengeanceRegex = Regex("""(\d+(?:,\d+)*)ﬗ""")
//    private val processedIds = mutableSetOf<Int>()
//    private var lastDamage = "0"
//
//    override fun init() {
//        hudElement(
//            name = "Vengeance Damage HUD",
//            enabled = { showHud.value },
//            shouldDraw = { lastDamage != "0" },
//            centered = true
//        ) { ctx, example ->
//            val displayValue = if (example) "1.2M" else lastDamage
//            val text = "§cVengeance Damage: §f$displayValue"
//
//            Render2D.drawCenteredString(ctx, text, 0, 0)
//            return@hudElement text.width().toFloat() to 9f
//        }
//
//        register<SlayerEvent.Boss.Spawn> {
//            lastDamage = "0"
//            processedIds.clear()
//        }
//
//        register<SlayerEvent.Boss.Death> {
//            lastDamage = "0"
//            processedIds.clear()
//        }
//
//        register<SlayerEvent.Reset.Any> {
//            lastDamage = "0"
//            processedIds.clear()
//        }
//
//        register<RenderWorldEvent> {
//            val world = mc.level ?: return@register
//            val player = mc.player ?: return@register
//
//            val armorStands = world.getEntities(player, player.boundingBox.inflate(10.0)) {
//                it is ArmorStand && !processedIds.contains(it.id)
//            }
//
//            armorStands.forEach { entity ->
//                val name = (entity as ArmorStand).customName?.string?.stripped() ?: return@forEach
//
//                if (name.contains("ﬗ")) {
//                    val match = vengeanceRegex.find(name) ?: return@forEach
//                    val damageRaw = match.groupValues[1]
//                    val damageLong = damageRaw.replace(",", "").toLong()
//
//                    if (damageLong < 500_000) return@forEach
//
//                    val display = if (abbreviate.value) formatDamage(damageLong) else damageRaw
//                    lastDamage = display
//
//                    if (showInTitle.value) ChatUtils.showTitle("§cVengeance Damage: §f$display")
//                    if (showInChat.value) ChatUtils.modMessage("§cVengeance Damage: §f$display")
//
//                    processedIds.add(entity.id)
//                }
//            }
//
//            if (processedIds.size > 50) processedIds.clear()
//        }
//    }
//
//    private fun formatDamage(damage: Long): String {
//        return when {
//            damage >= 1_000_000 -> "%.1fM".format(damage / 1_000_000.0)
//            damage >= 1_000 -> "%.1fK".format(damage / 1_000.0)
//            else -> damage.toString()
//        }
//    }
//}