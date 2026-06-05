package com.github.gameringop.utils.datafixer

import net.minecraft.resources.Identifier
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object MeowddingItemDfu : Logger by LoggerFactory.getLogger("MeowddingItemDfu") {
    var logErrors: Boolean = false

    fun load() = LegacyDataFixer
    internal fun id(path: String) = Identifier.fromNamespaceAndPath("meowdding", path)
}