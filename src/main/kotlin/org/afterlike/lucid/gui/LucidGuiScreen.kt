package org.afterlike.lucid.gui

import net.minecraft.client.audio.PositionedSoundRecord
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.ChatComponentText
import net.minecraft.util.ResourceLocation
import org.afterlike.lucid.core.handler.ConfigHandler
import org.afterlike.lucid.gui.component.base.BaseComponent
import org.afterlike.lucid.gui.component.impl.ButtonComponent
import org.afterlike.lucid.gui.component.impl.CloseComponent
import org.afterlike.lucid.gui.component.impl.LinkComponent
import org.afterlike.lucid.gui.component.impl.ScrollbarComponent
import org.afterlike.lucid.gui.tab.base.BaseTab
import org.afterlike.lucid.gui.tab.impl.AppearanceTab
import org.afterlike.lucid.gui.tab.impl.ChecksTab
import org.afterlike.lucid.gui.tab.impl.SettingsTab
import org.afterlike.lucid.util.GuiUtil
import org.afterlike.lucid.util.ThemeUtil
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.GL11
import java.io.IOException
import kotlin.math.max
import kotlin.math.min

class LucidGuiScreen : GuiScreen() {

    private enum class Tab { CHECKS, APPEARANCE, SETTINGS }

    private var currentTab = Tab.CHECKS
    private val components = mutableListOf<BaseComponent>()
    private val buttons = mutableListOf<ButtonComponent>()
    private val tabs = mutableListOf<BaseTab>()
    private var closeButton: CloseComponent? = null
    private var scrollbarComponent: ScrollbarComponent? = null
    private var linkComponent: LinkComponent? = null

    private lateinit var theme: ThemeUtil

    private val panelWidth = 300
    private val panelHeight = 330
    private val headerHeight = 30
    private val tabHeight = 24
    private val contentPadding = 15
    private val scrollbarWidth = 4
    private val scrollbarPadding = 5
    private val closeButtonSize = 16

    private var scrollOffset = 0
    private var maxScrollOffset = 0
    private var contentHeight = 0
    private var hasScrollbar = false

    private val buttonSound = ResourceLocation("gui.button.press")

