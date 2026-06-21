package com.github.gameringop

import com.github.gameringop.commands.CommandManager
import com.github.gameringop.config.PogObject
import com.github.gameringop.event.EventDispatcher
import com.github.gameringop.features.FeatureManager
import com.github.gameringop.init.NetworkLoop
import com.github.gameringop.utils.*
import com.github.gameringop.utils.dungeons.DungeonListener
import com.github.gameringop.utils.render.OPRenderPipelines
import com.github.gameringop.websocket.WebSocket
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import org.slf4j.LoggerFactory

object SoTerm: ClientModInitializer {
    const val MOD_ID = "soterm"
    val MOD_NAME by lazy { FabricLoader.getInstance().getModContainer(MOD_ID).get().metadata.name }
    val MOD_VERSION by lazy { FabricLoader.getInstance().getModContainer(MOD_ID).get().metadata.version.friendlyString }
    const val PREFIX = "§8[§fTS§8]§r"

    @JvmField
    val scope = CoroutineScope(Dispatchers.Default + SupervisorJob() + CoroutineName(MOD_NAME))

    @JvmField
    val mc = Minecraft.getInstance()

    @JvmField
    val logger = LoggerFactory.getLogger(MOD_NAME)

    @JvmField
    var isLoaded = false

    val cacheData = PogObject("cacheData", mutableMapOf<String, String>())
    val debugFlags = mutableSetOf<String>()
    val isDev get() = debugFlags.contains("dev")

    var screen: Screen? = null
        set(value) {
            field = value
            if (value == null) return
            ThreadUtils.scheduledTask(1) {
                mc.setScreen(screen)
                field = null
            }
        }

    override fun onInitializeClient() {
        OPRenderPipelines.init()
        EventDispatcher.init()
        DungeonListener.init()
        ServerUtils.init()
        ActionBarParser.init()
        PartyUtils.init()
        ChatUtils.init()
        SlayerUtils.init()
        TestGround()

        NetworkLoop.init()

        FeatureManager.registerFeatures()
        CommandManager.registerAll()
        WebSocket.init()

        isLoaded = true
    }
}