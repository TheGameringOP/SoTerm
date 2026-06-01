package com.github.gameringop.features.impl.visual.hollows

import com.github.gameringop.features.Feature
import com.github.gameringop.ui.clickgui.components.Setting.Companion.showIf
import com.github.gameringop.ui.clickgui.components.impl.*
import java.awt.Color

data class StructureEspConfig(
    val enable: ToggleSetting,
    val highlightStyle: DropdownSetting,
    val color: ColorSetting,
    val tracer: ToggleSetting,
    val displayName: ToggleSetting,
    val displayScale: SliderSetting<Float>,
    val displayBackgroundOpacity: SliderSetting<Float>,
    val showNotification: ToggleSetting,
    val sendCoordsInChat: ToggleSetting,
)

fun Feature.structureEsp(section: String, defaultColor: Color): StructureEspConfig {
    if (configSettings.isNotEmpty()) {
        configSettings.add(SeparatorSetting())
    }
    configSettings.add(CategorySetting(section))

    val enable = ToggleSetting("Enable", true)
    val highlightStyle = DropdownSetting("Highlight Style", 2, listOf("Outline", "Filled", "Both"))
    val color = ColorSetting("ESP Color", defaultColor, false)
    val tracer = ToggleSetting("Tracer", false)
    val displayName = ToggleSetting("Display Name", true)
    val displayScale = SliderSetting("Name Scale", 1f, 0f, 1f, 0.05f)
    val displayBackgroundOpacity = SliderSetting("Display Background Opacity", 0.5f, 0f, 1f, 0.05f)
        .showIf { displayName.value }
    val showNotification = ToggleSetting("Show Notification", true)
    val sendCoordsInChat = ToggleSetting("Send Coords In Chat", true)

    displayScale.showIf { displayName.value }

    configSettings.addAll(
        listOf(
            enable,
            highlightStyle,
            color,
            tracer,
            displayName,
            displayScale,
            displayBackgroundOpacity,
            showNotification,
            sendCoordsInChat,
        )
    )

    return StructureEspConfig(
        enable,
        highlightStyle,
        color,
        tracer,
        displayName,
        displayScale,
        displayBackgroundOpacity,
        showNotification,
        sendCoordsInChat,
    )
}
