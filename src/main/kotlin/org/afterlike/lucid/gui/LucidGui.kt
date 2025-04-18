package org.afterlike.lucid.gui

import net.minecraft.client.Minecraft
import net.minecraft.client.audio.PositionedSoundRecord
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.ChatComponentText
import net.minecraft.util.ResourceLocation
import org.afterlike.lucid.check.Check
import org.afterlike.lucid.check.CheckManager
import org.afterlike.lucid.util.Config
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.io.IOException
import kotlin.math.max
import kotlin.math.min

class LucidGui : GuiScreen() {

    private fun getThemeColor(): Color {
        return when (Config.messageColor) {
            "0" -> Color(0, 0, 0) // Black
            "1" -> Color(0, 0, 170) // Dark Blue
            "2" -> Color(0, 170, 0) // Dark Green
            "3" -> Color(0, 170, 170) // Cyan
            "4" -> Color(170, 0, 0) // Dark Red
            "5" -> Color(170, 0, 170) // Purple
            "6" -> Color(255, 170, 0) // Gold
            "7" -> Color(170, 170, 170) // Gray
            "8" -> Color(85, 85, 85) // Dark Gray
            "9" -> Color(85, 85, 255) // Blue
            "a" -> Color(85, 255, 85) // Green
            "b" -> Color(85, 255, 255) // Aqua
            "c" -> Color(255, 85, 85) // Red
            "d" -> Color(255, 85, 255) // Pink
            "e" -> Color(255, 255, 85) // Yellow
            "f" -> Color(255, 255, 255) // White
            else -> Color(0, 170, 170) // Default Cyan
        }
    }

    private fun tintColor(baseColor: Color, intensity: Float = 0.3f): Color {
        val themeColor = getThemeColor()

        val r = (baseColor.red * (1 - intensity) + themeColor.red * intensity).toInt().coerceIn(0, 255)
        val g = (baseColor.green * (1 - intensity) + themeColor.green * intensity).toInt().coerceIn(0, 255)
        val b = (baseColor.blue * (1 - intensity) + themeColor.blue * intensity).toInt().coerceIn(0, 255)
        val a = baseColor.alpha

        return Color(r, g, b, a)
    }

    private val baseBackgroundColor = Color(25, 25, 25, 245)
    private val basePanelColor = Color(35, 35, 35, 255)
    private val baseAccentColor = Color(70, 70, 70)
    private val baseAccentColorHover = Color(90, 90, 90)
    private val baseAccentColorDisabled = Color(50, 50, 50)
    private val baseButtonColor = Color(45, 45, 45, 255)
    private val baseButtonHoverColor = Color(55, 55, 55, 255)
    private val baseActiveTabColor = Color(60, 60, 60, 255)
    private val baseTextColor = Color(240, 240, 240, 255)
    private val baseTextSecondaryColor = Color(180, 180, 180, 255)
    private val baseBorderColor = Color(65, 65, 65, 255)
    private val baseHeaderColor = Color(28, 28, 28, 255)
    private val baseTitleColor = Color(180, 180, 180, 255)
    private val basePreviewBoxColor = Color(20, 20, 20)
    private val baseScrollTrackColor = Color(40, 40, 40, 255)
    private val baseScrollThumbColor = Color(80, 80, 80, 255)

    private var backgroundColor = baseBackgroundColor
    private var panelColor = basePanelColor
    private var accentColor = baseAccentColor
    private var accentColorHover = baseAccentColorHover
    private var accentColorDisabled = baseAccentColorDisabled
    private var buttonColor = baseButtonColor
    private var buttonHoverColor = baseButtonHoverColor
    private var activeTabColor = baseActiveTabColor
    private var textColor = baseTextColor
    private var textSecondaryColor = baseTextSecondaryColor
    private var borderColor = baseBorderColor
    private var headerColor = baseHeaderColor
    private var titleColor = baseTitleColor
    private var previewBoxColor = basePreviewBoxColor
    private var scrollTrackColor = baseScrollTrackColor
    private var scrollThumbColor = baseScrollThumbColor

