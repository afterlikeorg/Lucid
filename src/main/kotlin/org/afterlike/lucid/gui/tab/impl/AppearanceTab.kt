package org.afterlike.lucid.gui.tab.impl

import net.minecraft.client.gui.FontRenderer
import org.afterlike.lucid.core.handler.ConfigHandler
import org.afterlike.lucid.gui.component.impl.ButtonComponent
import org.afterlike.lucid.util.GuiUtil

object AppearanceTab {
    
    private const val CONTENT_PADDING = 15
    private const val BUTTON_HEIGHT = 22
    private const val BUTTON_WIDTH = 22
    private const val BUTTON_SPACING = 4
    private const val DESCRIPTION_PADDING = 4
    private const val BOTTOM_PADDING = 15
    
    fun initButtons(
        panelLeft: Int,
        panelTop: Int,
        headerHeight: Int,
        tabHeight: Int,
        fullContentWidth: Int,
        settingContentWidth: Int,
        fontRenderer: FontRenderer,
        buttons: MutableList<ButtonComponent>
    ): Int {
        val contentLeft = panelLeft + CONTENT_PADDING
        val contentStartY = panelTop + headerHeight + tabHeight
        var y = contentStartY + CONTENT_PADDING
        var contentHeight = CONTENT_PADDING
        
        val currentColorName = GuiUtil.colorCodes.find { it.first == ConfigHandler.messageColor.value }?.second ?: "§3Cyan"
        val colorLabel = "Message Color: $currentColorName"
        
        buttons.add(
            ButtonComponent(
                100, contentLeft, y, settingContentWidth, BUTTON_HEIGHT,
                colorLabel, ButtonComponent.ButtonAction.TOGGLE_SOUND,
                description = "The color used for Lucid in chat messages"
            )
        )
        
        buttons.add(
            ButtonComponent(
                101, contentLeft + settingContentWidth + BUTTON_SPACING, y,
                BUTTON_WIDTH, BUTTON_HEIGHT, "◀", ButtonComponent.ButtonAction.COLOR_PREV
            )
        )
        
        buttons.add(
            ButtonComponent(
                102, contentLeft + settingContentWidth + BUTTON_WIDTH + BUTTON_SPACING * 2, y,
                BUTTON_WIDTH, BUTTON_HEIGHT, "▶", ButtonComponent.ButtonAction.COLOR_NEXT
            )
        )
        
        val colorDescLines = GuiUtil.wrapText("The color used for Lucid in chat messages", fullContentWidth, fontRenderer).size
        val colorDescHeight = colorDescLines * 10 + DESCRIPTION_PADDING
        y += BUTTON_HEIGHT + colorDescHeight + BUTTON_SPACING * 2
        contentHeight += BUTTON_HEIGHT + colorDescHeight + BUTTON_SPACING * 2
        
        val boldStatus = if (ConfigHandler.messageBold.value) "§aON§r" else "§cOFF§r"
        val boldLabel = "Bold Text: $boldStatus"
        
        buttons.add(
            ButtonComponent(
                103, contentLeft, y, fullContentWidth, BUTTON_HEIGHT,
                boldLabel, ButtonComponent.ButtonAction.TOGGLE_BOLD,
                description = "Make Lucid text bold in chat messages"
            )
        )
        
        val boldDescLines = GuiUtil.wrapText("Make Lucid text bold in chat messages", fullContentWidth, fontRenderer).size
        val boldDescHeight = boldDescLines * 10 + DESCRIPTION_PADDING
        y += BUTTON_HEIGHT + boldDescHeight + BUTTON_SPACING * 2
        contentHeight += BUTTON_HEIGHT + boldDescHeight + BUTTON_SPACING * 2
        
        val symbolLabel = "Symbol: ${ConfigHandler.messageSymbol.value}"
        
        buttons.add(
            ButtonComponent(
                104, contentLeft, y, fullContentWidth, BUTTON_HEIGHT,
                symbolLabel, ButtonComponent.ButtonAction.SYMBOL_TOGGLE,
                description = "Toggle between > and » symbols"
            )
        )
        
        val symbolDescLines = GuiUtil.wrapText("Toggle between > and » symbols", fullContentWidth, fontRenderer).size
        val symbolDescHeight = symbolDescLines * 10 + DESCRIPTION_PADDING
        y += BUTTON_HEIGHT + symbolDescHeight + BUTTON_SPACING * 2
        contentHeight += BUTTON_HEIGHT + symbolDescHeight + BUTTON_SPACING * 2
        
        val vlStatus = if (ConfigHandler.showVLInFlag.value) "§aON§r" else "§cOFF§r"
        val vlLabel = "Show VL in Flag: $vlStatus"
        
        buttons.add(
            ButtonComponent(
                105, contentLeft, y, fullContentWidth, BUTTON_HEIGHT,
                vlLabel, ButtonComponent.ButtonAction.TOGGLE_SHOW_VL,
                description = "Show violation level in flag messages"
            )
        )
        
        val vlDescLines = GuiUtil.wrapText("Show violation level in flag messages", fullContentWidth, fontRenderer).size
        val vlDescHeight = vlDescLines * 10 + DESCRIPTION_PADDING
        y += BUTTON_HEIGHT + vlDescHeight + BUTTON_SPACING * 2
        contentHeight += BUTTON_HEIGHT + vlDescHeight + BUTTON_SPACING * 2
        
        val wdrStatus = if (ConfigHandler.showWDR.value) "§aON§r" else "§cOFF§r"
        val wdrLabel = "Show WDR Button: $wdrStatus"
        
        buttons.add(
            ButtonComponent(
                106, contentLeft, y, fullContentWidth, BUTTON_HEIGHT,
                wdrLabel, ButtonComponent.ButtonAction.TOGGLE_SHOW_WDR,
                description = "Show the WDR (Watchdog Report) button in flag messages"
            )
        )
        
        val wdrDescLines = GuiUtil.wrapText("Show the WDR (Watchdog Report) button in flag messages", fullContentWidth, fontRenderer).size
        val wdrDescHeight = wdrDescLines * 10 + DESCRIPTION_PADDING
        
        return contentHeight + BUTTON_HEIGHT + wdrDescHeight + BUTTON_SPACING * 2 + BOTTOM_PADDING
    }
}