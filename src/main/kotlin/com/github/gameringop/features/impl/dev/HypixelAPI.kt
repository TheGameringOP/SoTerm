package com.github.gameringop.features.impl.dev

import com.github.gameringop.SoTerm
import com.github.gameringop.SoTerm.mc
import com.github.gameringop.features.Feature
import com.github.gameringop.ui.clickgui.components.impl.ButtonSetting
import com.github.gameringop.ui.clickgui.components.impl.TextInputSetting
import com.github.gameringop.ui.clickgui.components.impl.ToggleSetting
import com.github.gameringop.ui.clickgui.components.section
import com.github.gameringop.utils.ChatUtils
import com.github.gameringop.utils.ThreadUtils
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object HypixelAPI : Feature("Hypixel API Integration") {
    
    private val enabled by ToggleSetting("Enabled", false).section("Main")
    private val apiKey by TextInputSetting("API Key", "")
        .withDescription("Get your API key from https://developer.hypixel.net/")
    
    private val testKey by ButtonSetting("Test API Key", false) {
        testApiKey()
    }
    
    private val clearCache by ButtonSetting("Clear Spirit Cache", false) {
        spiritCache.clear()
        ChatUtils.modMessage("§aSpirit pet cache cleared!")
    }
    
    private val gson = Gson()
    private val client = OkHttpClient()
    private val spiritCache = ConcurrentHashMap<String, Boolean>()
    private val pendingRequests = ConcurrentHashMap<String, Long>()
    
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
        
        ThreadUtils.startThread {
            try {
                val request = Request.Builder()
                    .url("https://api.hypixel.net/key")
                    .header("API-Key", apiKey.value)
                    .build()
                
                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string() ?: ""
                    if (response.isSuccessful) {
                        ChatUtils.modMessage("§aAPI key is valid!")
                    } else {
                        ChatUtils.modMessage("§cAPI key is invalid! Response: $responseBody")
                    }
                }
            } catch (e: Exception) {
                ChatUtils.modMessage("§cFailed to test API key: ${e.message}")
            }
        }
    }
    
    fun getUUIDFromUsername(username: String): String? {
        return try {
            val request = Request.Builder()
                .url("https://api.mojang.com/users/profiles/minecraft/$username")
                .build()
            
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val json = response.body?.string() ?: return null
                val data = gson.fromJson(json, Map::class.java)
                data["id"] as? String
            }
        } catch (e: Exception) {
            null
        }
    }
    
    fun checkSpiritPet(username: String): Boolean {
        if (!enabled.value || apiKey.value.isBlank()) return false
        
        // Check cache first
        spiritCache[username]?.let { return it }
        
        // Check if request is pending (avoid spam)
        if (pendingRequests.containsKey(username)) {
            val lastRequest = pendingRequests[username] ?: 0
            if (System.currentTimeMillis() - lastRequest < 60000) { // 1 minute cooldown
                return false
            }
        }
        
        pendingRequests[username] = System.currentTimeMillis()
        
        ThreadUtils.startThread {
            try {
                // Get UUID
                val uuid = getUUIDFromUsername(username) ?: run {
                    spiritCache[username] = false
                    return@startThread
                }
                
                // Get selected Skyblock profile
                val profilesRequest = Request.Builder()
                    .url("https://api.hypixel.net/skyblock/profiles?uuid=$uuid")
                    .header("API-Key", apiKey.value)
                    .build()
                
                client.newCall(profilesRequest).execute().use { response ->
                    if (!response.isSuccessful) {
                        spiritCache[username] = false
                        return@use
                    }
                    
                    val json = response.body?.string() ?: return@use
                    val profiles = gson.fromJson(json, SkyblockProfiles::class.java)
                    
                    if (!profiles.success) {
                        spiritCache[username] = false
                        return@use
                    }
                    
                    // Find selected profile
                    val selectedProfile = profiles.profiles?.find { it.selected }
                    val member = selectedProfile?.members?.get(uuid)
                    
                    val hasSpirit = member?.pets_data?.pets?.any { it.isSpirit } ?: false
                    spiritCache[username] = hasSpirit
                    
                    if (SoTerm.debugFlags.contains("spirit")) {
                        ChatUtils.modMessage("§aSpirit pet check for $username: $hasSpirit")
                    }
                }
            } catch (e: Exception) {
                if (SoTerm.debugFlags.contains("spirit")) {
                    ChatUtils.modMessage("§cSpirit pet check failed for $username: ${e.message}")
                }
            } finally {
                pendingRequests.remove(username)
            }
        }
        
        return false // Return false while loading
    }
    
    fun getSpiritStatus(username: String): Boolean? = spiritCache[username]
    
    fun isSpiritLoaded(username: String): Boolean = username in spiritCache
}