    override fun initGui() {
        components.clear()
        buttons.clear()
        tabs.clear()
        scrollbarComponent = null
        linkComponent = null
        super.initGui()

        theme = ThemeUtil(ConfigHandler.messageColor.value)

        val centerX = width / 2
        val panelLeft = centerX - panelWidth / 2
        val panelTop = height / 2 - panelHeight / 2

        closeButton = CloseComponent(
            0, panelLeft + panelWidth - closeButtonSize - 6,
            panelTop + (headerHeight - closeButtonSize) / 2,
            closeButtonSize
        )
        closeButton?.let { components.add(it) }

        val tabWidth = panelWidth / 3

        val checksTab = BaseTab(
            1, panelLeft, panelTop + headerHeight,
            tabWidth, tabHeight, "Checks",
            BaseTab.TabAction.CHECKS,
            isActive = currentTab == Tab.CHECKS
        )
        tabs.add(checksTab)
        components.add(checksTab)

        val appearanceTab = BaseTab(
            2, panelLeft + tabWidth, panelTop + headerHeight,
            tabWidth, tabHeight, "Appearance",
            BaseTab.TabAction.APPEARANCE,
            isActive = currentTab == Tab.APPEARANCE
        )
        tabs.add(appearanceTab)
        components.add(appearanceTab)

        val settingsTab = BaseTab(
            3, panelLeft + tabWidth * 2, panelTop + headerHeight,
            tabWidth, tabHeight, "Settings",
            BaseTab.TabAction.SETTINGS,
            isActive = currentTab == Tab.SETTINGS
        )
        tabs.add(settingsTab)
        components.add(settingsTab)

        val contentAreaHeight = panelHeight - headerHeight - tabHeight - contentPadding
        val tempContentWidth = getContentWidth(false)

        contentHeight = when (currentTab) {
            Tab.CHECKS -> ChecksTab.initButtons(
                panelLeft, panelTop, headerHeight, tabHeight,
                tempContentWidth, fontRendererObj, buttons
            )
            Tab.APPEARANCE -> AppearanceTab.initButtons(
                panelLeft, panelTop, headerHeight, tabHeight,
                getButtonWidth(fullWidth = true, withScrollbar = false),
                getButtonWidth(fullWidth = false, withScrollbar = false),
                fontRendererObj, buttons
            )
            Tab.SETTINGS -> SettingsTab.initButtons(
                panelLeft, panelTop, headerHeight, tabHeight,
                getButtonWidth(fullWidth = true, withScrollbar = false),
                getButtonWidth(fullWidth = false, withScrollbar = false),
                fontRendererObj, buttons
            )
        }

        hasScrollbar = contentHeight > contentAreaHeight

        buttons.clear()

        contentHeight = when (currentTab) {
            Tab.CHECKS -> ChecksTab.initButtons(
                panelLeft, panelTop, headerHeight, tabHeight,
                getContentWidth(hasScrollbar), fontRendererObj, buttons
            )
            Tab.APPEARANCE -> AppearanceTab.initButtons(
                panelLeft, panelTop, headerHeight, tabHeight,
                getButtonWidth(true, hasScrollbar),
                getButtonWidth(false, hasScrollbar),
                fontRendererObj, buttons
            )
            Tab.SETTINGS -> SettingsTab.initButtons(
                panelLeft, panelTop, headerHeight, tabHeight,
                getButtonWidth(true, hasScrollbar),
                getButtonWidth(false, hasScrollbar),
                fontRendererObj, buttons
            )
        }

        buttons.forEach { components.add(it) }

        maxScrollOffset = max(0, contentHeight - contentAreaHeight)
        scrollOffset = min(scrollOffset, maxScrollOffset)

        if (hasScrollbar) {
            val contentTop = panelTop + headerHeight + tabHeight
            val contentAreaHeight = panelHeight - headerHeight - tabHeight
            scrollbarComponent = ScrollbarComponent(
                999,
                panelLeft + panelWidth - scrollbarWidth - scrollbarPadding,
                contentTop,
                scrollbarWidth,
                contentAreaHeight,
                { scrollOffset },
                { maxScrollOffset },
                { contentHeight }
            )
            scrollbarComponent?.let { components.add(it) }
        }

        if (currentTab == Tab.SETTINGS) {
            val creditY = panelTop + panelHeight - contentPadding - fontRendererObj.FONT_HEIGHT
            linkComponent = LinkComponent(
                998,
                centerX,
                creditY,
                "View Lucid on GitHub ↗",
                "https://github.com/m-afterlike/Lucid",
                this,
                centered = true
            )
            linkComponent?.let { components.add(it) }
        }
    }

    private fun getContentWidth(withScrollbar: Boolean): Int {
        return panelWidth - 2 * contentPadding -
                if (withScrollbar) scrollbarWidth + scrollbarPadding * 2 else 0
    }

    private fun getButtonWidth(fullWidth: Boolean, withScrollbar: Boolean): Int {
        val effectiveWidth = getContentWidth(withScrollbar)
        return if (fullWidth) effectiveWidth else effectiveWidth - (44 + 8)
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawDefaultBackground()

        val centerX = width / 2
        val panelLeft = centerX - panelWidth / 2
        val panelTop = height / 2 - panelHeight / 2

        components.forEach {
            it.isHovered = it.isMouseOver(mouseX, mouseY, scrollOffset)
            it.visualY = if (it.id >= 100) it.y - scrollOffset else it.y
        }

        drawRect(panelLeft, panelTop, panelLeft + panelWidth, panelTop + panelHeight, theme.backgroundColor.rgb)
        drawRect(panelLeft, panelTop + headerHeight + tabHeight, panelLeft + panelWidth,
            panelTop + panelHeight, theme.panelColor.rgb)

        drawPanelBorder(panelLeft, panelTop)
        drawHeader(panelLeft, panelTop, centerX)

        tabs.forEach { it.draw(theme) }
        closeButton?.draw(theme)

        drawScrollableContent(panelLeft, panelTop)

        if (currentTab == Tab.APPEARANCE) {
            drawAppearancePreview(panelLeft, panelTop)
        }

        scrollbarComponent?.draw(theme)
        linkComponent?.draw(theme)
    }

    private fun drawPanelBorder(panelLeft: Int, panelTop: Int) {
        drawRect(panelLeft, panelTop, panelLeft + panelWidth, panelTop + 1, theme.borderColor.rgb)
        drawRect(panelLeft, panelTop + panelHeight - 1, panelLeft + panelWidth,
            panelTop + panelHeight, theme.borderColor.rgb)
        drawRect(panelLeft, panelTop, panelLeft + 1, panelTop + panelHeight, theme.borderColor.rgb)
        drawRect(panelLeft + panelWidth - 1, panelTop, panelLeft + panelWidth,
            panelTop + panelHeight, theme.borderColor.rgb)
    }

