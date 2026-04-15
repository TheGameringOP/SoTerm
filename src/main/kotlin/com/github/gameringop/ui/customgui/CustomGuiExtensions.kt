package com.github.gameringop.ui.customgui

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.inventory.Slot

var AbstractContainerScreen<*>.customGui: CustomGui?
    get() = (this as IHasCustomGui).soterm_getCustomGui()
    set(value) = (this as IHasCustomGui).soterm_setCustomGui(value)

val Slot.originalX: Int get() = (this as ICoordRememberingSlot).soterm_getOriginalX()
val Slot.originalY: Int get() = (this as ICoordRememberingSlot).soterm_getOriginalY()

fun Slot.rememberCoords() = (this as ICoordRememberingSlot).soterm_rememberCoords()
fun Slot.restoreCoords() = (this as ICoordRememberingSlot).soterm_restoreCoords()

fun Slot.setSlotX(x: Int) = (this as ICoordRememberingSlot).soterm_setX(x)
fun Slot.setSlotY(y: Int) = (this as ICoordRememberingSlot).soterm_setY(y)