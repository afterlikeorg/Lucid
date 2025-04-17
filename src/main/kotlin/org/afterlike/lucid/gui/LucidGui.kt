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

class LucidGui : GuiScreen() {

    private val backgroundColor = Color(22, 25, 37, 245)
    private val panelColor = Color(32, 35, 48, 255)
    private val accentColor = Color(42, 123, 155)
    private val accentColorHover = Color(60, 145, 180)
    private val accentColorDisabled = Color(30, 80, 110)
    private val buttonColor = Color(45, 48, 60, 255)
    private val buttonHoverColor = Color(55, 60, 75, 255)
    private val activeTabColor = Color(60, 65, 80, 255)
    private val textColor = Color(240, 240, 250, 255)
    private val textSecondaryColor = Color(180, 180, 195, 255)
    private val borderColor = Color(60, 65, 80, 255)
    private val headerColor = Color(28, 32, 42, 255)
    private val titleColor = Color(66, 197, 245, 255)

    private enum class Tab { CHECKS, SETTINGS }

    private var currentTab = Tab.CHECKS

    private enum class ButtonAction {
        TOGGLE, DEC, INC,
        TOGGLE_SOUND, TOGGLE_VERBOSE,
        TAB_CHECKS, TAB_SETTINGS,
        CLOSE,
        COOLDOWN_DEC, COOLDOWN_INC
    }

    private val panelWidth = 300
    private val panelHeight = 330
    private val headerHeight = 30
    private val tabHeight = 24
    private val buttonHeight = 22
    private val buttonWidth = 22
    private val toggleWidth = panelWidth - 2 * 15 - buttonWidth * 2 - 15
    private val contentPadding = 15
    private val buttonSpacing = 4
    private val descriptionPadding = 4
    private val scrollbarWidth = 4
    private val closeButtonSize = 16

    private var scrollOffset = 0
    private var maxScrollOffset = 0
    private var isScrolling = false
    private var contentHeight = 0

    private val buttonSound = ResourceLocation("gui.button.press")

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

    override fun initGui() {
        buttons.clear()
        super.initGui()

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

        val tabWidth = panelWidth / 2
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
                tabWidth, tabHeight, "Settings",
                ButtonAction.TAB_SETTINGS,
                enabled = currentTab != Tab.SETTINGS,
                isTab = true,
                isActive = currentTab == Tab.SETTINGS
            )
        )


        when (currentTab) {
            Tab.CHECKS -> initChecksTab(panelLeft, panelTop)
            Tab.SETTINGS -> initSettingsTab(panelLeft, panelTop)
        }

        val contentAreaHeight = panelHeight - headerHeight - tabHeight - contentPadding
        maxScrollOffset = Math.max(0, contentHeight - contentAreaHeight)
        scrollOffset = Math.min(scrollOffset, maxScrollOffset)
    }

    private fun initChecksTab(panelLeft: Int, panelTop: Int) {
        val contentLeft = panelLeft + contentPadding
        val contentStartY = panelTop + headerHeight + tabHeight
        var y = contentStartY + contentPadding

        contentHeight = contentPadding

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


        contentHeight += contentPadding
    }

    private fun initSettingsTab(panelLeft: Int, panelTop: Int) {
        val contentLeft = panelLeft + contentPadding
        val contentStartY = panelTop + headerHeight + tabHeight
        var y = contentStartY + contentPadding

        val settingWidth =
            panelWidth - contentPadding * 2 - (buttonWidth * 2 + buttonSpacing * 3)
        contentHeight = contentPadding

        val soundStatus = if (Config.playSoundOnFlag) "§aON§r" else "§cOFF§r"
        val soundLabel = "Play Sound on Flag: $soundStatus"
        buttons.add(
            ModernButton(
                100, contentLeft, y, panelWidth - contentPadding * 2, buttonHeight,
                soundLabel, ButtonAction.TOGGLE_SOUND,
                description = "Play a sound when a player is flagged for cheating"
            )
        )

        val soundDescLines =
            wrapText("Play a sound when a player is flagged for cheating", panelWidth - contentPadding * 2).size
        val soundDescHeight = soundDescLines * 10 + descriptionPadding

        y += buttonHeight + soundDescHeight + buttonSpacing * 2
        contentHeight += buttonHeight + soundDescHeight + buttonSpacing * 2

        val verboseStatus = if (Config.verboseMode) "§aON§r" else "§cOFF§r"
        val verboseLabel = "Verbose Mode: $verboseStatus"
        buttons.add(
            ModernButton(
                101, contentLeft, y, panelWidth - contentPadding * 2, buttonHeight,
                verboseLabel, ButtonAction.TOGGLE_VERBOSE,
                description = "Show detailed information about each violation"
            )
        )

        val verboseDescLines =
            wrapText("Show detailed information about each violation", panelWidth - contentPadding * 2).size
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
            panelWidth - contentPadding * 2
        ).size
        val cooldownDescHeight = cooldownDescLines * 10 + descriptionPadding

        y += buttonHeight + cooldownDescHeight + buttonSpacing * 2
        contentHeight += buttonHeight + cooldownDescHeight + buttonSpacing * 2


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
            if (button.id <= 2) {
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
                        if (button.action == ButtonAction.TOGGLE) button.width else panelWidth - contentPadding * 2
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
                                    (button.x + 5).toFloat(),
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


        if (maxScrollOffset > 0) {

            val scrollTrackTop = contentTop + 5
            val scrollTrackBottom = contentTop + contentAreaHeight - 5
            val scrollTrackHeight = scrollTrackBottom - scrollTrackTop

            val scrollbarHeight = Math.max(30, scrollTrackHeight * scrollTrackHeight / contentHeight)
            val scrollProgress = scrollOffset.toFloat() / maxScrollOffset.toFloat()
            val scrollbarY = scrollTrackTop + ((scrollTrackHeight - scrollbarHeight) * scrollProgress).toInt()


            drawRect(
                panelLeft + panelWidth - scrollbarWidth - 5, scrollTrackTop,
                panelLeft + panelWidth - 5, scrollTrackBottom,
                Color(40, 40, 50).rgb
            )


            drawRect(
                panelLeft + panelWidth - scrollbarWidth - 5, scrollbarY,
                panelLeft + panelWidth - 5, scrollbarY + scrollbarHeight,
                Color(100, 100, 120).rgb
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
        if (scroll != 0) {
            scrollOffset -= scroll / 10
            scrollOffset = Math.max(0, Math.min(scrollOffset, maxScrollOffset))
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
        mc.thePlayer.addChatMessage(ChatComponentText("§3Lucid §8> §aConfiguration saved successfully!"))
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

            ButtonAction.TAB_CHECKS -> {
                currentTab = Tab.CHECKS
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