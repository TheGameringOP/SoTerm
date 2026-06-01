package com.github.gameringop.features.impl.general

import com.github.gameringop.event.impl.ChatMessageEvent
import com.github.gameringop.event.impl.RenderWorldEvent
import com.github.gameringop.event.impl.WorldChangeEvent
import com.github.gameringop.features.Feature
import com.github.gameringop.init.NetworkLoop
import com.github.gameringop.ui.clickgui.components.impl.ColorSetting
import com.github.gameringop.ui.clickgui.components.impl.SliderSetting
import com.github.gameringop.ui.clickgui.components.impl.TextInputSetting
import com.github.gameringop.ui.clickgui.components.impl.ToggleSetting
import com.github.gameringop.utils.ChatUtils
import com.github.gameringop.utils.NumbersUtils.toFixed
import com.github.gameringop.utils.ThreadUtils
import com.github.gameringop.utils.location.LocationUtils
import com.github.gameringop.utils.location.WorldType
import com.github.gameringop.utils.render.Render2D
import com.github.gameringop.utils.render.Render2D.width
import com.github.gameringop.utils.render.Render3D
import com.github.gameringop.utils.render.RenderHelper.renderBoundingBox
import com.github.gameringop.utils.render.RenderHelper.renderVec
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.animal.Chicken
import net.minecraft.world.entity.animal.Cow
import net.minecraft.world.entity.animal.Pig
import net.minecraft.world.entity.animal.Rabbit
import net.minecraft.world.entity.animal.horse.Horse
import net.minecraft.world.entity.animal.sheep.Sheep
import java.awt.Color

object TrevorHelper: Feature(name = "Trevor Helper", description = "Helper features for Trevor the Trapper on the Farming Islands.") {
    private val mobEsp by ToggleSetting("Animal ESP", true).section("ESP")
    private val espTracer by ToggleSetting("Show tracer", false).showIf { mobEsp.value }

    private val autoCall by ToggleSetting("Auto call", false).section("Automation")
    private val callDelay by SliderSetting("Call delay", 1, 0, 5, 1, " ticks").showIf { autoCall.value }
    private val callOff by SliderSetting("Call early", 2.0f, 0.0f, 5.0f, 0.1f, " s").showIf { autoCall.value }

    private val autoAccept by ToggleSetting("Auto accept", false)
    private val acceptDelay by SliderSetting("Accept delay", 1, 0, 5, 1, " ticks").showIf { autoAccept.value }

    private val endAlert by ToggleSetting("Cooldown end alert", true).section("Alerts")
    private val alertMessage by TextInputSetting("Alert message", "&cCooldown ended!").showIf { endAlert.value }
    private val alertSound = createSoundSettings("Alert sound", SoundEvents.NOTE_BLOCK_PLING.value(), showIf = { endAlert.value })

    private val colorTrackable by ColorSetting("Trackable color", Color(205, 214, 244), false).section("Colors")
    private val colorUntrackable by ColorSetting("Untrackable color", Color(166, 227, 161), false)
    private val colorUndetected by ColorSetting("Undetected color", Color(137, 180, 250), false)
    private val colorEndangered by ColorSetting("Endangered color", Color(203, 166, 247), false)
    private val colorElusive by ColorSetting("Elusive color", Color(249, 226, 175), false)

    private val startRegex = Regex("\\[NPC] Trevor: You can find your (?<type>\\w+) animal near the .*")

    private var cooldown = 0L
    private var rarity: Rarity? = null

