package com.github.gameringop.commands

import com.github.gameringop.SoTerm
import io.github.classgraph.ClassGraph
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback

object CommandManager {
    val commands = mutableSetOf<BaseCommand>()

    init {
        val scanResult = ClassGraph()
            .enableAllInfo()
            .acceptPackages("com.github.gameringop")
            .ignoreClassVisibility()
            .overrideClassLoaders(Thread.currentThread().contextClassLoader)
            .scan()

        scanResult.use { result ->
            val commandClasses = result.getSubclasses("com.github.gameringop.commands.BaseCommand")
            SoTerm.logger.info("CommandManager found ${commandClasses.size} commands.")

            commandClasses.forEach { classInfo ->
                try {
                    val instance = classInfo.loadClass().getDeclaredField("INSTANCE").get(null) as? BaseCommand

                    instance?.let { command ->
                        commands.add(command)
                        SoTerm.logger.info("Registered command: /${command.name}")
                    }
                } catch (e: Exception) {
                    SoTerm.logger.error("Failed to register command: ${classInfo.name}", e)
                }
            }
        }
    }

    fun registerAll() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            commands.forEach { command ->
                val root = ClientCommandManager.literal(command.name)
                CommandNodeBuilder(root).apply { with(command) { build() } }
                dispatcher.register(root)
                commands.add(command)
                SoTerm.logger.debug("Registered command: /${command.name}")
            }
        }
    }
}