package com.github.gameringop.utils.dungeons.enums

import java.awt.Color

enum class DungeonClass(val color: Color, val code: String) {
    Archer(Color(125, 0, 0), "§4"),
    Berserk(Color(205, 106, 0), "§6"),
    Healer(Color(123, 0, 123), "§5"),
    Mage(Color(0, 185, 185), "§3"),
    Tank(Color(0, 125, 0), "§2"),
    Empty(Color(0, 0, 0), "§7");

    companion object {
        fun fromName(name: String): DungeonClass {
            return entries.find { it.name.equals(name, ignoreCase = true) } ?: Empty
        }
    }
}