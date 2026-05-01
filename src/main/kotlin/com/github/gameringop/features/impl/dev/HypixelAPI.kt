package com.github.gameringop.features.impl.dev

import com.github.gameringop.SoTerm
import com.github.gameringop.features.Feature
import com.github.gameringop.ui.clickgui.components.getValue
import com.github.gameringop.ui.clickgui.components.impl.ButtonSetting
import com.github.gameringop.ui.clickgui.components.impl.TextInputSetting
import com.github.gameringop.ui.clickgui.components.provideDelegate
import com.github.gameringop.ui.clickgui.components.withDescription
import com.github.gameringop.utils.ChatUtils
import com.github.gameringop.utils.network.ApiUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred

object HypixelAPI : Feature("Hypixel API Integration") {

    // ---------- Configuration ----------
    private const val PROXY_BASE = "https://api.soterm.workers.dev"
    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    private const val TIMEOUT = 10_000

    // ---------- Rate Limiting ----------
    private val globalRequestTimestamps = ConcurrentLinkedDeque<Long>()
    private const val GLOBAL_MAX_PER_SECOND = 5

    // ---------- Request history (last 5 minutes) ----------
    private val requestHistory = ConcurrentLinkedDeque<Long>()
    private const val REQUEST_HISTORY_WINDOW_MS = 5 * 60 * 1000L // 5 minutes

    // ---------- Pending requests deduplication ----------
    private val pendingFetches = ConcurrentHashMap<String, CompletableDeferred<Member?>>()

    // ---------- UI Settings ----------
    private val testProxy by ButtonSetting("Test Proxy", false) { testProxyConnection() }
    private val testUsername by TextInputSetting("Test Username", "")
        .withDescription("Enter a username to check for Legendary Spirit pet")
    private val checkSpirit by ButtonSetting("Check Spirit Pet", false) {
        if (testUsername.value.isNotBlank()) checkSpecificPlayer(testUsername.value)
        else ChatUtils.modMessage("§cPlease enter a username first!")
    }
    private val checkSecrets by ButtonSetting("Check Total Secrets", false) {
        if (testUsername.value.isNotBlank()) checkSpecificPlayerSecrets(testUsername.value)
        else ChatUtils.modMessage("§cPlease enter a username first!")
    }
    private val checkCataExp by ButtonSetting("Check Catacombs XP", false) {
        if (testUsername.value.isNotBlank()) checkSpecificPlayerCataExp(testUsername.value)
        else ChatUtils.modMessage("§cPlease enter a username first!")
    }
    private val checkMagicalPower by ButtonSetting("Check Magical Power", false) {
        if (testUsername.value.isNotBlank()) checkSpecificPlayerMagicalPower(testUsername.value)
        else ChatUtils.modMessage("§cPlease enter a username first!")
    }
    private val clearCache by ButtonSetting("Clear All Caches", false) {
        uuidCache.clear()
        userMemberCache.clear()
        displayNameCache.clear()
        globalRequestTimestamps.clear()
        pendingFetches.clear()
        requestHistory.clear()   // also clear request history
        ChatUtils.modMessage("§aAll caches cleared!")
    }
    private val showCache by ButtonSetting("Show Cache", false) {
        if (displayNameCache.isEmpty()) {
            ChatUtils.modMessage("§eNo cached user data")
            return@ButtonSetting
        }
        ChatUtils.modMessage("§6=== Cached Users (${displayNameCache.size}) ===")
        displayNameCache.forEach { (lower, original) ->
            val member = userMemberCache[lower] ?: return@forEach
            val hasSpirit = member.pets_data?.pets?.any { it.isSpirit } ?: false
            val spiritStatus = if (hasSpirit) "§a✓ (Spirit)" else "§c✗ (No Spirit)"
            val secrets = member.dungeons?.secrets ?: -1
            val secretsStr = if (secrets >= 0) secrets.toString() else "?"
            val exp = member.dungeons?.dungeon_types?.catacombs?.experience ?: -1.0
            val cataLvl = if (exp >= 0) ApiUtils.getCatacombsLevel(exp).toString() else "?"
            val mp = member.accessory_bag_storage?.highest_magical_power ?: -1
            val mpStr = if (mp >= 0) mp.toString() else "?"

            ChatUtils.modMessage("§f$original: $spiritStatus §c($cataLvl) §6($mpStr) §b($secretsStr)")
        }
        ChatUtils.modMessage("§6==================")
    }

    // ---------- Caches ----------
    private val uuidCache = ConcurrentHashMap<String, String>()
    private val userMemberCache = ConcurrentHashMap<String, Member>()
    private val displayNameCache = ConcurrentHashMap<String, String>()

