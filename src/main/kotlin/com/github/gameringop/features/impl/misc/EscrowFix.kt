package com.github.gameringop.features.impl.misc

import com.github.gameringop.event.impl.ChatMessageEvent
import com.github.gameringop.features.Feature
import com.github.gameringop.utils.ChatUtils

object EscrowFix: Feature(name = "Escrow Fix", description = "Automatically reopens AH/BZ when closed by escrow.") {
    private val messageToCommand = mapOf(
        "There was an error with the auction house! (AUCTION_EXPIRED_OR_NOT_FOUND)" to "ah",
        "There was an error with the auction house! (INVALID_BID)" to "ah",
        "Claiming BIN auction..." to "ah",
        "Visit the Auction House to collect your item!" to "ah"
    )

    private val bazaarEscrowRegex = Regex("Escrow refunded (\\d+) coins for Bazaar Instant Buy Submit!")

    override fun init() {
        register<ChatMessageEvent> {
            val text = event.unformattedText
            val command = messageToCommand[text] ?: if (text.matches(bazaarEscrowRegex)) "bz" else null
            command?.let { ChatUtils.sendCommand(it) }
        }
    }
}