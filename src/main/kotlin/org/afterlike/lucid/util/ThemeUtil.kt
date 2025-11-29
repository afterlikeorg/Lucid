package org.afterlike.lucid.util

import java.awt.Color

class ThemeUtil(colorCode: String) {

    private val themeColor = GuiUtil.getMinecraftColor(colorCode)

    private val baseBackgroundColor = Color(25, 25, 25, 245)
    private val basePanelColor = Color(35, 35, 35, 255)
    private val baseAccentColor = Color(70, 70, 70)
    private val baseAccentColorDisabled = Color(50, 50, 50)
    private val baseButtonColor = Color(45, 45, 45, 255)
    private val baseButtonHoverColor = Color(55, 55, 55, 255)
    private val baseActiveTabColor = Color(60, 60, 60, 255)
    private val baseTextColor = Color(240, 240, 240, 255)
    private val baseTextSecondaryColor = Color(180, 180, 180, 255)
    private val baseBorderColor = Color(65, 65, 65, 255)
    private val baseHeaderColor = Color(28, 28, 28, 255)
    private val basePreviewBoxColor = Color(20, 20, 20)
    private val baseScrollTrackColor = Color(40, 40, 40, 255)
    private val baseScrollThumbColor = Color(80, 80, 80, 255)

    val backgroundColor = GuiUtil.tintColor(baseBackgroundColor, themeColor, 0.05f)
    val panelColor = GuiUtil.tintColor(basePanelColor, themeColor, 0.05f)
    val accentColor = GuiUtil.tintColor(baseAccentColor, themeColor, 0.5f)
    val accentColorDisabled = GuiUtil.tintColor(baseAccentColorDisabled, themeColor, 0.2f)
    val buttonColor = GuiUtil.tintColor(baseButtonColor, themeColor, 0.08f)
    val buttonHoverColor = GuiUtil.tintColor(baseButtonHoverColor, themeColor, 0.15f)
    val activeTabColor = GuiUtil.tintColor(baseActiveTabColor, themeColor, 0.12f)
    val textColor = baseTextColor
    val textSecondaryColor = baseTextSecondaryColor
    val borderColor = GuiUtil.tintColor(baseBorderColor, themeColor, 0.15f)
    val headerColor = GuiUtil.tintColor(baseHeaderColor, themeColor, 0.05f)
    val previewBoxColor = GuiUtil.tintColor(basePreviewBoxColor, themeColor, 0.05f)
    val scrollTrackColor = GuiUtil.tintColor(baseScrollTrackColor, themeColor, 0.05f)
    val scrollThumbColor = GuiUtil.tintColor(baseScrollThumbColor, themeColor, 0.2f)
}