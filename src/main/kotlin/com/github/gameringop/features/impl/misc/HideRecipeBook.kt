package com.github.gameringop.features.impl.misc

import com.github.gameringop.features.Feature
import com.github.gameringop.ui.clickgui.components.impl.ToggleSetting

object HideRecipeBook: Feature("Hides the recipe book button in inventory GUIs.") {
    @JvmStatic val closeRecipeBook by ToggleSetting("Close Recipe Book", false).withDescription("Also closes the recipe book screen.")
}