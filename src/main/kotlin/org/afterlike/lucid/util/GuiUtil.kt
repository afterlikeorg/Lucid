package org.afterlike.lucid.util

import net.minecraft.client.gui.FontRenderer
import java.awt.Color

object GuiUtil {
    
    fun getMinecraftColor(code: String): Color {
        return when (code) {
            "0" -> Color(0, 0, 0)
            "1" -> Color(0, 0, 170)
            "2" -> Color(0, 170, 0)
            "3" -> Color(0, 170, 170)
            "4" -> Color(170, 0, 0)
            "5" -> Color(170, 0, 170)
            "6" -> Color(255, 170, 0)
            "7" -> Color(170, 170, 170)
            "8" -> Color(85, 85, 85)
            "9" -> Color(85, 85, 255)
            "a" -> Color(85, 255, 85)
            "b" -> Color(85, 255, 255)
            "c" -> Color(255, 85, 85)
            "d" -> Color(255, 85, 255)
            "e" -> Color(255, 255, 85)
            "f" -> Color(255, 255, 255)
            else -> Color(0, 170, 170)
        }
    }
    
    fun tintColor(baseColor: Color, themeColor: Color, intensity: Float = 0.3f): Color {
        val r = (baseColor.red * (1 - intensity) + themeColor.red * intensity).toInt().coerceIn(0, 255)
        val g = (baseColor.green * (1 - intensity) + themeColor.green * intensity).toInt().coerceIn(0, 255)
        val b = (baseColor.blue * (1 - intensity) + themeColor.blue * intensity).toInt().coerceIn(0, 255)
        val a = baseColor.alpha
        return Color(r, g, b, a)
    }
    
    fun wrapText(text: String, maxWidth: Int, fontRenderer: FontRenderer): List<String> {
        val words = text.split(" ")
        val result = mutableListOf<String>()
        var currentLine = ""

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            val width = fontRenderer.getStringWidth(testLine)

            if (width <= maxWidth - 10) {
                currentLine = testLine
            } else {
                result.add(currentLine)
                currentLine = word
            }
        }

        if (currentLine.isNotEmpty()) {
            result.add(currentLine)
        }

        return result
    }
    
    val colorCodes = listOf(
        "0" to "§0Black",
        "1" to "§1Dark Blue",
        "2" to "§2Dark Green",
        "3" to "§3Cyan",
        "4" to "§4Dark Red",
        "5" to "§5Purple",
        "6" to "§6Gold",
        "7" to "§7Gray",
        "8" to "§8Dark Gray",
        "9" to "§9Blue",
        "a" to "§aGreen",
        "b" to "§bAqua",
        "c" to "§cRed",
        "d" to "§dPink",
        "e" to "§eYellow",
        "f" to "§fWhite"
    )
}
