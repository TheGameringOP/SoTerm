package com.github.gameringop.config

object PersonalBest {
    private val pbData = PogObject("personal_bests", mutableMapOf<String, Number>())

    /**
     * @return true if this was a new Personal Best, false otherwise.
     */
    fun checkAndSetPB(key: String, value: Number, lowerIsBetter: Boolean = true): Boolean {
        val dataMap = pbData.getData()
        val currentPB = dataMap[key]

        val isNewPB = when {
            currentPB == null -> true

            lowerIsBetter && value.toDouble() < currentPB.toDouble() -> true
            ! lowerIsBetter && value.toDouble() > currentPB.toDouble() -> true

            else -> false
        }

        if (isNewPB) {
            dataMap[key] = value
            pbData.save()
        }

        return isNewPB
    }

    fun getPB(key: String): Number? {
        return pbData.getData()[key]
    }
}