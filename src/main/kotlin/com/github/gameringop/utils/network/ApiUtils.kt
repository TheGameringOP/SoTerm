package com.github.gameringop.utils.network

import com.github.gameringop.utils.JsonUtils.getArray
import com.github.gameringop.utils.JsonUtils.getBoolean
import com.github.gameringop.utils.items.ItemRarity
import com.github.gameringop.utils.items.ItemUtils
import com.github.gameringop.utils.items.ItemUtils.lore
import com.github.gameringop.utils.items.ItemUtils.skyblockId
import kotlinx.serialization.json.JsonObject
import net.minecraft.world.item.ItemStack
import kotlin.math.floor

object ApiUtils {
    private val xpRequirements = listOf(
        50, 125, 235, 395, 625, 955, 1425, 2095, 3045, 4385, 6275, 8940, 12700, 17960, 25340, 35640, 50040, 70040,
        97640, 135640, 188140, 259640, 356640, 488640, 668640, 911640, 1239640, 1683640, 2284640, 3084640, 4149640,
        5559640, 7459640, 9959640, 13259640, 17559640, 23159640, 30359640, 39559640, 51559640, 66559640, 85559640,
        109559640, 139559640, 177559640, 225559640, 285559640, 360559640, 453559640, 569809640,
    )

    fun getCatacombsLevel(totalXp: Double): Int {
        if (totalXp < 0) return 0
        for (i in xpRequirements.indices) {
            if (totalXp < xpRequirements[i].toDouble()) {
                return i
            }
        }

        val lastLevelInList = xpRequirements.size
        val xpRequiredForLastLevelInList = xpRequirements.last().toDouble()
        val xpBeyondLastLevelInList = totalXp - xpRequiredForLastLevelInList
        val levelsAboveLastLevel = (xpBeyondLastLevelInList / 200_000_000.0).toInt()
        return lastLevelInList + levelsAboveLastLevel
    }

    private val requiredRegex = Regex("§7§4☠ §cRequires §5.+§c.")
    fun getMagicalPower(talismanBag: MutableList<ItemStack?>, profileInfo: JsonObject): Int {
        return talismanBag.filterNotNull().map {
            val itemId = it.skyblockId.let { id -> if (id.startsWith("PARTY_HAT_")) "PARTY_HAT" else id }
            val unusable = it.lore.any { line -> requiredRegex.matches(line) }
            val rarity = ItemUtils.getRarity(it)

            val mp = if (unusable) 0
            else when (rarity) {
                ItemRarity.MYTHIC -> 22
                ItemRarity.LEGENDARY -> 16
                ItemRarity.EPIC -> 12
                ItemRarity.RARE -> 8
                ItemRarity.UNCOMMON -> 5
                ItemRarity.COMMON -> 3
                ItemRarity.SPECIAL -> 3
                ItemRarity.VERY_SPECIAL -> 5
                else -> 0
            }

            val bonus = when (itemId) {
                "HEGEMONY_ARTIFACT" -> mp
                "ABICASE" -> {
                    val contacts = profileInfo.getArray("abiphone_contacts")?.size ?: 0
                    floor(contacts / 2.0).toInt()
                }

                else -> 0
            }

            Pair(itemId, mp + bonus)
        }.groupBy { it.first }.mapValues { entry ->
            entry.value.maxBy { it.second }
        }.values.fold(0) { acc, pair ->
            acc + pair.second
        }.let {
            when {
                profileInfo.getBoolean("consumed_rift_prism") == true -> it + 11
                else -> it
            }
        }
    }
}