package com.github.gameringop.utils.dungeons.map.core

enum class DoorType {
    BLOOD, ENTRANCE, NORMAL, WITHER;

    companion object {
        fun fromMapColor(color: Int): DoorType? = when (color) {
            18 -> BLOOD
            30 -> ENTRANCE
            74, 82, 66, 62, 85, 63 -> NORMAL
            119 -> WITHER
            else -> null
        }
    }
}