package com.github.gameringop.features

import com.github.gameringop.SoTerm
import com.github.gameringop.SoTerm.mc
import com.github.gameringop.config.Config
import com.github.gameringop.event.EventBus.register
import com.github.gameringop.event.impl.RenderOverlayEvent
import com.github.gameringop.ui.clickgui.CategoryType
import com.github.gameringop.ui.hud.HudEditorScreen
import com.github.gameringop.ui.hud.HudElement
import com.github.gameringop.ui.utils.Resolution
import com.github.gameringop.utils.render.Render2D.width
import io.github.classgraph.ClassGraph

object FeatureManager {
    val hudElements = mutableListOf<HudElement>()
    val features = mutableSetOf<Feature>()

    fun registerFeatures() {
        val scanResult = ClassGraph()
            .enableAllInfo()
            .acceptPackages("com.github.gameringop")
            .ignoreClassVisibility()
            .overrideClassLoaders(Thread.currentThread().contextClassLoader)
            .scan()

        scanResult.use { result ->
            val featureClasses = result.getSubclasses("com.github.gameringop.features.Feature")
            SoTerm.logger.debug("ClassGraph found ${featureClasses.size} subclasses of Feature")

            featureClasses.forEach { classInfo ->
                try {
                    val clazz = classInfo.loadClass()
                    val instance = clazz.getDeclaredField("INSTANCE").get(null) as? Feature

                    instance?.let { feature ->
                        feature.initialize()
                        hudElements.addAll(feature.hudElements)
                        features.add(feature)
                        SoTerm.logger.info("Successfully loaded feature: ${feature::class.simpleName}")
                    }
                }
                catch (e: Exception) {
                    SoTerm.logger.error("Failed to load feature class: ${classInfo.name}", e)
                }
            }
        }

        Config.load()

        register<RenderOverlayEvent> {
            if (mc.screen == HudEditorScreen) return@register
            Resolution.refresh()
            Resolution.push(event.context)
            hudElements.forEach { if (it.shouldDraw) it.renderElement(event.context, false) }
            Resolution.pop(event.context)
        }
    }

    fun getFeaturesByCategory(category: CategoryType): List<Feature> {
        return features.filter { it.category == category }
    }

    fun getFeatureByName(name: String): Feature? {
        return features.find { it.name == name }
    }

    fun getHudByName(name: String): HudElement? {
        return hudElements.find { it.name == name }
    }

    fun createFeatureList(): String {
        val featureList = StringBuilder()
        for ((category, features) in features.groupBy { it.category }.entries.sortedBy { it.key.ordinal }) {
            featureList.appendLine("Category: ${category.name}")
            for (feature in features.sortedByDescending { it.name.width() }) {
                featureList.appendLine("- ${feature.name}: ${feature.description ?: ""}")
            }
            featureList.appendLine()
        }
        return featureList.toString()
    }
}