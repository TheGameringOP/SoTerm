private fun testApiKey() {
    if (apiKey.value.isBlank()) {
        ChatUtils.modMessage("§cPlease enter an API key first!")
        return
    }
    
    Thread {
        try {
            val url = "https://api.hypixel.net/v2/player?name=Hypixel"
            
            if (SoTerm.debugFlags.contains("link")) {
                ChatUtils.modMessage("§7Request URL: $url")
                ChatUtils.modMessage("§7API Key: ${apiKey.value.take(8)}...${apiKey.value.takeLast(4)}")
            }
            
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("API-Key", apiKey.value)
                .build()
            
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                
                if (SoTerm.debugFlags.contains("link")) {
                    ChatUtils.modMessage("§7Response code: ${response.code}")
                }
                
                if (response.code == 403) {
                    if (responseBody.contains("rate", ignoreCase = true) || 
                        responseBody.contains("limit", ignoreCase = true) ||
                        responseBody.contains("throttle", ignoreCase = true)) {
                        ChatUtils.modMessage("§aAPI key is valid! (Rate limited - too many requests)")
                    } else {
                        ChatUtils.modMessage("§cAPI key is invalid! (403 Forbidden)")
                    }
                    return@use
                }
                
                if (responseBody.trimStart().startsWith("<")) {
                    if (SoTerm.debugFlags.contains("link")) {
                        ChatUtils.modMessage("§cReceived HTML instead of JSON")
                        ChatUtils.modMessage("§7First 200 chars: ${responseBody.take(200)}")
                    }
                    return@use
                }
                
                val jsonResponse = try {
                    gson.fromJson(responseBody, Map::class.java)
                } catch (e: Exception) {
                    if (SoTerm.debugFlags.contains("link")) {
                        ChatUtils.modMessage("§cFailed to parse JSON response")
                        ChatUtils.modMessage("§7Raw response: ${responseBody.take(200)}")
                    }
                    return@use
                }
                
                val cause = jsonResponse["cause"] as? String ?: ""
                if (cause.contains("You have already looked up this name recently")) {
                    ChatUtils.modMessage("§aAPI key is valid! (Rate limited - key works)")
                    return@use
                }
                
                if (response.isSuccessful && jsonResponse["success"] == true) {
                    ChatUtils.modMessage("§aAPI key is valid!")
                } else {
                    ChatUtils.modMessage("§cAPI key is invalid! $cause")
                }
            }
        } catch (e: Exception) {
            ChatUtils.modMessage("§cFailed to test API key: ${e.message}")
            if (SoTerm.debugFlags.contains("link")) {
                e.printStackTrace()
            }
        }
    }.start()
}