    private fun updateColors() {
        backgroundColor = tintColor(baseBackgroundColor, 0.05f)
        panelColor = tintColor(basePanelColor, 0.05f)

        accentColor = tintColor(baseAccentColor, 0.5f)
        accentColorHover = tintColor(baseAccentColorHover, 0.5f)
        accentColorDisabled = tintColor(baseAccentColorDisabled, 0.2f)
        buttonColor = tintColor(baseButtonColor, 0.08f)
        buttonHoverColor = tintColor(baseButtonHoverColor, 0.15f)
        activeTabColor = tintColor(baseActiveTabColor, 0.12f)
        textColor = baseTextColor
        textSecondaryColor = baseTextSecondaryColor
        borderColor = tintColor(baseBorderColor, 0.15f)
        headerColor = tintColor(baseHeaderColor, 0.05f)
        titleColor = tintColor(baseTitleColor, 0.4f)
        previewBoxColor = tintColor(basePreviewBoxColor, 0.05f)
        scrollTrackColor = tintColor(baseScrollTrackColor, 0.05f)
        scrollThumbColor = tintColor(baseScrollThumbColor, 0.2f)
    }

    private enum class Tab { CHECKS, APPEARANCE, SETTINGS }

    private var currentTab = Tab.CHECKS

    private enum class ButtonAction {
        TOGGLE, DEC, INC,
        TOGGLE_SOUND, TOGGLE_VERBOSE,
        TAB_CHECKS, TAB_APPEARANCE, TAB_SETTINGS,
        CLOSE,
        COOLDOWN_DEC, COOLDOWN_INC,
        COLOR_PREV, COLOR_NEXT,
        TOGGLE_BOLD,
        SYMBOL_TOGGLE,
        TOGGLE_SHOW_VL,
        TOGGLE_SHOW_WDR
    }

    private val panelWidth = 300
    private val panelHeight = 330
    private val headerHeight = 30
    private val tabHeight = 24
    private val buttonHeight = 22
    private val buttonWidth = 22
    private val contentPadding = 15
    private val buttonSpacing = 4
    private val descriptionPadding = 4
    private val scrollbarWidth = 4
    private val scrollbarPadding = 5
    private val closeButtonSize = 16
    private val bottomPadding = 15

    private var scrollOffset = 0
    private var maxScrollOffset = 0
    private var isScrolling = false
    private var contentHeight = 0
    private var hasScrollbar = false

    private val buttonSound = ResourceLocation("gui.button.press")

