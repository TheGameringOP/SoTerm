package com.github.gameringop.ui.customgui

interface IHasCustomGui {
    fun soterm_getCustomGui(): CustomGui?
    fun soterm_setCustomGui(gui: CustomGui?)
}

interface ICoordRememberingSlot {
    fun soterm_rememberCoords()
    fun soterm_restoreCoords()
    fun soterm_getOriginalX(): Int
    fun soterm_getOriginalY(): Int
    fun soterm_setX(x: Int)
    fun soterm_setY(y: Int)
}