package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.editor.CodeSyntaxPalette

// Core Liquid Glass Palette - Dark
val ObsidianNight = Color(0xFF060B16)
val DeepSapphire = Color(0xFF0D172B)
val GlassDarkSurface = Color(0xCC111C33)
val GlassDarkSurfaceElevated = Color(0xE0162442)
val GlassDarkBorderTop = Color(0x4DFFFFFF)
val GlassDarkBorderBottom = Color(0x1494A3B8)

val ElectricCyan = Color(0xFF38BDF8)
val AquaGlow = Color(0xFF22D3EE)
val AmethystViolet = Color(0xFFA78BFA)
val EmeraldMint = Color(0xFF34D399)
val CoralRose = Color(0xFFFB7185)
val AmberGold = Color(0xFFFBBF24)

// Core Liquid Glass Palette - Light
val FrostMist = Color(0xFFEDF3FA)
val CloudCanvas = Color(0xFFF6F9FE)
val GlassLightSurface = Color(0xD9FFFFFF)
val GlassLightSurfaceElevated = Color(0xF0FFFFFF)
val GlassLightBorderTop = Color(0xFFFFFFFF)
val GlassLightBorderBottom = Color(0x3364748B)

val CobaltBlue = Color(0xFF0284C7)
val DeepIndigo = Color(0xFF6D28D9)
val TealDeep = Color(0xFF0D9488)
val InkSlate = Color(0xFF0F172A)
val MutedSlate = Color(0xFF475569)

// File-type badge accents
val HtmlOrange = Color(0xFFF97316)
val CssBlue = Color(0xFF38BDF8)
val JsYellow = Color(0xFFFACC15)
val JsonGreen = Color(0xFF4ADE80)
val SvgPink = Color(0xFFF472B6)
val MdPurple = Color(0xFFA78BFA)

val DarkSyntaxPalette = CodeSyntaxPalette(
    plain = Color(0xFFE2E8F0),
    keyword = Color(0xFFC084FC),
    tag = Color(0xFF38BDF8),
    attribute = Color(0xFF7DD3FC),
    string = Color(0xFF6EE7B7),
    number = Color(0xFFFBBF24),
    comment = Color(0xFF64748B),
    functionName = Color(0xFF93C5FD),
    punctuation = Color(0xFF94A3B8),
    findHighlightBg = Color(0x55FBBF24),
    activeFindHighlightBg = Color(0xAAFB923C),
    bracketMatchBg = Color(0x5538BDF8)
)

val LightSyntaxPalette = CodeSyntaxPalette(
    plain = Color(0xFF1E293B),
    keyword = Color(0xFF7C3AED),
    tag = Color(0xFF0369A1),
    attribute = Color(0xFF0284C7),
    string = Color(0xFF047857),
    number = Color(0xFFB45309),
    comment = Color(0xFF64748B),
    functionName = Color(0xFF2563EB),
    punctuation = Color(0xFF475569),
    findHighlightBg = Color(0x55FDE047),
    activeFindHighlightBg = Color(0xAAF59E0B),
    bracketMatchBg = Color(0x440284C7)
)
