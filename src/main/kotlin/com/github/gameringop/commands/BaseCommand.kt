package com.github.gameringop.commands

abstract class BaseCommand(val name: String) {
    abstract fun CommandNodeBuilder.build()
}