package com.github.gameringop.features.impl.visual

import com.github.gameringop.event.impl.ActionBarMessageEvent
import com.github.gameringop.features.Feature
import com.github.gameringop.ui.clickgui.components.*
import com.github.gameringop.ui.clickgui.components.impl.MultiCheckboxSetting
import com.github.gameringop.ui.clickgui.components.impl.ToggleSetting
import com.github.gameringop.ui.hud.getValue
import com.github.gameringop.ui.hud.provideDelegate
import com.github.gameringop.utils.ActionBarParser
import com.github.gameringop.utils.NumbersUtils
import com.github.gameringop.utils.location.LocationUtils
import com.github.gameringop.utils.render.Render2D
import com.github.gameringop.utils.render.Render2D.height
import com.github.gameringop.utils.render.Render2D.width

object PlayerHud: Feature(name = "Player HUD", description = "Displays your stats as moveable HUD elements.") {
    private val elements by MultiCheckboxSetting("Elements", mutableMapOf(
        "Health" to false, "Defense" to false, "Mana" to false,
        "Overflow Mana" to false, "Effective HP" to false, "Speed" to false
    ))

    private val hideFromActionbar by MultiCheckboxSetting("Hide from Actionbar", mutableMapOf(
        "Health" to false, "Defense" to false, "Mana" to false, "Overflow Mana" to false,
        "Dungeon Room Secrets" to false
    ))

    val hideFoodbar by ToggleSetting("Hide Food bar").withDescription("Hides the food bar.").section("Extras")
    val hideHealthbar by ToggleSetting("Hide Health bar").withDescription("Hides the health bar.")
    val hideArmorbar by ToggleSetting("Hide Armor bar").withDescription("Hides the defense bar.").hideIf { hideHealthbar.value }

    val healthDisplay by hudElement(
        this.name + " Health",
        { elements.value["Health"] == true },
        { LocationUtils.inSkyblock }
    ) { context, example ->
        val text = if (example) "§e3452§f/§c2452" else getHpFormatted()
        Render2D.drawString(context, text, 0, 0)
        return@hudElement text.width().toFloat() to text.height().toFloat()
    }

    val defenseDisplay by hudElement(
        this.name + " Defense",
        { elements.value["Defense"] == true },
        { LocationUtils.inSkyblock }
    ) { context, example ->
        val text = if (example) "§a5001" else "§a${ActionBarParser.currentDefense}"
        Render2D.drawString(context, text, 0, 0)
        return@hudElement text.width().toFloat() to text.height().toFloat()
    }

    val manaDisplay by hudElement(
        this.name + " Mana",
        { elements.value["Mana"] == true },
        { LocationUtils.inSkyblock }
    ) { context, example ->
        val text = if (example) "§b2452/2452" else "§b${ActionBarParser.currentMana}/${ActionBarParser.maxMana}"
        Render2D.drawString(context, text, 0, 0)
        return@hudElement text.width().toFloat() to text.height().toFloat()
    }

    val overflowDisplay by hudElement(
        this.name + " Overflow Mana",
        { elements.value["Overflow Mana"] == true },
        { LocationUtils.inSkyblock && ActionBarParser.overflowMana > 0 }
    ) { context, example ->
        val text = if (example) "§3600ʬ" else "§3${ActionBarParser.overflowMana}ʬ"
        Render2D.drawString(context, text, 0, 0)
        return@hudElement text.width().toFloat() to text.height().toFloat()
    }

    val ehpDisplay by hudElement(
        this.name + " Effective HP",
        { elements.value["Effective HP"] == true },
        { LocationUtils.inSkyblock }
    ) { context, example ->
        val text = if (example) "§27.3m" else "§2${NumbersUtils.format(ActionBarParser.effectiveHP)}"
        Render2D.drawString(context, text, 0, 0)
        return@hudElement text.width().toFloat() to text.height().toFloat()
    }

    val speedDisplay by hudElement(
        this.name + " Speed",
        { elements.value["Speed"] == true },
        { LocationUtils.inSkyblock }
    ) { context, example ->
        val text = if (example) "§f400✦" else "§f${ActionBarParser.currentSpeed}✦"
        Render2D.drawString(context, text, 0, 0)
        return@hudElement text.width().toFloat() to text.height().toFloat()
    }

    private fun getHpFormatted(): String {
        val color = if (ActionBarParser.currentHealth > ActionBarParser.maxHealth) "§e" else "§c"
        return "$color${ActionBarParser.currentHealth}§f/§c${ActionBarParser.maxHealth}"
    }

    override fun init() {
        register<ActionBarMessageEvent> {
            if (! LocationUtils.inSkyblock) return@register
            var result = event.formattedText

            if (hideFromActionbar.value["Health"] == true) result = result.replace(ActionBarParser.HP_REGEX, "")
            if (hideFromActionbar.value["Defense"] == true) result = result.replace(ActionBarParser.DEF_REGEX, "")
            if (hideFromActionbar.value["Mana"] == true) result = result.replace(ActionBarParser.MANA_REGEX, "")
            if (hideFromActionbar.value["Overflow Mana"] == true) result = result.replace(ActionBarParser.OVERFLOW_REGEX, "")
            if (hideFromActionbar.value["Dungeon Room Secrets"] == true) result = result.replace(ActionBarParser.SECRETS_REGEX, "")

            event.message = result
        }
    }
}