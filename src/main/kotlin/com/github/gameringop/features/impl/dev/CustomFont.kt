package com.github.gameringop.features.impl.dev

import com.github.gameringop.SoTerm
import com.github.gameringop.event.impl.TickEvent
import com.github.gameringop.features.Feature
import com.github.gameringop.mixin.MixinFontSetAccessor
import com.github.gameringop.ui.clickgui.components.impl.ButtonSetting
import com.github.gameringop.ui.clickgui.components.impl.SliderSetting
import com.github.gameringop.ui.clickgui.components.impl.TextInputSetting
import com.github.gameringop.ui.clickgui.components.impl.ToggleSetting
import com.github.gameringop.utils.ChatUtils
import com.mojang.blaze3d.font.GlyphProvider
import com.mojang.blaze3d.font.TrueTypeGlyphProvider
import com.mojang.blaze3d.font.UnbakedGlyph
import com.mojang.blaze3d.platform.TextureUtil
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import it.unimi.dsi.fastutil.ints.IntSet
import net.minecraft.client.gui.font.FontOption
import net.minecraft.client.gui.font.FontSet
import net.minecraft.client.gui.font.providers.FreeTypeUtil
import net.minecraft.resources.ResourceLocation
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import org.lwjgl.util.freetype.FT_Face
import org.lwjgl.util.freetype.FreeType
import java.awt.Desktop
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.util.*

object CustomFont : Feature(name = "Custom Font", description = "Replaces Minecraft text glyphs with selected TTF/OTF fonts.") {
    val modName = SoTerm.MOD_NAME.lowercase()
    private val fontsFolder = File("config/$modName/fonts")

    private val fontFile by TextInputSetting("Font File", "")
        .section("Main Font")
    private val fontSize by SliderSetting("Font Size", 11.0, 6.0, 32.0, 0.5)

    private val bracketFontFile by TextInputSetting("Bracket Font File", "")
        .section("Brackets")
        .withDescription("Leave empty to use the main font file.")
    private val bracketFontSize by SliderSetting("Bracket Size", 11.0, 6.0, 32.0, 0.5)
    private val bracketYOffset by SliderSetting("Bracket Y-Offset", 0.0, -10.0, 10.0, 0.05)

    private val descenderYOffset by SliderSetting("Descender Y-Offset (g,j,y)", 0.0, -10.0, 10.0, 0.05)
        .section("Special Letters")
        .withDescription("Moves g, j, and y vertically without changing their size.")

    private val originalUnderscore by ToggleSetting("Original Underscore", false)
        .withDescription("If enabled, uses the standard Minecraft underscore instead of the custom font's.")

    private val oversample by SliderSetting("Oversample", 2.0, 1.0, 16.0, 0.5)
        .section("Global Rendering")
        .withDescription("Affects ALL characters. Higher = smoother, but uses more GPU memory.")

    private val reloadFontButton by ButtonSetting("Reload Font") { reloadMinecraftFonts() }
    private val openFontsFolderButton by ButtonSetting("Open Fonts Folder") { openFontsFolder() }

    @Volatile private var customProvider: GlyphProvider? = null
    @Volatile private var lastLoadedKey: FontKey? = null
    @Volatile private var pendingReload = false

    override fun init() {
        register<TickEvent.Start> {
            if (!pendingReload) return@register
            if (mc.level == null && mc.screen == null) return@register
            pendingReload = false
            reloadMinecraftFonts()
        }
    }

    @JvmStatic
    fun applyToFontSets(fontSets: Map<ResourceLocation, FontSet>) {
        if (!enabled) {
            closeCustomProvider()
            return
        }

        val key = currentFontKey() ?: return
        val provider = getOrLoadProvider(key) ?: return
        val customConditional = GlyphProvider.Conditional(provider, FontOption.Filter.ALWAYS_PASS)

        fontSets.values.forEach { fontSet ->
            val accessor = fontSet as MixinFontSetAccessor
            val originalProviders = accessor.`soterm$getAllProviders`()
                .filterNot { it.provider() === provider }

            fontSet.reload(listOf(customConditional) + originalProviders, Collections.emptySet())
        }
    }

    private fun currentFontKey(): FontKey? {
        val file = resolveFontFile(fontFile.value.trim()) ?: return null
        if (!file.isFile) return null

        val bFileRaw = bracketFontFile.value.trim()
        val bFile = if (bFileRaw.isNotBlank()) resolveFontFile(bFileRaw) else file

        return FontKey(
            file.absoluteFile.normalize(),
            fontSize.value.toFloat(),
            oversample.value.toFloat(),
            bFile?.absoluteFile?.normalize() ?: file,
            bracketFontSize.value.toFloat(),
            bracketYOffset.value.toFloat(),
            descenderYOffset.value.toFloat(),
            originalUnderscore.value
        )
    }

