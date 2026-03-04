package com.github.gameringop.features.impl.dev

import com.github.gameringop.SoTerm
import com.github.gameringop.features.Feature
import com.github.gameringop.ui.clickgui.components.getValue
import com.github.gameringop.ui.clickgui.components.impl.ButtonSetting
import com.github.gameringop.ui.clickgui.components.impl.TextInputSetting
import com.github.gameringop.ui.clickgui.components.impl.ToggleSetting
import com.github.gameringop.ui.clickgui.components.provideDelegate
import com.github.gameringop.ui.clickgui.components.section
import com.github.gameringop.ui.clickgui.components.withDescription
import com.github.gameringop.utils.ChatUtils
import com.github.gameringop.utils.ThreadUtils
import com.github.gameringop.utils.dungeons.DungeonListener
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object HypixelAPI : Feature("Hypixel API Integration") {
    
    private val apiEnabled by ToggleSetting("Enabled", false).section("API")
    private val apiKey by TextInputSetting("API Key", "")
        .withDescription("Get your API key from https://developer.hypixel.net/")
    
    private val testKey by ButtonSetting("Test API Key", false) {
        testApiKey()
    }
    
    private val clearCache by ButtonSetting("Clear Spirit Cache", false) {
        spiritCache.clear()
        uuidCache.clear()
        ChatUtils.modMessage("§aSpirit pet cache cleared!")
    }
    
    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    
    private val uuidCache = ConcurrentHashMap<String, String>()
    private val uuidPendingRequests = ConcurrentHashMap<String, Long>()
    
    private val spiritCache = ConcurrentHashMap<String, Boolean>()
    private val spiritPendingRequests = ConcurrentHashMap<String, Long>()
    
    data class HypixelKeyResponse(
        val success: Boolean,
        val cause: String? = null,
        val record: KeyRecord? = null
    )
    
    data class KeyRecord(
        val key: String,
        val owner: String,
        val limit: Int,
        val queriesInPastMin: Int,
        val totalQueries: Int
    )
    
    data class HypixelProfile(
        val success: Boolean,
        val cause: String? = null,
        val player: PlayerData? = null
    )
    
    data class PlayerData(
        val displayname: String,
        val uuid: String
    )
    
    data class SkyblockProfiles(
        val success: Boolean,
        val cause: String? = null,
        val profiles: List<Profile>? = null
    )
    
    data class Profile(
        val profile_id: String,
        val cute_name: String,
        val selected: Boolean,
        val members: Map<String, Member>
    )
    
    data class Member(
        val pets_data: PetsData? = null
    )
    
    data class PetsData(
        val pets: List<Pet>? = null
    )
    
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
    
    private fun testApiKey() {
        if (apiKey.value.isBlank()) {
            ChatUtils.modMessage("§cPlease enter an API key first!")
            return
        }
        
        ThreadUtils.scheduledTask(0) {
            try {
                val url = "https://api.hypixel.net/v2/player?name=Hypixel"
                
                // Debug: show the URL being requested
                if (SoTerm.debugFlags.contains("link")) {
                    ChatUtils.modMessage("§7Request URL: $url")
                    ChatUtils.modMessage("§7API Key: ${apiKey.value.take(8)}...${apiKey.value.takeLast(4)}")
                }
                
                val request = Request.Builder()
                    .url(url)
                    .header("API-Key", apiKey.value)
                    .header("User-Agent", "SoTerm-Mod/1.0")
                    .build()
                
                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string() ?: ""
                    
                    // Always show response code
                    ChatUtils.modMessage("§7Response code: ${response.code}")
                    
                    // Show more details for 403
                    if (response.code == 403) {
                        ChatUtils.modMessage("§c403 Forbidden - Your API key is likely invalid or has been revoked")
                        if (SoTerm.debugFlags.contains("link")) {
                            ChatUtils.modMessage("§7Response headers: ${response.headers}")
                            ChatUtils.modMessage("§7Raw response: ${responseBody.take(200)}")
                        }
                        return@use
                    }
                    
                    // Check if it's HTML (common error response)
                    if (responseBody.trimStart().startsWith("<")) {
                        ChatUtils.modMessage("§cReceived HTML instead of JSON")
                        if (SoTerm.debugFlags.contains("link")) {
                            ChatUtils.modMessage("§7First 200 chars: ${responseBody.take(200)}")
                        }
                        return@use
                    }
                    
                    // Try to parse as Map
                    val jsonResponse = try {
                        gson.fromJson(responseBody, Map::class.java)
                    } catch (e: Exception) {
                        ChatUtils.modMessage("§cFailed to parse JSON response")
                        if (SoTerm.debugFlags.contains("link")) {
                            ChatUtils.modMessage("§7Raw response: ${responseBody.take(200)}")
                        }
                        return@use
                    }
                    
                    if (response.isSuccessful && jsonResponse["success"] == true) {
                        ChatUtils.modMessage("§aAPI key is valid!")
                        
                        val rateLimit = response.header("RateLimit-Limit")
                        val rateRemaining = response.header("RateLimit-Remaining")
                        if (rateLimit != null && rateRemaining != null) {
                            ChatUtils.modMessage("§7Rate limit: $rateRemaining/$rateLimit remaining")
                        }
                    } else {
                        val cause = jsonResponse["cause"] as? String ?: "Unknown error"
                        ChatUtils.modMessage("§cAPI key is invalid! $cause")
                    }
                }
            } catch (e: Exception) {
                ChatUtils.modMessage("§cFailed to test API key: ${e.message}")
                if (SoTerm.debugFlags.contains("link")) {
                    e.printStackTrace()
                }
            }
        }
    }
    
    fun getUUIDFromUsername(username: String): String? {
        uuidCache[username]?.let { return it }
        
        if (uuidPendingRequests.containsKey(username)) {
            val lastRequest = uuidPendingRequests[username] ?: 0
            if (System.currentTimeMillis() - lastRequest < 60000) {
                return null
            }
        }
        
        uuidPendingRequests[username] = System.currentTimeMillis()
        
        ThreadUtils.scheduledTask(0) {
            try {
                val request = Request.Builder()
                    .url("https://api.mojang.com/users/profiles/minecraft/$username")
                    .build()
                
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        uuidPendingRequests.remove(username)
                        return@use
                    }
                    val json = response.body?.string() ?: return@use
                    val data = gson.fromJson(json, Map::class.java)
                    val uuid = data["id"] as? String
                    
                    if (uuid != null) {
                        uuidCache[username] = uuid
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                uuidPendingRequests.remove(username)
            }
        }
        
        return null
    }
    
    fun checkSpiritPet(username: String): Boolean {
        if (!apiEnabled.value || apiKey.value.isBlank()) {
            return true
        }
        
        spiritCache[username]?.let { return it }
        
        if (spiritPendingRequests.containsKey(username)) {
            val lastRequest = spiritPendingRequests[username] ?: 0
            if (System.currentTimeMillis() - lastRequest < 60000) {
                return false
            }
        }
        
        spiritPendingRequests[username] = System.currentTimeMillis()
        
        ThreadUtils.scheduledTask(0) {
            try {
                val uuid = getUUIDFromUsername(username) ?: run {
                    spiritCache[username] = true
                    if (SoTerm.debugFlags.contains("spirit")) {
                        ChatUtils.modMessage("§cFailed to get UUID for $username, assuming Spirit")
                    }
                    return@scheduledTask
                }
                
                val profilesRequest = Request.Builder()
                    .url("https://api.hypixel.net/v2/skyblock/profiles?uuid=$uuid")
                    .header("API-Key", apiKey.value)
                    .build()
                
                client.newCall(profilesRequest).execute().use { response ->
                    if (!response.isSuccessful) {
                        spiritCache[username] = true
                        if (SoTerm.debugFlags.contains("spirit")) {
                            ChatUtils.modMessage("§cAPI request failed for $username, assuming Spirit")
                        }
                        return@use
                    }
                    
                    val json = response.body?.string() ?: return@use
                    val profilesResponse = gson.fromJson(json, SkyblockProfiles::class.java)
                    
                    if (!profilesResponse.success) {
                        spiritCache[username] = true
                        if (SoTerm.debugFlags.contains("spirit")) {
                            ChatUtils.modMessage("§cAPI returned error for $username: ${profilesResponse.cause}, assuming Spirit")
                        }
                        return@use
                    }
                    
                    val selectedProfile = profilesResponse.profiles?.find { it.selected }
                    
                    if (selectedProfile == null) {
                        spiritCache[username] = true
                        if (SoTerm.debugFlags.contains("spirit")) {
                            ChatUtils.modMessage("§cNo selected profile found for $username, assuming Spirit")
                        }
                        return@use
                    }
                    
                    val member = selectedProfile.members[uuid]
                    
                    val hasLegendarySpirit = member?.pets_data?.pets?.any { it.isSpirit } ?: false
                    
                    spiritCache[username] = hasLegendarySpirit
                    
                    if (SoTerm.debugFlags.contains("spirit")) {
                        ChatUtils.modMessage("§aSpirit pet check for $username: $hasLegendarySpirit")
                    }
                }
            } catch (e: Exception) {
                if (SoTerm.debugFlags.contains("spirit")) {
                    ChatUtils.modMessage("§cSpirit pet check failed for $username: ${e.message}, assuming Spirit")
                }
                spiritCache[username] = true
            } finally {
                spiritPendingRequests.remove(username)
            }
        }
        
        return false
    }
    
    fun getSpiritStatus(username: String): Boolean? = spiritCache[username]
    
    fun isSpiritLoaded(username: String): Boolean = spiritCache.containsKey(username)
    
    fun preloadTeammates() {
        if (!apiEnabled.value || apiKey.value.isBlank()) return
        
        DungeonListener.dungeonTeammatesNoSelf.forEach { teammate ->
            if (!isSpiritLoaded(teammate.name)) {
                checkSpiritPet(teammate.name)
            }
        }
    }
}