    private val colorCodes = listOf(
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

    private class ModernButton(
        val id: Int,
        val x: Int,
        var y: Int,
        val width: Int,
        val height: Int,
        val text: String,
        val action: ButtonAction,
        val check: Check? = null,
        val enabled: Boolean = true,
        val description: String? = null,
        val isTab: Boolean = false,
        val isActive: Boolean = false,
        val isClose: Boolean = false
    ) {
        var isHovered = false
        var visualY: Int = y

        fun isMouseOver(mouseX: Int, mouseY: Int, scrollOffset: Int = 0): Boolean {
            val effectiveY = if (id >= 100 && !isClose) y - scrollOffset else y
            return enabled && mouseX >= x && mouseX <= x + width &&
                    mouseY >= effectiveY && mouseY <= effectiveY + height
        }

        fun draw(gui: LucidGui) {
            val color = when {
                isClose -> Color(0, 0, 0, 0)
                isTab && isActive -> gui.activeTabColor
                !enabled -> gui.accentColorDisabled
                isHovered -> gui.buttonHoverColor
                else -> gui.buttonColor
            }

            val textColor = if (!enabled) Color(190, 190, 190) else gui.textColor
            val borderColor = if (isHovered && enabled) gui.accentColor else gui.borderColor

            if (!isClose) {

                Gui.drawRect(x, visualY, x + width, visualY + height, color.rgb)


                if (!isTab || (isTab && isActive)) {
                    Gui.drawRect(x, visualY, x + width, visualY + 1, borderColor.rgb)
                    Gui.drawRect(x + width - 1, visualY, x + width, visualY + height, borderColor.rgb)
                    Gui.drawRect(x, visualY + height - 1, x + width, visualY + height, borderColor.rgb)
                    Gui.drawRect(x, visualY, x + 1, visualY + height, borderColor.rgb)
                } else if (isTab) {

                    Gui.drawRect(x, visualY + height - 1, x + width, visualY + height, borderColor.rgb)
                }
            } else {

                val xColor = if (isHovered) Color(255, 100, 100).rgb else Color(200, 200, 200).rgb
                gui.drawCenteredString(gui.fontRendererObj, "✕", x + width / 2, visualY + (height - 8) / 2, xColor)
            }


            if (!isClose) {
                val mc = Minecraft.getMinecraft()
                val textWidth = mc.fontRendererObj.getStringWidth(text)
                mc.fontRendererObj.drawStringWithShadow(
                    text,
                    (x + (width - textWidth) / 2).toFloat(),
                    (visualY + (height - 8) / 2).toFloat(),
                    textColor.rgb
                )
            }
        }
    }

    private val buttons = mutableListOf<ModernButton>()

    private fun getContentWidth(): Int {
        return panelWidth - 2 * contentPadding - (if (hasScrollbar) scrollbarWidth + scrollbarPadding * 2 else 0)
    }

    private fun getButtonWidth(fullWidth: Boolean = true): Int {
        val effectiveWidth = getContentWidth()

        return if (fullWidth) {
            effectiveWidth
        } else {
            effectiveWidth - (2 * buttonWidth + 2 * buttonSpacing)
        }
    }

    override fun initGui() {
        buttons.clear()
        super.initGui()

        updateColors()

        val centerX = width / 2
        val panelLeft = centerX - panelWidth / 2
        val panelTop = height / 2 - panelHeight / 2


        buttons.add(
            ModernButton(
                0, panelLeft + panelWidth - closeButtonSize - 6, panelTop + (headerHeight - closeButtonSize) / 2,
                closeButtonSize, closeButtonSize, "X", ButtonAction.CLOSE,
                isClose = true
            )
        )

        val tabWidth = panelWidth / 3
        buttons.add(
            ModernButton(
                1, panelLeft, panelTop + headerHeight,
                tabWidth, tabHeight, "Checks",
                ButtonAction.TAB_CHECKS,
                enabled = currentTab != Tab.CHECKS,
                isTab = true,
                isActive = currentTab == Tab.CHECKS
            )
        )

        buttons.add(
            ModernButton(
                2, panelLeft + tabWidth, panelTop + headerHeight,
                tabWidth, tabHeight, "Appearance",
                ButtonAction.TAB_APPEARANCE,
                enabled = currentTab != Tab.APPEARANCE,
                isTab = true,
                isActive = currentTab == Tab.APPEARANCE
            )
        )

        buttons.add(
            ModernButton(
                3, panelLeft + tabWidth * 2, panelTop + headerHeight,
                tabWidth, tabHeight, "Settings",
                ButtonAction.TAB_SETTINGS,
                enabled = currentTab != Tab.SETTINGS,
                isTab = true,
                isActive = currentTab == Tab.SETTINGS
            )
        )

        when (currentTab) {
            Tab.CHECKS -> initChecksTab(panelLeft, panelTop, false)
            Tab.APPEARANCE -> initAppearanceTab(panelLeft, panelTop, false)
            Tab.SETTINGS -> initSettingsTab(panelLeft, panelTop, false)
        }

        val contentAreaHeight = panelHeight - headerHeight - tabHeight - contentPadding
        hasScrollbar = contentHeight > contentAreaHeight

        buttons.removeAll { it.id >= 100 }
        when (currentTab) {
            Tab.CHECKS -> initChecksTab(panelLeft, panelTop, hasScrollbar)
            Tab.APPEARANCE -> initAppearanceTab(panelLeft, panelTop, hasScrollbar)
            Tab.SETTINGS -> initSettingsTab(panelLeft, panelTop, hasScrollbar)
        }

        maxScrollOffset = max(0, contentHeight - contentAreaHeight)
        scrollOffset = min(scrollOffset, maxScrollOffset)
    }

    private fun initChecksTab(panelLeft: Int, panelTop: Int, withScrollbar: Boolean) {
        val contentLeft = panelLeft + contentPadding
        val contentStartY = panelTop + headerHeight + tabHeight
        var y = contentStartY + contentPadding

        contentHeight = contentPadding

        val toggleWidth = getButtonWidth(false)

        CheckManager.allChecks().forEachIndexed { index, check ->
            val id = 100 + index * 3

            val statusText = if (check.enabled) "§aON§r" else "§cOFF§r"
            val toggleLabel = "${check.name}: $statusText [VL:${check.vlThreshold}]"
            buttons.add(
                ModernButton(
                    id, contentLeft, y, toggleWidth, buttonHeight,
                    toggleLabel, ButtonAction.TOGGLE, check, description = check.description
                )
            )


            buttons.add(
                ModernButton(
                    id + 1, contentLeft + toggleWidth + buttonSpacing, y,
                    buttonWidth, buttonHeight, "-", ButtonAction.DEC, check
                )
            )


            buttons.add(
                ModernButton(
                    id + 2, contentLeft + toggleWidth + buttonWidth + buttonSpacing * 2, y,
                    buttonWidth, buttonHeight, "+", ButtonAction.INC, check
                )
            )

            val descLines = wrapText(check.description, toggleWidth).size
            val descriptionHeight = descLines * 10 + descriptionPadding

            y += buttonHeight + descriptionHeight + buttonSpacing * 2
            contentHeight += buttonHeight + descriptionHeight + buttonSpacing * 2
        }


        contentHeight += contentPadding + bottomPadding
    }

    private fun initAppearanceTab(panelLeft: Int, panelTop: Int, withScrollbar: Boolean) {
        val contentLeft = panelLeft + contentPadding
        val contentStartY = panelTop + headerHeight + tabHeight
        var y = contentStartY + contentPadding

        val fullButtonWidth = getButtonWidth(true)
        val settingWidth = getButtonWidth(false)

        contentHeight = contentPadding

        val currentColorName = colorCodes.find { it.first == Config.messageColor }?.second ?: "§3Cyan"
        val colorLabel = "Message Color: $currentColorName"

        buttons.add(
            ModernButton(
                100, contentLeft, y, settingWidth, buttonHeight,
                colorLabel, ButtonAction.TOGGLE_SOUND,
                description = "The color used for Lucid in chat messages"
            )
        )

        buttons.add(
            ModernButton(
                101, contentLeft + settingWidth + buttonSpacing, y,
                buttonWidth, buttonHeight, "◀", ButtonAction.COLOR_PREV
            )
        )

        buttons.add(
            ModernButton(
                102, contentLeft + settingWidth + buttonWidth + buttonSpacing * 2, y,
                buttonWidth, buttonHeight, "▶", ButtonAction.COLOR_NEXT
            )
        )

        val colorDescLines = wrapText(
            "The color used for Lucid in chat messages",
            fullButtonWidth
        ).size
        val colorDescHeight = colorDescLines * 10 + descriptionPadding

        y += buttonHeight + colorDescHeight + buttonSpacing * 2
        contentHeight += buttonHeight + colorDescHeight + buttonSpacing * 2

        val boldStatus = if (Config.messageBold) "§aON§r" else "§cOFF§r"
        val boldLabel = "Bold Text: $boldStatus"

        buttons.add(
            ModernButton(
                103, contentLeft, y, fullButtonWidth, buttonHeight,
                boldLabel, ButtonAction.TOGGLE_BOLD,
                description = "Make Lucid text bold in chat messages"
            )
        )

        val boldDescLines = wrapText(
            "Make Lucid text bold in chat messages",
            fullButtonWidth
        ).size
        val boldDescHeight = boldDescLines * 10 + descriptionPadding

        y += buttonHeight + boldDescHeight + buttonSpacing * 2
        contentHeight += buttonHeight + boldDescHeight + buttonSpacing * 2

        val symbolLabel = "Symbol: ${Config.messageSymbol}"

        buttons.add(
            ModernButton(
                104, contentLeft, y, fullButtonWidth, buttonHeight,
                symbolLabel, ButtonAction.SYMBOL_TOGGLE,
                description = "Toggle between > and » symbols"
            )
        )

        val symbolDescLines = wrapText(
            "Toggle between > and » symbols",
            fullButtonWidth
        ).size
        val symbolDescHeight = symbolDescLines * 10 + descriptionPadding

        y += buttonHeight + symbolDescHeight + buttonSpacing * 2
        contentHeight += buttonHeight + symbolDescHeight + buttonSpacing * 2

        val vlStatus = if (Config.showVLInFlag) "§aON§r" else "§cOFF§r"
        val vlLabel = "Show VL in Flag: $vlStatus"

        buttons.add(
            ModernButton(
                105, contentLeft, y, fullButtonWidth, buttonHeight,
                vlLabel, ButtonAction.TOGGLE_SHOW_VL,
                description = "Show violation level in flag messages"
            )
        )

        val vlDescLines = wrapText(
            "Show violation level in flag messages",
            fullButtonWidth
        ).size
        val vlDescHeight = vlDescLines * 10 + descriptionPadding

        y += buttonHeight + vlDescHeight + buttonSpacing * 2
        contentHeight += buttonHeight + vlDescHeight + buttonSpacing * 2

        val wdrStatus = if (Config.showWDR) "§aON§r" else "§cOFF§r"
        val wdrLabel = "Show WDR Button: $wdrStatus"

        buttons.add(
            ModernButton(
                106, contentLeft, y, fullButtonWidth, buttonHeight,
                wdrLabel, ButtonAction.TOGGLE_SHOW_WDR,
                description = "Show the WDR (Watchdog Report) button in flag messages"
            )
        )

        val wdrDescLines = wrapText(
            "Show the WDR (Watchdog Report) button in flag messages",
            fullButtonWidth
        ).size
        val wdrDescHeight = wdrDescLines * 10 + descriptionPadding

        y += buttonHeight + wdrDescHeight + buttonSpacing * 2
        contentHeight += buttonHeight + wdrDescHeight + buttonSpacing * 2 + bottomPadding
    }

    private fun initSettingsTab(panelLeft: Int, panelTop: Int, withScrollbar: Boolean) {
        val contentLeft = panelLeft + contentPadding
        val contentStartY = panelTop + headerHeight + tabHeight
        var y = contentStartY + contentPadding

        val fullButtonWidth = getButtonWidth(true)
        val settingWidth = getButtonWidth(false)

        contentHeight = contentPadding

        val soundStatus = if (Config.playSoundOnFlag) "§aON§r" else "§cOFF§r"
        val soundLabel = "Play Sound on Flag: $soundStatus"
        buttons.add(
            ModernButton(
                100, contentLeft, y, fullButtonWidth, buttonHeight,
                soundLabel, ButtonAction.TOGGLE_SOUND,
                description = "Play a sound when a player is flagged for cheating"
            )
        )

        val soundDescLines =
            wrapText("Play a sound when a player is flagged for cheating", fullButtonWidth).size
        val soundDescHeight = soundDescLines * 10 + descriptionPadding

        y += buttonHeight + soundDescHeight + buttonSpacing * 2
        contentHeight += buttonHeight + soundDescHeight + buttonSpacing * 2

        val verboseStatus = if (Config.verboseMode) "§aON§r" else "§cOFF§r"
        val verboseLabel = "Verbose Mode: $verboseStatus"
        buttons.add(
            ModernButton(
                101, contentLeft, y, fullButtonWidth, buttonHeight,
                verboseLabel, ButtonAction.TOGGLE_VERBOSE,
                description = "Show detailed information about each violation"
            )
        )

        val verboseDescLines =
            wrapText("Show detailed information about each violation", fullButtonWidth).size
        val verboseDescHeight = verboseDescLines * 10 + descriptionPadding

        y += buttonHeight + verboseDescHeight + buttonSpacing * 2
        contentHeight += buttonHeight + verboseDescHeight + buttonSpacing * 2

        val cooldownLabel = "Flag Cooldown: ${Config.flagCooldown}s"
        buttons.add(
            ModernButton(
                102, contentLeft, y, settingWidth, buttonHeight,
                cooldownLabel, ButtonAction.TOGGLE_VERBOSE,
                description = "Time in seconds between consecutive flags for the same player and check"
            )
        )


        buttons.add(
            ModernButton(
                103, contentLeft + settingWidth + buttonSpacing, y,
                buttonWidth, buttonHeight, "-", ButtonAction.COOLDOWN_DEC
            )
        )


        buttons.add(
            ModernButton(
                104, contentLeft + settingWidth + buttonWidth + buttonSpacing * 2, y,
                buttonWidth, buttonHeight, "+", ButtonAction.COOLDOWN_INC
            )
        )

        val cooldownDescLines = wrapText(
            "Time in seconds between consecutive flags for the same player and check",
            fullButtonWidth
        ).size
        val cooldownDescHeight = cooldownDescLines * 10 + descriptionPadding

        y += buttonHeight + cooldownDescHeight + buttonSpacing * 2
        contentHeight += buttonHeight + cooldownDescHeight + buttonSpacing * 2

        val creditText = "by @desiyn"
        val creditCenterX = panelLeft + panelWidth / 2
        fontRendererObj.drawString(
            creditText,
            (creditCenterX - fontRendererObj.getStringWidth(creditText) / 2).toFloat(),
            (panelTop + panelHeight - contentPadding - fontRendererObj.FONT_HEIGHT).toFloat(),
            textSecondaryColor.rgb,
            false
        )

        contentHeight += contentPadding
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {

        drawDefaultBackground()

        val centerX = width / 2
        val panelLeft = centerX - panelWidth / 2
        val panelTop = height / 2 - panelHeight / 2


        buttons.forEach {
            it.isHovered = it.isMouseOver(mouseX, mouseY, scrollOffset)
            it.visualY = if (it.id >= 100 && !it.isClose) it.y - scrollOffset else it.y
        }


        drawRect(panelLeft, panelTop, panelLeft + panelWidth, panelTop + panelHeight, backgroundColor.rgb)


        drawRect(
            panelLeft, panelTop + headerHeight + tabHeight,
            panelLeft + panelWidth, panelTop + panelHeight,
            panelColor.rgb
        )


        drawRect(panelLeft, panelTop, panelLeft + panelWidth, panelTop + 1, borderColor.rgb)
        drawRect(
            panelLeft,
            panelTop + panelHeight - 1,
            panelLeft + panelWidth,
            panelTop + panelHeight,
            borderColor.rgb
        )
        drawRect(panelLeft, panelTop, panelLeft + 1, panelTop + panelHeight, borderColor.rgb)
        drawRect(
            panelLeft + panelWidth - 1,
            panelTop,
            panelLeft + panelWidth,
            panelTop + panelHeight,
            borderColor.rgb
        )


        drawRect(
            panelLeft, panelTop,
            panelLeft + panelWidth, panelTop + headerHeight,
            headerColor.rgb
        )

        val titleText = "§lLucid Configuration"
        drawCenteredString(
            fontRendererObj, titleText,
            centerX, panelTop + (headerHeight - 8) / 2,
            0xFFFFFF
        )


        buttons.forEach { button ->
            if (button.id <= 3) {
                button.draw(this)
            }
        }


        GlStateManager.pushMatrix()
        val contentTop = panelTop + headerHeight + tabHeight

        val contentAreaHeight = panelHeight - headerHeight - tabHeight
        val scaledResolution = ScaledResolution(mc)
        val scaleFactor = scaledResolution.scaleFactor
        val scissorLeft = panelLeft * scaleFactor
        val scissorTop = (contentTop) * scaleFactor
        val scissorWidth = panelWidth * scaleFactor
        val scissorHeight = contentAreaHeight * scaleFactor

        GL11.glEnable(GL11.GL_SCISSOR_TEST)
        GL11.glScissor(scissorLeft, mc.displayHeight - scissorTop - scissorHeight, scissorWidth, scissorHeight)


        buttons.forEach { button ->
            if (button.id >= 100) {
                if (button.visualY + button.height >= contentTop && button.visualY <= contentTop + contentAreaHeight) {
                    button.draw(this)
                }

                if (button.description != null) {
                    val wrappedDesc = wrapText(
                        button.description,
                        if (button.action == ButtonAction.TOGGLE) button.width else getButtonWidth(true)
                    )

                    val descriptionStartY = button.visualY + button.height + 4
                    val descriptionHeight = wrappedDesc.size * 10
                    val descriptionEndY = descriptionStartY + descriptionHeight

                    if (descriptionEndY >= contentTop && descriptionStartY <= contentTop + contentAreaHeight) {
                        for (i in wrappedDesc.indices) {
                            val lineY = descriptionStartY + i * 10

                            if (lineY + 10 >= contentTop && lineY <= contentTop + contentAreaHeight) {
                                fontRendererObj.drawString(
                                    wrappedDesc[i],
                                    (button.x).toFloat(),
                                    lineY.toFloat(),
                                    textSecondaryColor.rgb,
                                    false
                                )
                            }
                        }
                    }
                }
            }
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST)
        GlStateManager.popMatrix()

        if (currentTab == Tab.APPEARANCE) {
            val contentLeft = panelLeft
            val previewY = panelTop + panelHeight + 10
            val previewBoxHeight = 40
            val previewBoxWidth = panelWidth

            Gui.drawRect(
                contentLeft,
                previewY,
                contentLeft + previewBoxWidth,
                previewY + previewBoxHeight,
                previewBoxColor.rgb
            )

            Gui.drawRect(contentLeft, previewY, contentLeft + previewBoxWidth, previewY + 1, borderColor.rgb)
            Gui.drawRect(
                contentLeft + previewBoxWidth - 1,
                previewY,
                contentLeft + previewBoxWidth,
                previewY + previewBoxHeight,
                borderColor.rgb
            )
            Gui.drawRect(
                contentLeft,
                previewY + previewBoxHeight - 1,
                contentLeft + previewBoxWidth,
                previewY + previewBoxHeight,
                borderColor.rgb
            )
            Gui.drawRect(contentLeft, previewY, contentLeft + 1, previewY + previewBoxHeight, borderColor.rgb)

            val vlText = if (Config.showVLInFlag) " §7[VL: 10]" else ""
            val wdrText = if (Config.showWDR) " §c[WDR]" else ""

            fontRendererObj.drawStringWithShadow(
                Config.getFormattedPrefix() + "§fPlayer §7failed §${Config.messageColor}Check" + vlText + wdrText,
                (contentLeft + contentPadding).toFloat(),
                (previewY + (previewBoxHeight / 2) - (fontRendererObj.FONT_HEIGHT / 2)).toFloat(),
                textColor.rgb
            )
        }


        if (hasScrollbar) {
            val scrollTrackTop = contentTop + 5
            val scrollTrackBottom = contentTop + contentAreaHeight - 5
            val scrollTrackHeight = scrollTrackBottom - scrollTrackTop

            val scrollbarHeight = max(30, scrollTrackHeight * scrollTrackHeight / contentHeight)
            val scrollProgress = scrollOffset.toFloat() / maxScrollOffset.toFloat()
            val scrollbarY = scrollTrackTop + ((scrollTrackHeight - scrollbarHeight) * scrollProgress).toInt()


            drawRect(
                panelLeft + panelWidth - scrollbarWidth - scrollbarPadding, scrollTrackTop,
                panelLeft + panelWidth - scrollbarPadding, scrollTrackBottom,
                scrollTrackColor.rgb
            )


            drawRect(
                panelLeft + panelWidth - scrollbarWidth - scrollbarPadding, scrollbarY,
                panelLeft + panelWidth - scrollbarPadding, scrollbarY + scrollbarHeight,
                scrollThumbColor.rgb
            )
        }


        if (currentTab == Tab.SETTINGS) {
            val creditText = "by @desiyn"
            val creditX = centerX - fontRendererObj.getStringWidth(creditText) / 2
            val creditY = panelTop + panelHeight - contentPadding - fontRendererObj.FONT_HEIGHT
            fontRendererObj.drawString(creditText, creditX.toFloat(), creditY.toFloat(), textSecondaryColor.rgb, false)
        }
    }

    private fun wrapText(text: String, maxWidth: Int): List<String> {
        val words = text.split(" ")
        val result = mutableListOf<String>()
        var currentLine = ""

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            val width = fontRendererObj.getStringWidth(testLine)

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

    @Throws(IOException::class)
    override fun handleMouseInput() {
        super.handleMouseInput()

        val scroll = Mouse.getEventDWheel()
        if (scroll != 0 && hasScrollbar) {
            scrollOffset -= scroll / 10
            scrollOffset = max(0, min(scrollOffset, maxScrollOffset))
        }
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {

        if (keyCode == 1) {
            saveAndClose()
        }
    }

    @Throws(IOException::class)
    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        super.mouseClicked(mouseX, mouseY, mouseButton)

        if (mouseButton == 0) {
            for (button in buttons) {
                if (button.isMouseOver(mouseX, mouseY, scrollOffset)) {
                    playButtonSound()
                    actionPerformed(button)
                    break
                }
            }
        }
    }

    private fun playButtonSound() {
        mc.soundHandler.playSound(PositionedSoundRecord.create(buttonSound, 1.0f))
    }

    private fun saveAndClose() {
        Config.save()
        mc.thePlayer.addChatMessage(ChatComponentText("${Config.getFormattedPrefix()}§aConfiguration saved successfully!"))
        mc.displayGuiScreen(null)
    }

    private fun actionPerformed(button: ModernButton) {
        when (button.action) {
            ButtonAction.TOGGLE -> if (button.check != null) {
                button.check.enabled = !button.check.enabled
                initGui()
            }

            ButtonAction.DEC -> if (button.check != null && button.check.vlThreshold > 1) {
                button.check.vlThreshold--
                initGui()
            }

            ButtonAction.INC -> if (button.check != null) {
                button.check.vlThreshold++
                initGui()
            }

            ButtonAction.TOGGLE_SOUND -> {
                Config.playSoundOnFlag = !Config.playSoundOnFlag
                initGui()
            }

            ButtonAction.TOGGLE_VERBOSE -> {
                Config.verboseMode = !Config.verboseMode
                initGui()
            }

            ButtonAction.COOLDOWN_DEC -> {
                if (Config.flagCooldown > 1) {
                    Config.flagCooldown--
                    initGui()
                }
            }

            ButtonAction.COOLDOWN_INC -> {
                Config.flagCooldown++
                initGui()
            }

            ButtonAction.COLOR_PREV -> {
                val currentIndex = colorCodes.indexOfFirst { it.first == Config.messageColor }
                val newIndex = if (currentIndex <= 0) colorCodes.size - 1 else currentIndex - 1
                Config.messageColor = colorCodes[newIndex].first
                updateColors()
                initGui()
            }

            ButtonAction.COLOR_NEXT -> {
                val currentIndex = colorCodes.indexOfFirst { it.first == Config.messageColor }
                val newIndex = if (currentIndex >= colorCodes.size - 1) 0 else currentIndex + 1
                Config.messageColor = colorCodes[newIndex].first
                updateColors()
                initGui()
            }

            ButtonAction.TOGGLE_BOLD -> {
                Config.messageBold = !Config.messageBold
                initGui()
            }

            ButtonAction.SYMBOL_TOGGLE -> {
                Config.messageSymbol = if (Config.messageSymbol == ">") "»" else ">"
                initGui()
            }

            ButtonAction.TOGGLE_SHOW_VL -> {
                Config.showVLInFlag = !Config.showVLInFlag
                initGui()
            }

            ButtonAction.TOGGLE_SHOW_WDR -> {
                Config.showWDR = !Config.showWDR
                initGui()
            }

            ButtonAction.TAB_CHECKS -> {
                currentTab = Tab.CHECKS
                scrollOffset = 0
                initGui()
            }

            ButtonAction.TAB_APPEARANCE -> {
                currentTab = Tab.APPEARANCE
                scrollOffset = 0
                initGui()
            }

            ButtonAction.TAB_SETTINGS -> {
                currentTab = Tab.SETTINGS
                scrollOffset = 0
                initGui()
            }

            ButtonAction.CLOSE -> {
                saveAndClose()
            }
        }
    }

    override fun doesGuiPauseGame() = false
}