    private fun drawHeader(panelLeft: Int, panelTop: Int, centerX: Int) {
        drawRect(panelLeft, panelTop, panelLeft + panelWidth, panelTop + headerHeight, theme.headerColor.rgb)
        drawCenteredString(fontRendererObj, "§lLucid Configuration", centerX,
            panelTop + (headerHeight - 8) / 2, 0xFFFFFF)
    }

    private fun drawScrollableContent(panelLeft: Int, panelTop: Int) {
        GlStateManager.pushMatrix()
        val contentTop = panelTop + headerHeight + tabHeight
        val contentAreaHeight = panelHeight - headerHeight - tabHeight
        val scaledResolution = ScaledResolution(mc)
        val scaleFactor = scaledResolution.scaleFactor

        GL11.glEnable(GL11.GL_SCISSOR_TEST)
        GL11.glScissor(
            panelLeft * scaleFactor,
            mc.displayHeight - (contentTop * scaleFactor) - (contentAreaHeight * scaleFactor),
            panelWidth * scaleFactor,
            contentAreaHeight * scaleFactor
        )

        buttons.forEach { button ->
            if (button.visualY + button.height >= contentTop &&
                button.visualY <= contentTop + contentAreaHeight) {
                button.draw(theme)
            }

            button.description?.let { description ->
                val wrappedDesc = GuiUtil.wrapText(
                    description,
                    if (button.action == ButtonComponent.ButtonAction.TOGGLE) button.width
                    else getButtonWidth(true, hasScrollbar),
                    fontRendererObj
                )

                val descriptionStartY = button.visualY + button.height + 4
                val descriptionEndY = descriptionStartY + wrappedDesc.size * 10

                if (descriptionEndY >= contentTop && descriptionStartY <= contentTop + contentAreaHeight) {
                    wrappedDesc.forEachIndexed { i, line ->
                        val lineY = descriptionStartY + i * 10
                        if (lineY + 10 >= contentTop && lineY <= contentTop + contentAreaHeight) {
                            fontRendererObj.drawString(line, button.x.toFloat(),
                                lineY.toFloat(), theme.textSecondaryColor.rgb, false)
                        }
                    }
                }
            }
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST)
        GlStateManager.popMatrix()
    }