    override fun init() {
        hudElement(name = "Cooldown timer", shouldDraw = { onBarn() },) { ctx, example ->
            if (example) {
                Render2D.drawString(ctx, "Cooldown: §c12.4s", 0, 0)
                return@hudElement "Cooldown: §c12.4s".width().toFloat() to 9f
            }

            if (cooldown <= 0L) return@hudElement 0f to 0f

            val remaining = (cooldown - System.currentTimeMillis()).coerceAtLeast(0)
            val text = "Cooldown: §c${(remaining / 1000.0).toFixed(1)}s"
            Render2D.drawString(ctx, text, 0, 0)
            return@hudElement text.width().toFloat() to 9f
        }

        register<WorldChangeEvent> { reset() }

        register<RenderWorldEvent> {
            if (! mobEsp.value || ! onBarn()) return@register

            val rarity = rarity ?: return@register
            val level = mc.level ?: return@register

            for (entity in level.entitiesForRendering()) {
                val living = entity as? LivingEntity ?: continue
                if (! living.isTrevorAnimal()) continue

                val maxHp = living.serverMaxHealth
                if (maxHp != rarity.hp) continue

                val box = living.renderBoundingBox
                val centerX = (box.minX + box.maxX) / 2.0
                val centerZ = (box.minZ + box.maxZ) / 2.0

                Render3D.renderBox(
                    ctx = event.ctx,
                    x = centerX,
                    y = box.minY,
                    z = centerZ,
                    width = box.xsize,
                    height = box.ysize,
                    color = rarity.color,
                    outline = true,
                    fill = false,
                    phase = false,
                )

                if (espTracer.value) {
                    Render3D.renderTracer(event.ctx, living.renderVec.add(0.0, living.bbHeight / 2.0, 0.0), rarity.color)
                }
            }
        }

        register<ChatMessageEvent> {
            if (! onBarn()) return@register

            val stripped = event.unformattedText

            startRegex.find(stripped)?.let { match ->
                val type = match.groups["type"]?.value ?: return@register
                rarity = Rarity.from(type) ?: return@register
                cooldown = System.currentTimeMillis() + 15_000

                ThreadUtils.setTimeout(15_000) {
                    if (endAlert.value) {
                        ChatUtils.showTitle(alertMessage.value)
                        mc.soundManager.play(
                            SimpleSoundInstance.forUI(
                                alertSound.sound.value,
                                alertSound.pitch.value,
                                alertSound.volume.value,
                            )
                        )
                    }
                    cooldown = 0L
                }
                return@register
            }

            if (stripped == "Return to the Trapper soon to get a new animal to hunt!") {
                if (! autoCall.value) {
                    reset()
                    return@register
                }

                val ms = (cooldown - System.currentTimeMillis() - (callOff.value * 1000).toLong()).coerceAtLeast(0)
                val extra = (callDelay.value.toInt() + (0..2).random()) * 50L

                ThreadUtils.setTimeout(ms + extra) { ChatUtils.sendCommand("call trevor") }
                reset()
                return@register
            }

            if (! autoAccept.value) return@register
            if (! stripped.startsWith("Accept the trapper's task to hunt the animal?")) return@register

            val command = event.component.findRunCommand() ?: return@register
            ThreadUtils.scheduledTask(acceptDelay.value.toInt() + (0..2).random()) {
                ChatUtils.sendCommand(command.removePrefix("/"))
            }
        }
    }

    private fun onBarn() = LocationUtils.inSkyblock && LocationUtils.world == WorldType.TheBarn

    private fun reset() {
        rarity = null
        cooldown = 0L
    }

    private val LivingEntity.serverMaxHealth: Float
        get() {
            val max = getAttributeBaseValue(Attributes.MAX_HEALTH).toFloat()
            return if (this is Horse) max / 2f else max
        }

    private fun LivingEntity.isTrevorAnimal(): Boolean = when (this) {
        is Cow, is Pig, is Sheep, is Chicken, is Rabbit, is Horse -> true
        else -> false
    }

    private fun Component.findRunCommand(): String? {
        when (val click = style.clickEvent) {
            is ClickEvent.RunCommand -> return click.command()
            else -> {}
        }

        for (sibling in siblings) {
            sibling.findRunCommand()?.let { return it }
        }

        return null
    }

    private enum class Rarity(val normal: Float, val derpy: Float) {
        Trackable(100f, 200f),
        Untrackable(500f, 1000f),
        Undetected(1000f, 2000f),
        Endangered(5000f, 10000f),
        Elusive(10000f, 20000f);

        val hp: Float
            get() = if (isDerpyActive) derpy else normal

        val color: Color
            get() = when (this) {
                Trackable -> colorTrackable.value
                Untrackable -> colorUntrackable.value
                Undetected -> colorUndetected.value
                Endangered -> colorEndangered.value
                Elusive -> colorElusive.value
            }

        companion object {
            private val isDerpyActive get() = NetworkLoop.electionData.mayor.name.equals("Derpy", ignoreCase = true)

            fun from(type: String): Rarity? = entries.find { it.name.equals(type, ignoreCase = true) }
        }
    }
}