    private fun resolveFontFile(raw: String): File? {
        if (raw.isBlank()) return null
        val file = File(raw)
        val resolved = if (file.isAbsolute) file else File(fontsFolder, raw)
        return if (resolved.extension.lowercase() in listOf("ttf", "otf")) resolved else null
    }

    @Synchronized
    private fun getOrLoadProvider(key: FontKey): GlyphProvider? {
        if (customProvider != null && lastLoadedKey == key) return customProvider

        closeCustomProvider()
        return runCatching {
            val mainP = loadTrueTypeProvider(key.file, key.size, key.oversample, 0.0f)

            val bracketP = if (key.file != key.bracketFile || key.size != key.bracketSize || key.bracketYOffset != 0.0f) {
                loadTrueTypeProvider(key.bracketFile, key.bracketSize, key.oversample, key.bracketYOffset)
            } else {
                mainP
            }

            val descenderP = if (key.descenderYOffset != 0.0f) {
                loadTrueTypeProvider(key.file, key.size, key.oversample, key.descenderYOffset)
            } else {
                mainP
            }

            MultiAwareGlyphProvider(mainP, bracketP, descenderP, key.originalUnderscore).also {
                customProvider = it
                lastLoadedKey = key
                ChatUtils.modMessage("&aFont Loaded!")
            }
        }.onFailure {
            ChatUtils.modMessage("&cLoad failed: ${it.message}")
        }.getOrNull()
    }

    private fun loadTrueTypeProvider(file: File, size: Float, oversample: Float, shiftY: Float): TrueTypeGlyphProvider {
        val input = FileInputStream(file)
        val fontBuffer: ByteBuffer = input.use { TextureUtil.readResource(it) }.also { it.flip() }
        var face: FT_Face? = null
        try {
            synchronized(FreeTypeUtil.LIBRARY_LOCK) {
                MemoryStack.stackPush().use { stack ->
                    val pointer = stack.mallocPointer(1)
                    FreeTypeUtil.assertError(FreeType.FT_New_Memory_Face(FreeTypeUtil.getLibrary(), fontBuffer, 0L, pointer), "Loading Face")
                    face = FT_Face.create(pointer.get())
                }
                val loadedFace = face ?: throw IllegalStateException("Face null")
                FreeTypeUtil.assertError(FreeType.FT_Select_Charmap(loadedFace, FreeType.FT_ENCODING_UNICODE), "Charmap")

                return TrueTypeGlyphProvider(fontBuffer, loadedFace, size, oversample, 0.0f, -shiftY, "")
            }
        } catch (t: Throwable) {
            synchronized(FreeTypeUtil.LIBRARY_LOCK) { face?.let { FreeType.FT_Done_Face(it) } }
            MemoryUtil.memFree(fontBuffer)
            throw t
        }
    }

    @Synchronized
    private fun closeCustomProvider() {
        customProvider?.close()
        customProvider = null
        lastLoadedKey = null
    }

    private fun reloadMinecraftFonts() {
        fontsFolder.mkdirs()
        mc.execute { mc.reloadResourcePacks() }
    }

    private fun openFontsFolder() {
        fontsFolder.mkdirs()
        Desktop.getDesktop().open(fontsFolder)
    }

    private data class FontKey(
        val file: File,
        val size: Float,
        val oversample: Float,
        val bracketFile: File,
        val bracketSize: Float,
        val bracketYOffset: Float,
        val descenderYOffset: Float,
        val originalUnderscore: Boolean
    )

    private class MultiAwareGlyphProvider(
        private val mainProvider: GlyphProvider,
        private val bracketProvider: GlyphProvider,
        private val descenderProvider: GlyphProvider,
        private val useOriginalUnderscore: Boolean
    ) : GlyphProvider {
        private val brackets: IntSet = IntOpenHashSet(charArrayOf('(', ')', '[', ']', '{', '}').map { it.code })
        private val descenders: IntSet = IntOpenHashSet(charArrayOf('g', 'j', 'y').map { it.code })

        override fun getSupportedGlyphs(): IntSet {
            val set = IntOpenHashSet()
            set.addAll(mainProvider.supportedGlyphs)
            set.addAll(bracketProvider.supportedGlyphs)
            set.addAll(descenderProvider.supportedGlyphs)

            if (useOriginalUnderscore) {
                set.remove('_'.code)
            }
            return set
        }

        override fun getGlyph(codePoint: Int): UnbakedGlyph? {
            if (useOriginalUnderscore && codePoint == '_'.code) return null

            return when (codePoint) {
                in brackets -> bracketProvider.getGlyph(codePoint)
                in descenders -> descenderProvider.getGlyph(codePoint)
                else -> mainProvider.getGlyph(codePoint)
            }
        }

        override fun close() {
            val closed = mutableSetOf<GlyphProvider>()
            listOf(mainProvider, bracketProvider, descenderProvider).forEach {
                if (closed.add(it)) it.close()
            }
        }
    }
}