    private fun drawAppearancePreview(panelLeft: Int, panelTop: Int) {
        val previewY = panelTop + panelHeight + 10
        val previewBoxHeight = 40

        drawRect(panelLeft, previewY, panelLeft + panelWidth,
            previewY + previewBoxHeight, theme.previewBoxColor.rgb)

        drawRect(panelLeft, previewY, panelLeft + panelWidth, previewY + 1, theme.borderColor.rgb)
        drawRect(panelLeft + panelWidth - 1, previewY, panelLeft + panelWidth,
            previewY + previewBoxHeight, theme.borderColor.rgb)
        drawRect(panelLeft, previewY + previewBoxHeight - 1, panelLeft + panelWidth,
            previewY + previewBoxHeight, theme.borderColor.rgb)
        drawRect(panelLeft, previewY, panelLeft + 1, previewY + previewBoxHeight, theme.borderColor.rgb)

        val vlText = if (ConfigHandler.showVLInFlag.value) " §7[VL:10]" else ""
        val wdrText = if (ConfigHandler.showWDR.value) " §c[WDR]" else ""

        fontRendererObj.drawStringWithShadow(
            ConfigHandler.getFormattedPrefix() + "§fPlayer §7failed §${ConfigHandler.messageColor.value}Check" + vlText + wdrText,
            (panelLeft + contentPadding).toFloat(),
            (previewY + (previewBoxHeight / 2) - (fontRendererObj.FONT_HEIGHT / 2)).toFloat(),
            theme.textColor.rgb
        )
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
            closeButton?.let {
                if (it.isMouseOver(mouseX, mouseY, scrollOffset)) {
                    playButtonSound()
                    saveAndClose()
                    return
                }
            }

            for (tab in tabs) {
                if (tab.isMouseOver(mouseX, mouseY, scrollOffset)) {
                    playButtonSound()
                    handleTabAction(tab.action)
                    return
                }
            }

            for (button in buttons) {
                if (button.isMouseOver(mouseX, mouseY, scrollOffset)) {
                    playButtonSound()
                    handleButtonAction(button)
                    return
                }
            }

            linkComponent?.let {
                if (it.isMouseOver(mouseX, mouseY, scrollOffset)) {
                    playButtonSound()
                    it.onClick()
                    return
                }
            }
        }
    }

    private fun playButtonSound() {
        mc.soundHandler.playSound(PositionedSoundRecord.create(buttonSound, 1.0f))
    }

    private fun saveAndClose() {
        ConfigHandler.save()
        mc.thePlayer.addChatMessage(ChatComponentText("${ConfigHandler.getFormattedPrefix()}§aConfiguration saved successfully!"))
        mc.displayGuiScreen(null)
    }

    private fun handleTabAction(action: BaseTab.TabAction) {
        when (action) {
            BaseTab.TabAction.CHECKS -> {
                currentTab = Tab.CHECKS
                scrollOffset = 0
                initGui()
            }
            BaseTab.TabAction.APPEARANCE -> {
                currentTab = Tab.APPEARANCE
                scrollOffset = 0
                initGui()
            }
            BaseTab.TabAction.SETTINGS -> {
                currentTab = Tab.SETTINGS
                scrollOffset = 0
                initGui()
            }
        }
    }

    private fun handleButtonAction(button: ButtonComponent) {
        when (button.action) {
            ButtonComponent.ButtonAction.TOGGLE -> {
                button.check?.let {
                    it.enabled = !it.enabled
                    initGui()
                }
            }
            ButtonComponent.ButtonAction.DEC -> {
                button.check?.let {
                    if (it.violationLevelThreshold > 1) {
                        it.violationLevelThreshold--
                        initGui()
                    }
                }
            }
            ButtonComponent.ButtonAction.INC -> {
                button.check?.let {
                    it.violationLevelThreshold++
                    initGui()
                }
            }
            ButtonComponent.ButtonAction.TOGGLE_SOUND -> {
                ConfigHandler.playSoundOnFlag.value = !ConfigHandler.playSoundOnFlag.value
                initGui()
            }
            ButtonComponent.ButtonAction.TOGGLE_VERBOSE -> {
                ConfigHandler.verboseMode.value = !ConfigHandler.verboseMode.value
                initGui()
            }
            ButtonComponent.ButtonAction.COOLDOWN_DEC -> {
                if (ConfigHandler.flagCooldown.value > 1) {
                    ConfigHandler.flagCooldown.value--
                    initGui()
                }
            }
            ButtonComponent.ButtonAction.COOLDOWN_INC -> {
                ConfigHandler.flagCooldown.value++
                initGui()
            }
            ButtonComponent.ButtonAction.COLOR_PREV -> {
                val currentIndex = GuiUtil.colorCodes.indexOfFirst { it.first == ConfigHandler.messageColor.value }
                val newIndex = if (currentIndex <= 0) GuiUtil.colorCodes.size - 1 else currentIndex - 1
                ConfigHandler.messageColor.value = GuiUtil.colorCodes[newIndex].first
                initGui()
            }
            ButtonComponent.ButtonAction.COLOR_NEXT -> {
                val currentIndex = GuiUtil.colorCodes.indexOfFirst { it.first == ConfigHandler.messageColor.value }
                val newIndex = if (currentIndex >= GuiUtil.colorCodes.size - 1) 0 else currentIndex + 1
                ConfigHandler.messageColor.value = GuiUtil.colorCodes[newIndex].first
                initGui()
            }
            ButtonComponent.ButtonAction.TOGGLE_BOLD -> {
                ConfigHandler.messageBold.value = !ConfigHandler.messageBold.value
                initGui()
            }
            ButtonComponent.ButtonAction.SYMBOL_TOGGLE -> {
                ConfigHandler.messageSymbol.value = if (ConfigHandler.messageSymbol.value == ">") "»" else ">"
                initGui()
            }
            ButtonComponent.ButtonAction.TOGGLE_SHOW_VL -> {
                ConfigHandler.showVLInFlag.value = !ConfigHandler.showVLInFlag.value
                initGui()
            }
            ButtonComponent.ButtonAction.TOGGLE_SHOW_WDR -> {
                ConfigHandler.showWDR.value = !ConfigHandler.showWDR.value
                initGui()
            }
        }
    }

    override fun doesGuiPauseGame() = false
}