    // ---------- Coroutine Scope ----------
    private val apiScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ---------- JSON ----------
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    // ---------- Data Classes ----------
    @Serializable
    data class MojangProfile(val id: String, val name: String)

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
        val favorite_arrow: String? = null,
        val selected_power: String? = null,
        val blood_mobs_killed: Int? = null,
        val armor_data: String? = null,
        val talisman_bag_data: String? = null,
        val pets_data: PetsData? = null,
        val dungeons: DungeonStats? = null,
        val accessory_bag_storage: AccessoryBagStorage? = null
    )

    @Serializable
    data class PetsData(val pets: List<Pet>? = null)

    @Serializable
    data class Pet(val type: String, val tier: String, val heldItem: String? = null) {
        val isSpirit: Boolean
            get() = type.equals("SPIRIT", ignoreCase = true) &&
                    (tier.equals("LEGENDARY", ignoreCase = true) ||
                            (tier.equals("EPIC", ignoreCase = true) && heldItem == "PET_ITEM_TIER_BOOST"))
    }

    @Serializable
    data class DungeonStats(
        val secrets: Int? = null,
        val dungeon_types: DungeonTypes? = null,
        val player_classes: Map<String, PlayerClassData>? = null
    )

    @Serializable
    data class PlayerClassData(val experience: Double? = null)

    @Serializable
    data class DungeonTypes(
        val catacombs: TierData? = null,
        val master_catacombs: TierData? = null
    )

    @Serializable
    data class TierData(
        val experience: Double? = null,
        val fastest_time_s_plus: Map<String, Double>? = null,
        val tier_completions: Map<String, Double>? = null
    )

    @Serializable
    data class AccessoryBagStorage(
        val highest_magical_power: Int? = null
    )

    // ---------- Public non-suspend API ----------
    fun checkSpiritPet(username: String): Boolean {
        val member = userMemberCache[username.lowercase()]
        if (member != null) {
            val hasSpirit = member.pets_data?.pets?.any { it.isSpirit } ?: false
            if (SoTerm.debugFlags.contains("request")) {
                val msg = if (hasSpirit) "§a$username has Legendary Spirit pet" else "§c$username does NOT have Legendary Spirit pet"
                ChatUtils.modMessage(msg)
            }
            return hasSpirit
        }
        fetchUserData(username)
        if (SoTerm.debugFlags.contains("request"))
            ChatUtils.modMessage("§eFetching data for $username, assuming Spirit for now")
        return true
    }

    fun getTotalSecrets(username: String): Long {
        val member = userMemberCache[username.lowercase()]
        if (member != null) {
            val secrets = member.dungeons?.secrets?.toLong() ?: -1L
            if (SoTerm.debugFlags.contains("request") && secrets >= 0)
                ChatUtils.modMessage("§a$username has $secrets total dungeon secrets")
            return secrets
        }
        fetchUserData(username)
        if (SoTerm.debugFlags.contains("request"))
            ChatUtils.modMessage("§eFetching data for $username, cannot retrieve secrets yet")
        return -1L
    }

    fun getCatacombsExperience(username: String): Double {
        val member = userMemberCache[username.lowercase()]
        if (member != null) {
            val exp = member.dungeons?.dungeon_types?.catacombs?.experience ?: -1.0
            if (SoTerm.debugFlags.contains("request") && exp >= 0)
                ChatUtils.modMessage("§a$username has ${exp.toLong()} catacombs experience")
            return exp
        }
        fetchUserData(username)
        if (SoTerm.debugFlags.contains("request"))
            ChatUtils.modMessage("§eFetching data for $username, cannot retrieve catacombs XP yet")
        return -1.0
    }

    fun getMagicalPower(username: String): Int {
        val member = userMemberCache[username.lowercase()]
        if (member != null) {
            val mp = member.accessory_bag_storage?.highest_magical_power ?: -1
            if (SoTerm.debugFlags.contains("request") && mp >= 0)
                ChatUtils.modMessage("§a$username has §e$mp §amagical power")
            return mp
        }
        fetchUserData(username)
        if (SoTerm.debugFlags.contains("request"))
            ChatUtils.modMessage("§eFetching data for $username, cannot retrieve magical power yet")
        return -1
    }

    fun getCachedMember(username: String): Member? = userMemberCache[username.lowercase()]

    // ---------- Public suspend API ----------
    suspend fun fetchSpiritPet(username: String): Boolean {
        val member = getMemberData(username)
        return member?.pets_data?.pets?.any { it.isSpirit } ?: false
    }

    suspend fun fetchTotalSecrets(username: String): Long {
        val member = getMemberData(username)
        return member?.dungeons?.secrets?.toLong() ?: -1L
    }

    suspend fun fetchCatacombsExperience(username: String): Double {
        val member = getMemberData(username)
        return member?.dungeons?.dungeon_types?.catacombs?.experience ?: -1.0
    }

    suspend fun fetchMagicalPower(username: String): Int {
        val member = getMemberData(username)
        return member?.accessory_bag_storage?.highest_magical_power ?: -1
    }

    suspend fun fetchFastestTime(username: String, floor: String): Long {
        val member = getMemberData(username) ?: return -1L
        val floorUpper = floor.uppercase()
        val (mode, floorNum) = when {
            floorUpper.startsWith("F") && floorUpper.length > 1 -> {
                val num = floorUpper.substring(1).toIntOrNull()
                if (num !in 0..7) return -1L
                "catacombs" to num.toString()
            }
            floorUpper.startsWith("M") && floorUpper.length > 1 -> {
                val num = floorUpper.substring(1).toIntOrNull()
                if (num !in 1..7) return -1L
                "master_catacombs" to num.toString()
            }
            else -> return -1L
        }
        val tierData = when (mode) {
            "catacombs" -> member.dungeons?.dungeon_types?.catacombs
            "master_catacombs" -> member.dungeons?.dungeon_types?.master_catacombs
            else -> null
        }
        val timeMs = tierData?.fastest_time_s_plus?.get(floorNum) ?: return -1L
        return timeMs.toLong()
    }

    suspend fun fetchTotalRuns(username: String): Int {
        val member = getMemberData(username) ?: return -1
        val catacombs = member.dungeons?.dungeon_types?.catacombs?.tier_completions?.get("total")?.toInt() ?: 0
        val master = member.dungeons?.dungeon_types?.master_catacombs?.tier_completions?.get("total")?.toInt() ?: 0
        return catacombs + master
    }

    suspend fun fetchMemberData(username: String): Member? = getMemberData(username)

    suspend fun getRequestCount(): Int {
        cleanRequestHistory()
        return requestHistory.size
    }

    // ---------- Core fetch logic with rate limiting and deduplication ----------
    private fun cleanRequestHistory() {
        val now = System.currentTimeMillis()
        while (requestHistory.isNotEmpty() && now - requestHistory.first >= REQUEST_HISTORY_WINDOW_MS) {
            requestHistory.removeFirst()
        }
    }

    private suspend fun getMemberData(username: String): Member? {
        val lower = username.lowercase()
        userMemberCache[lower]?.let { return it }

        val existing = pendingFetches[lower]
        if (existing != null) {
            return existing.await()
        }

        val deferred = CompletableDeferred<Member?>()
        pendingFetches[lower] = deferred

        try {
            // Apply global rate limiting
            val now = System.currentTimeMillis()
            while (globalRequestTimestamps.isNotEmpty() && now - globalRequestTimestamps.first >= 1000) {
                globalRequestTimestamps.removeFirst()
            }
            if (globalRequestTimestamps.size >= GLOBAL_MAX_PER_SECOND) {
                // Rate limit exceeded, do not fetch now.
                deferred.complete(null)
                return null
            }
            globalRequestTimestamps.addLast(now)

            val result = performFetch(username)
            if (result != null) {
                userMemberCache[lower] = result
                displayNameCache[lower] = username
                if (SoTerm.debugFlags.contains("request")) {
                    ChatUtils.modMessage("§a[API] Cached $username")
                }
                // Record successful request (after rate limiting passed)
                requestHistory.addLast(System.currentTimeMillis())
                cleanRequestHistory() // keep only last 5 minutes
            }
            deferred.complete(result)
            return result
        } finally {
            pendingFetches.remove(lower, deferred)
        }
    }

    private suspend fun performFetch(username: String): Member? {
        val uuid = getUUIDFromUsername(username) ?: return null
        val profiles = fetchProfiles(uuid) ?: return null
        val selected = profiles.profiles?.find { it.selected } ?: return null
        return selected.members[uuid]
    }

    private suspend fun fetchProfiles(uuid: String): SkyblockProfiles? {
        val url = "$PROXY_BASE/v2/skyblock/profiles?uuid=$uuid"
        val result = get(url)
        result.onSuccess { body ->
            return try {
                val profiles = json.decodeFromString<SkyblockProfiles>(body)
                if (profiles.success) profiles else null
            } catch (e: Exception) {
                null
            }
        }.onFailure { null }
        return null
    }

    private suspend fun getUUIDFromUsername(username: String): String? {
        uuidCache[username]?.let { return it }
        val url = "https://api.mojang.com/users/profiles/minecraft/$username"
        val result = get(url)
        result.onSuccess { body ->
            val profile = json.decodeFromString<MojangProfile>(body)
            uuidCache[username] = profile.id
            return profile.id
        }
        return null
    }

    // ---------- UI test and buttons ----------
    private fun fetchUserData(username: String) {
        if (userMemberCache.containsKey(username.lowercase())) return
        apiScope.launch {
            getMemberData(username)
        }
    }

    private fun testProxyConnection() {
        apiScope.launch {
            val url = "$PROXY_BASE/v2/player?name=Hypixel"
            val result = get(url)
            result.onSuccess { body ->
                val jsonObj = json.decodeFromString<JsonObject>(body)
                val success = jsonObj["success"]?.jsonPrimitive?.booleanOrNull ?: false
                val cause = jsonObj["cause"]?.jsonPrimitive?.contentOrNull
                when {
                    cause?.contains("You have already looked up this name recently") == true -> {
                        ChatUtils.modMessage("§aProxy is working! (Rate limited - still good)")
                        if (SoTerm.debugFlags.contains("request")) ChatUtils.modMessage("§7$body")
                    }
                    success -> ChatUtils.modMessage("§aProxy is working!")
                    else -> {
                        ChatUtils.modMessage("§cProxy returned error: $cause")
                        if (SoTerm.debugFlags.contains("request")) ChatUtils.modMessage("§7$body")
                    }
                }
            }.onFailure { error ->
                val msg = error.message ?: ""
                if (msg.contains("429") || msg.contains("You have already looked up this name recently")) {
                    ChatUtils.modMessage("§aProxy is working! (Rate limited - still good)")
                    if (SoTerm.debugFlags.contains("request")) ChatUtils.modMessage("§7$msg")
                } else {
                    ChatUtils.modMessage("§cFailed to connect to proxy: ${error.message}")
                }
            }
        }
    }

    private fun checkSpecificPlayer(username: String) {
        ChatUtils.modMessage("§eChecking Spirit pet for §f$username§e...")
        apiScope.launch {
            val member = getMemberData(username)
            if (member == null) {
                ChatUtils.modMessage("§cFailed to fetch profile for $username")
                return@launch
            }
            val hasSpirit = member.pets_data?.pets?.any { it.isSpirit } ?: false
            if (hasSpirit) {
                ChatUtils.modMessage("§a$username has a Legendary Spirit pet! §7(§6Spirit§7)")
            } else {
                ChatUtils.modMessage("§c$username does NOT have a Legendary Spirit pet")
            }
        }
    }

    private fun checkSpecificPlayerSecrets(username: String) {
        ChatUtils.modMessage("§eFetching total secrets for §f$username§e...")
        apiScope.launch {
            val member = getMemberData(username)
            if (member == null) {
                ChatUtils.modMessage("§cFailed to fetch profile for $username")
                return@launch
            }
            val secrets = member.dungeons?.secrets ?: -1
            if (secrets >= 0) {
                ChatUtils.modMessage("§a$username has §e$secrets §atotal dungeon secrets")
            } else {
                ChatUtils.modMessage("§cCould not find secrets data for $username")
            }
        }
    }

    private fun checkSpecificPlayerCataExp(username: String) {
        ChatUtils.modMessage("§eFetching catacombs experience for §f$username§e...")
        apiScope.launch {
            val member = getMemberData(username)
            if (member == null) {
                ChatUtils.modMessage("§cFailed to fetch profile for $username")
                return@launch
            }
            val exp = member.dungeons?.dungeon_types?.catacombs?.experience ?: -1.0
            if (exp >= 0) {
                ChatUtils.modMessage("§a$username has §e${exp.toLong()} §acatacombs experience")
            } else {
                ChatUtils.modMessage("§cCould not find catacombs experience for $username")
            }
        }
    }

    private fun checkSpecificPlayerMagicalPower(username: String) {
        ChatUtils.modMessage("§eFetching magical power for §f$username§e...")
        apiScope.launch {
            val member = getMemberData(username)
            if (member == null) {
                ChatUtils.modMessage("§cFailed to fetch profile for $username")
                return@launch
            }
            val mp = member.accessory_bag_storage?.highest_magical_power ?: -1
            if (mp >= 0) {
                ChatUtils.modMessage("§a$username has §e$mp §amagical power")
            } else {
                ChatUtils.modMessage("§cCould not find magical power for $username")
            }
        }
    }

    // ---------- Low-level HTTP ----------
    private suspend fun get(url: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = URI(url).toURL().openConnection() as HttpURLConnection
            connection.apply {
                connectTimeout = TIMEOUT
                readTimeout = TIMEOUT
                setRequestProperty("User-Agent", USER_AGENT)
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream ?: connection.inputStream
            val response = stream.bufferedReader().use(BufferedReader::readText)
            if (code !in 200..299) throw IllegalStateException("HTTP $code: $response")
            response
        }
    }
}