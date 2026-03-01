package com.github.gameringop.features.impl.visual

import com.github.gameringop.event.impl.WorldChangeEvent
import com.github.gameringop.features.Feature
import com.github.gameringop.ui.hud.getValue
import com.github.gameringop.ui.hud.provideDelegate
import com.github.gameringop.utils.render.Render2D
import com.github.gameringop.utils.render.Render2D.width

object CpsDisplay: Feature("Displays your left and right clicks per second.") {
    private val leftClicks = mutableListOf<Long>()
    private val rightClicks = mutableListOf<Long>()

    val combinedCps by hudElement("CPS Display") { ctx, _ ->
        val l = getCps(leftClicks)
        val r = getCps(rightClicks)
        val text = "§f$l §7| §f$r §bCPS"
        Render2D.drawString(ctx, text, 2f, 2f)
        return@hudElement text.width().toFloat() + 4f to 12f
    }


    private fun getCps(list: MutableList<Long>): Int {
        val now = System.currentTimeMillis()
        list.removeIf { now - it > 1000 }
        return list.size
    }

    @JvmStatic
    fun addLeftClick() {
        if (! enabled) return
        leftClicks.add(System.currentTimeMillis())
    }

    @JvmStatic
    fun addRightClick() {
        if (! enabled) return
        rightClicks.add(System.currentTimeMillis())
    }

    override fun init() {
        register<WorldChangeEvent> {
            leftClicks.clear()
            rightClicks.clear()
        }
    }
}