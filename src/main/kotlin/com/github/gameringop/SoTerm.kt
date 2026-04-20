package com.github.gameringop

import com.github.gameringop.commands.CommandManager
import com.github.gameringop.config.PogObject
import com.github.gameringop.event.EventBus
import com.github.gameringop.event.EventDispatcher
import com.github.gameringop.event.impl.TickEvent
import com.github.gameringop.features.FeatureManager
import com.github.gameringop.utils.*
import com.github.gameringop.utils.ChatUtils.removeFormatting
import com.github.gameringop.utils.dungeons.DungeonListener
import com.github.gameringop.utils.items.ItemUtils.idToNameMap
import com.github.gameringop.utils.items.ItemUtils.nameToIdMap
import com.github.gameringop.utils.network.WebUtils
import com.github.gameringop.utils.network.data.ElectionData
import com.github.gameringop.utils.render.OPRenderPipelines
import com.github.gameringop.websocket.WebSocket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.*
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import org.slf4j.LoggerFactory

object SoTerm: ClientModInitializer {
    const val MOD_NAME = "SoTerm"
    const val MOD_ID = "SoTerm"
    const val PREFIX = "§8[§fTS§8]§r"
    val MOD_VERSION get() = FabricLoader.getInstance().getModContainer(MOD_ID).get().metadata.version.friendlyString

    @JvmField
    val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    @JvmField
    val mc = Minecraft.getInstance()

    @JvmField
    val logger = LoggerFactory.getLogger(MOD_NAME)

    @JvmField
    var isLoaded = false

    val cacheData = PogObject("cacheData", mutableMapOf<String, Any>())
    val debugFlags = mutableSetOf<String>()
    val isDev get() = debugFlags.contains("dev")

    var screen: Screen? = null

    var electionData = ElectionData.empty
    var priceData = mutableMapOf<String, Long>()


    override fun onInitializeClient() {
        DataDownloader.downloadData()

        OPRenderPipelines.init()
        EventDispatcher.init()
        DungeonListener.init()
        ServerUtils.init()
        ActionBarParser.init()
        PartyUtils.init()
        ChatUtils.init()
        TestGround()

        this.initNetworkLoop()

        FeatureManager.registerFeatures()
        CommandManager.registerAll()
        WebSocket.init()

        EventBus.register<TickEvent.Start> {
            mc.execute {
                if (screen == null) return@execute
                mc.setScreen(screen)
                screen = null
            }
        }

        isLoaded = true
    }

    private fun initNetworkLoop() = ThreadUtils.loop(600_000) {
        runCatching {
            val data = WebUtils.getAs<JsonObject>("https://api.hypixel.net/v2/resources/skyblock/election").getOrThrow()
            val mayor = data["mayor"]?.jsonObject !!
            val minister = mayor["minister"]?.jsonObject
            val perks = mayor["perks"]?.jsonArray
                ?.map { it.jsonObject["name"]?.jsonPrimitive?.content to it.jsonObject["description"]?.jsonPrimitive?.content?.removeFormatting() }
                ?.map { ElectionData.Perk(it.first !!, it.second !!) }
                ?: return@runCatching

            electionData = ElectionData(
                ElectionData.Mayor(
                    mayor["name"]?.jsonPrimitive?.content !!,
                    perks
                ),
                ElectionData.Minister(
                    minister?.get("name")?.jsonPrimitive?.content.orEmpty(),
                    ElectionData.Perk(minister?.get("perk")?.jsonObject["name"]?.jsonPrimitive?.content.orEmpty(), minister?.get("perk")?.jsonObject["description"]?.jsonPrimitive?.content?.removeFormatting().orEmpty())
                )
            )
        }
        .onFailure {
            logger.error("Error while making a web request", it)
            it.printStackTrace()
        }

        runCatching {
            priceData.putAll(WebUtils.getAs<Map<String, Double>>("https://lb.tricked.dev/lowestbins").getOrThrow().map {
                it.key to it.value.toLong()
            })
        }
        .onFailure {
            logger.error("Error while making a web request", it)
            it.printStackTrace()
        }

        runCatching {
            val data = WebUtils.getAs<JsonObject>("https://api.hypixel.net/v2/skyblock/bazaar").getOrThrow()
            data["products"]?.jsonObject?.forEach { (key, element) ->
                val product = element.jsonObject
                val productId = product["product_id"]?.jsonPrimitive?.content ?: key
                val buyPrice = product["buy_summary"]?.jsonArray?.getOrNull(0)
                    ?.jsonObject?.get("pricePerUnit")?.jsonPrimitive?.doubleOrNull?.toLong() ?: 0L

                priceData[productId] = buyPrice
            }
        }
        .onFailure {
            logger.error("Error while making a web request", it)
            it.printStackTrace()
        }

        runCatching {
            val data = WebUtils.getAs<JsonObject>("https://api.hypixel.net/v2/resources/skyblock/items").getOrThrow()
            val itemsArray = data["items"]?.jsonArray ?: return@runCatching
            for (element in itemsArray) {
                val item = element.jsonObject
                val id = item["id"]?.jsonPrimitive?.content ?: continue
                val name = item["name"]?.jsonPrimitive?.content ?: continue

                idToNameMap[id] = name
                nameToIdMap[name] = id
            }
        }
        .onFailure {
            logger.error("Error while making a web request", it)
            it.printStackTrace()
        }
    }
}