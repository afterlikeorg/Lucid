package org.afterlike.lucid.gui.tab.impl

import org.afterlike.lucid.core.handler.ConfigHandler
import org.afterlike.lucid.gui.component.impl.ButtonComponent
import org.afterlike.lucid.gui.tab.api.TabContent
import org.afterlike.lucid.gui.tab.api.TabContext
import org.afterlike.lucid.util.GuiUtil

object AppearanceTab : TabContent {

    override val name = "Appearance"

    private const val BUTTON_HEIGHT = 22
    private const val BUTTON_WIDTH = 22
    private const val BUTTON_SPACING = 4
    private const val DESCRIPTION_PADDING = 4
    private const val BOTTOM_PADDING = 15

    override fun initButtons(context: TabContext, buttons: MutableList<ButtonComponent>): Int {
        var y = context.contentStartY + context.contentPadding
        var contentHeight = context.contentPadding

        val currentColorName = GuiUtil.colorCodes.find { it.first == ConfigHandler.messageColor }?.second ?: "§3Cyan"
        val colorLabel = "Message Color: $currentColorName"

        buttons.add(
            ButtonComponent(
                100, context.contentLeft, y, context.settingContentWidth, BUTTON_HEIGHT,
                colorLabel, ButtonComponent.ButtonAction.TOGGLE_SOUND,
                description = "The color used for Lucid in chat messages"
            )
        )

        buttons.add(
            ButtonComponent(
                101, context.contentLeft + context.settingContentWidth + BUTTON_SPACING, y,
                BUTTON_WIDTH, BUTTON_HEIGHT, "◀", ButtonComponent.ButtonAction.COLOR_PREV
            )
        )

        buttons.add(
            ButtonComponent(
                102, context.contentLeft + context.settingContentWidth + BUTTON_WIDTH + BUTTON_SPACING * 2, y,
                BUTTON_WIDTH, BUTTON_HEIGHT, "▶", ButtonComponent.ButtonAction.COLOR_NEXT
            )
        )

        val colorDescLines = GuiUtil.wrapText(
            "The color used for Lucid in chat messages",
            context.fullContentWidth,
            context.fontRenderer
        ).size
        val colorDescHeight = colorDescLines * 10 + DESCRIPTION_PADDING
        y += BUTTON_HEIGHT + colorDescHeight + BUTTON_SPACING * 2
        contentHeight += BUTTON_HEIGHT + colorDescHeight + BUTTON_SPACING * 2

        val boldStatus = if (ConfigHandler.messageBold) "§aON§r" else "§cOFF§r"
        val boldLabel = "Bold Text: $boldStatus"

        buttons.add(
            ButtonComponent(
                103, context.contentLeft, y, context.fullContentWidth, BUTTON_HEIGHT,
                boldLabel, ButtonComponent.ButtonAction.TOGGLE_BOLD,
                description = "Make Lucid text bold in chat messages"
            )
        )

        val boldDescLines = GuiUtil.wrapText(
            "Make Lucid text bold in chat messages",
            context.fullContentWidth,
            context.fontRenderer
        ).size
        val boldDescHeight = boldDescLines * 10 + DESCRIPTION_PADDING
        y += BUTTON_HEIGHT + boldDescHeight + BUTTON_SPACING * 2
        contentHeight += BUTTON_HEIGHT + boldDescHeight + BUTTON_SPACING * 2

        val symbolLabel = "Symbol: ${ConfigHandler.messageSymbol}"

        buttons.add(
            ButtonComponent(
                104, context.contentLeft, y, context.fullContentWidth, BUTTON_HEIGHT,
                symbolLabel, ButtonComponent.ButtonAction.SYMBOL_TOGGLE,
                description = "Toggle between > and » symbols"
            )
        )

        val symbolDescLines =
            GuiUtil.wrapText("Toggle between > and » symbols", context.fullContentWidth, context.fontRenderer).size
        val symbolDescHeight = symbolDescLines * 10 + DESCRIPTION_PADDING
        y += BUTTON_HEIGHT + symbolDescHeight + BUTTON_SPACING * 2
        contentHeight += BUTTON_HEIGHT + symbolDescHeight + BUTTON_SPACING * 2

        val vlStatus = if (ConfigHandler.showVLInFlag) "§aON§r" else "§cOFF§r"
        val vlLabel = "Show VL in Flag: $vlStatus"

        buttons.add(
            ButtonComponent(
                105, context.contentLeft, y, context.fullContentWidth, BUTTON_HEIGHT,
                vlLabel, ButtonComponent.ButtonAction.TOGGLE_SHOW_VL,
                description = "Show violation level in flag messages"
            )
        )

        val vlDescLines = GuiUtil.wrapText(
            "Show violation level in flag messages",
            context.fullContentWidth,
            context.fontRenderer
        ).size
        val vlDescHeight = vlDescLines * 10 + DESCRIPTION_PADDING
        y += BUTTON_HEIGHT + vlDescHeight + BUTTON_SPACING * 2
        contentHeight += BUTTON_HEIGHT + vlDescHeight + BUTTON_SPACING * 2

        val wdrStatus = if (ConfigHandler.showWDR) "§aON§r" else "§cOFF§r"
        val wdrLabel = "Show WDR Button: $wdrStatus"

        buttons.add(
            ButtonComponent(
                106, context.contentLeft, y, context.fullContentWidth, BUTTON_HEIGHT,
                wdrLabel, ButtonComponent.ButtonAction.TOGGLE_SHOW_WDR,
                description = "Show the WDR (Watchdog Report) button in flag messages"
            )
        )

        val wdrDescLines = GuiUtil.wrapText(
            "Show the WDR (Watchdog Report) button in flag messages",
            context.fullContentWidth,
            context.fontRenderer
        ).size
        val wdrDescHeight = wdrDescLines * 10 + DESCRIPTION_PADDING

        return contentHeight + BUTTON_HEIGHT + wdrDescHeight + BUTTON_SPACING * 2 + BOTTOM_PADDING
    }
}
