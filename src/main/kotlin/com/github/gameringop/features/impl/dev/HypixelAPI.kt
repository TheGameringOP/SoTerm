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
import com.github.gameringop.utils.network.WebUtils
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

object HypixelAPI : Feature("Hypixel API Integration") {

    private val apiKey by TextInputSetting("API Key", "")
        .withDescription("Get your API key from https://developer.hypixel.net/")

    private val testKey by ButtonSetting("Test API Key", false) {
        testApiKey()
    }

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

    val hasValidKey: Boolean
        get() = apiKey.value.isNotBlank()

    private val json = Json { ignoreUnknownKeys = true }

    private val uuidCache = ConcurrentHashMap<String, String>()
    private val spiritCache = ConcurrentHashMap<String, Boolean>()

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

    private fun withApiKey(url: String): String {
        val separator = if (url.contains("?")) "&" else "?"
        return "$url${separator}key=${apiKey.value}"
    }

    private fun testApiKey() {
        if (apiKey.value.isBlank()) {
            ChatUtils.modMessage("§cPlease enter an API key first!")
            return
        }

        ThreadUtils.async {
            try {
                val url = withApiKey("https://api.hypixel.net/v2/player?name=Hypixel")

                val response = runBlocking { WebUtils.getString(url) }

                response.onSuccess { responseBody ->
                    val jsonResponse = json.decodeFromString<Map<String, Any>>(responseBody)
                    val success = jsonResponse["success"] as? Boolean ?: false
                    val cause = jsonResponse["cause"] as? String

                    if (cause?.contains("You have already looked up this name recently") == true) {
                        ChatUtils.modMessage("§aAPI key is valid! (Rate limited - key works)")
                        return@onSuccess
                    }

                    if (success) {
                        ChatUtils.modMessage("§aAPI key is valid!")
                    } else {
                        ChatUtils.modMessage("§cAPI key is invalid! $cause")
                    }
                }.onFailure { error ->
                    ChatUtils.modMessage("§cFailed to test API key: ${error.message}")
                }
            } catch (e: Exception) {
                ChatUtils.modMessage("§cFailed to test API key: ${e.message}")
            }
        }
    }

    private fun checkSpecificPlayer(username: String) {
        if (apiKey.value.isBlank()) {
            ChatUtils.modMessage("§cPlease enter an API key first!")
            return
        }

        ChatUtils.modMessage("§eChecking Spirit pet for §f$username§e...")

        ThreadUtils.async {
            try {
                val uuid = getUUIDFromUsername(username)
                if (uuid == null) {
                    ChatUtils.modMessage("§cFailed to get UUID for $username")
                    return@async
                }

                val url = withApiKey("https://api.hypixel.net/v2/skyblock/profiles?uuid=$uuid")

                val response = runBlocking { WebUtils.getString(url) }

                response.onSuccess { responseBody ->
                    val profilesResponse = json.decodeFromString<SkyblockProfiles>(responseBody)

                    if (!profilesResponse.success) {
                        ChatUtils.modMessage("§cAPI error: ${profilesResponse.cause}")
                        return@onSuccess
                    }

                    val selectedProfile = profilesResponse.profiles?.find { it.selected }

                    if (selectedProfile == null) {
                        ChatUtils.modMessage("§cNo selected profile found for $username")
                        return@onSuccess
                    }

                    val member = selectedProfile.members[uuid]
                    val hasSpirit = member?.pets_data?.pets?.any { it.isSpirit } ?: false

                    if (hasSpirit) {
                        ChatUtils.modMessage("§a$username has a Legendary Spirit pet! §7(§6Spirit§7)")
                    } else {
                        ChatUtils.modMessage("§c$username does NOT have a Legendary Spirit pet")
                    }

                    spiritCache[username] = hasSpirit
                }.onFailure { error ->
                    ChatUtils.modMessage("§cFailed to check Spirit pet: ${error.message}")
                }
            } catch (e: Exception) {
                ChatUtils.modMessage("§cFailed to check Spirit pet: ${e.message}")
            }
        }
    }

    private fun getUUIDFromUsername(username: String): String? {
        uuidCache[username]?.let { return it }

        try {
            val url = "https://api.mojang.com/users/profiles/minecraft/$username"
            val response = runBlocking { WebUtils.getString(url) }

            response.onSuccess { responseBody ->
                val profile = json.decodeFromString<MojangProfile>(responseBody)
                uuidCache[username] = profile.id
                return profile.id
            }.onFailure {
                return null
            }
        } catch (e: Exception) {
            return null
        }

        return null
    }

    fun checkSpiritPet(username: String): Boolean {
        if (apiKey.value.isBlank()) {
            if (SoTerm.debugFlags.contains("spirit")) {
                ChatUtils.modMessage("§eNo API key, assuming Spirit for $username")
            }
            spiritCache[username] = true
            return true
        }

        spiritCache[username]?.let { return it }

        try {
            val uuid = getUUIDFromUsername(username)
            if (uuid == null) {
                if (SoTerm.debugFlags.contains("spirit")) {
                    ChatUtils.modMessage("§eUUID fetch failed for $username, assuming Spirit")
                }
                spiritCache[username] = true
                return true
            }

            val url = withApiKey("https://api.hypixel.net/v2/skyblock/profiles?uuid=$uuid")
            val response = runBlocking { WebUtils.getString(url) }

            response.onSuccess { responseBody ->
                val profilesResponse = json.decodeFromString<SkyblockProfiles>(responseBody)

                if (!profilesResponse.success) {
                    if (SoTerm.debugFlags.contains("spirit")) {
                        ChatUtils.modMessage("§eAPI error (${profilesResponse.cause}) for $username, assuming Spirit")
                    }
                    spiritCache[username] = true
                    return true
                }

                val selectedProfile = profilesResponse.profiles?.find { it.selected }

                if (selectedProfile == null) {
                    if (SoTerm.debugFlags.contains("spirit")) {
                        ChatUtils.modMessage("§eNo selected profile for $username, assuming Spirit")
                    }
                    spiritCache[username] = true
                    return true
                }

                val member = selectedProfile.members[uuid]
                val hasSpirit = member?.pets_data?.pets?.any { it.isSpirit } ?: false

                if (SoTerm.debugFlags.contains("spirit")) {
                    if (hasSpirit) {
                        ChatUtils.modMessage("§a$username has Legendary Spirit pet")
                    } else {
                        ChatUtils.modMessage("§c$username does NOT have Legendary Spirit pet")
                    }
                }

                spiritCache[username] = hasSpirit
                return hasSpirit
            }.onFailure { error ->
                if (SoTerm.debugFlags.contains("spirit")) {
                    ChatUtils.modMessage("§eRequest failed for $username: ${error.message}, assuming Spirit")
                }
                spiritCache[username] = true
                return true
            }

        } catch (e: Exception) {
            if (SoTerm.debugFlags.contains("spirit")) {
                ChatUtils.modMessage("§eException for $username: ${e.message}, assuming Spirit")
            }
            spiritCache[username] = true
            return true
        }

        spiritCache[username] = true
        return true
    }

    fun getSpiritStatus(username: String): Boolean? = spiritCache[username]

    fun isSpiritLoaded(username: String): Boolean = spiritCache.containsKey(username)
}