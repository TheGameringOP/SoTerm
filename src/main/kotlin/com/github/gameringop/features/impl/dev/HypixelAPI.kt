package com.github.gameringop.features.impl.dev

import com.github.gameringop.SoTerm
import com.github.gameringop.features.Feature
import com.github.gameringop.ui.clickgui.components.getValue
import com.github.gameringop.ui.clickgui.components.impl.ButtonSetting
import com.github.gameringop.ui.clickgui.components.impl.TextInputSetting
import com.github.gameringop.ui.clickgui.components.provideDelegate
import com.github.gameringop.ui.clickgui.components.withDescription
import com.github.gameringop.utils.ChatUtils
import com.github.gameringop.utils.ThreadUtils
import com.github.gameringop.utils.network.ProfileUtils
import com.github.gameringop.utils.network.WebApi
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.concurrent.ConcurrentHashMap

object `HypixelAPI` : Feature("Hypixel API Integration") {

    private val testUsername by TextInputSetting("Test Username", "")
        .withDescription("Enter a username to check for Legendary Spirit pet")

    private val checkSpirit by ButtonSetting("Check Spirit Pet", false) {
        if (testUsername.value.isNotBlank()) {
            checkSpecificPlayer(testUsername.value)
        } else {
            ChatUtils.modMessage("§cPlease enter a username first!")
        }
    }

    private val clearCache by ButtonSetting("Clear Spirit Cache", false) {
        spiritCache.clear()
        uuidCache.clear()
        ChatUtils.modMessage("§aSpirit pet cache cleared!")
    }

    private val showCache by ButtonSetting("Show Spirit Cache", false) {
        if (spiritCache.isEmpty()) {
            ChatUtils.modMessage("§eSpirit cache is empty")
            return@ButtonSetting
        }

        ChatUtils.modMessage("§6=== Spirit Cache ===")
        spiritCache.forEach { (username, hasSpirit) ->
            val status = if (hasSpirit) "§a✓ (Spirit)" else "§c✗ (No Spirit)"
            ChatUtils.modMessage("§f$username: $status")
        }
        ChatUtils.modMessage("§6==================")
    }

    private val json = Json { ignoreUnknownKeys = true }

    private val uuidCache = ConcurrentHashMap<String, String>()
    private val spiritCache = ConcurrentHashMap<String, Boolean>()
    private val pendingUuidRequests = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    @Serializable
    data class MojangProfile(
        val id: String,
        val name: String
    )

    @Serializable
    data class HypixelErrorResponse(
        val success: Boolean,
        val cause: String? = null
    )

    @Serializable
    data class SkyblockProfiles(
        val success: Boolean,
        val cause: String? = null,
        val profiles: List<Profile>? = null
    )

    @Serializable
    data class Profile(
        val profile_id: String,
        val cute_name: String,
        val selected: Boolean,
        val members: Map<String, Member>
    )

    @Serializable
    data class Member(
        val pets_data: PetsData? = null
    )

    @Serializable
    data class PetsData(
        val pets: List<Pet>? = null
    )

    @Serializable
    data class Pet(
        val type: String,
        val tier: String,
        val heldItem: String? = null
    ) {
        val isSpirit: Boolean
            get() = type.equals("SPIRIT", ignoreCase = true) &&
                    (tier.equals("LEGENDARY", ignoreCase = true) ||
                            (tier.equals("EPIC", ignoreCase = true) && heldItem == "PET_ITEM_TIER_BOOST"))
    }

    private fun checkSpecificPlayer(username: String) {
        ChatUtils.modMessage("§eChecking Spirit pet for §f$username§e...")

        val hasSpirit = checkSpiritPet(username)
        if (hasSpirit) {
            ChatUtils.modMessage("§a$username has a Legendary Spirit pet! §7(§6Spirit§7)")
        } else {
            ChatUtils.modMessage("§c$username does NOT have a Legendary Spirit pet")
        }

        spiritCache[username] = hasSpirit
    }

    private fun getUUIDFromUsername(username: String): String? {
        uuidCache[username]?.let { return it }

        if (pendingUuidRequests.contains(username)) return null

        pendingUuidRequests.add(username)
        ThreadUtils.async(Runnable {
            try {
                val url = "https://api.mojang.com/users/profiles/minecraft/$username"
                val response = runBlocking { WebApi.getString(url) }

                response.onSuccess { responseBody ->
                    val profile = json.decodeFromString<MojangProfile>(responseBody)
                    uuidCache[username] = profile.id
                }.onFailure {
                }
            } catch (e: Exception) {
            } finally {
                pendingUuidRequests.remove(username)
            }
        })

        return null
    }

    fun checkSpiritPet(username: String): Boolean {
        spiritCache[username]?.let { return it }

        if (getUUIDFromUsername(username) == null) {
            ChatUtils.modMessage("§eUsername for $username, is not found")
            return false
        }

        ThreadUtils.async(Runnable {
            try {
                val cleanName = username.lowercase().trim()
                val profileData = runBlocking { ProfileUtils.getProfile(cleanName).getOrNull() }
                if (profileData == null) {
                    if (SoTerm.debugFlags.contains("spirit")) {
                        ChatUtils.modMessage("§eProfile fetch failed for $username, assuming Spirit")
                    }
                    spiritCache[username] = true
                    return@Runnable
                }

                val petsArray = profileData["pets"]?.jsonArray
                if (petsArray == null) {
                    if (SoTerm.debugFlags.contains("spirit")) {
                        ChatUtils.modMessage("§eNo pets array in profile for $username, assuming Spirit")
                    }
                    spiritCache[username] = true
                    return@Runnable
                }

                var hasSpirit = false
                for (petElement in petsArray) {
                    val petObj = petElement.jsonObject
                    val type = petObj["type"]?.jsonPrimitive?.contentOrNull ?: continue
                    val tier = petObj["tier"]?.jsonPrimitive?.contentOrNull ?: continue
                    val heldItem = petObj["heldItem"]?.jsonPrimitive?.contentOrNull

                    if (type.equals("SPIRIT", ignoreCase = true)) {
                        if (tier.equals("LEGENDARY", ignoreCase = true) ||
                            (tier.equals("EPIC", ignoreCase = true) && heldItem == "PET_ITEM_TIER_BOOST")
                        ) {
                            hasSpirit = true
                            break
                        }
                    }
                }

                if (SoTerm.debugFlags.contains("spirit")) {
                    if (hasSpirit) {
                        ChatUtils.modMessage("§a$username has Legendary Spirit pet")
                    } else {
                        ChatUtils.modMessage("§c$username does NOT have Legendary Spirit pet")
                    }
                }

                spiritCache[username] = hasSpirit
            } catch (e: Exception) {
                if (SoTerm.debugFlags.contains("spirit")) {
                    ChatUtils.modMessage("§eException for $username: ${e.message}, assuming Spirit")
                }
                spiritCache[username] = true
            }
        })

        spiritCache[username] = true
        return true
    }

    fun getSpiritStatus(username: String): Boolean? = spiritCache[username]

    fun isSpiritLoaded(username: String): Boolean = spiritCache.containsKey(username)
}