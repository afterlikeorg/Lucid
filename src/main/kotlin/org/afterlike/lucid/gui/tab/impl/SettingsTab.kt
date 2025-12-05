package org.afterlike.lucid.gui.tab.impl

import org.afterlike.lucid.core.handler.ConfigHandler
import org.afterlike.lucid.gui.component.impl.ButtonComponent
import org.afterlike.lucid.gui.tab.api.TabContent
import org.afterlike.lucid.gui.tab.api.TabContext
import org.afterlike.lucid.util.GuiUtil

object SettingsTab : TabContent {

    override val name = "Settings"

    private const val BUTTON_HEIGHT = 22
    private const val BUTTON_WIDTH = 22
    private const val BUTTON_SPACING = 4
    private const val DESCRIPTION_PADDING = 4

    override fun initButtons(context: TabContext, buttons: MutableList<ButtonComponent>): Int {
        var y = context.contentStartY + context.contentPadding
        var contentHeight = context.contentPadding

        val soundStatus = if (ConfigHandler.playSoundOnFlag) "§aON§r" else "§cOFF§r"
        val soundLabel = "Play Sound on Flag: $soundStatus"

        buttons.add(
            ButtonComponent(
                100, context.contentLeft, y, context.fullContentWidth, BUTTON_HEIGHT,
                soundLabel, ButtonComponent.ButtonAction.TOGGLE_SOUND,
                description = "Play a sound when a player is flagged for cheating"
            )
        )

        val soundDescLines = GuiUtil.wrapText(
            "Play a sound when a player is flagged for cheating",
            context.fullContentWidth,
            context.fontRenderer
        ).size
        val soundDescHeight = soundDescLines * 10 + DESCRIPTION_PADDING
        y += BUTTON_HEIGHT + soundDescHeight + BUTTON_SPACING * 2
        contentHeight += BUTTON_HEIGHT + soundDescHeight + BUTTON_SPACING * 2

        val verboseStatus = if (ConfigHandler.verboseMode) "§aON§r" else "§cOFF§r"
        val verboseLabel = "Verbose Mode: $verboseStatus"

        buttons.add(
            ButtonComponent(
                101, context.contentLeft, y, context.fullContentWidth, BUTTON_HEIGHT,
                verboseLabel, ButtonComponent.ButtonAction.TOGGLE_VERBOSE,
                description = "Show detailed information about each violation"
            )
        )

        val verboseDescLines = GuiUtil.wrapText(
            "Show detailed information about each violation",
            context.fullContentWidth,
            context.fontRenderer
        ).size
        val verboseDescHeight = verboseDescLines * 10 + DESCRIPTION_PADDING
        y += BUTTON_HEIGHT + verboseDescHeight + BUTTON_SPACING * 2
        contentHeight += BUTTON_HEIGHT + verboseDescHeight + BUTTON_SPACING * 2

        val cooldownLabel = "Flag Cooldown: ${ConfigHandler.flagCooldown}s"

        buttons.add(
            ButtonComponent(
                102, context.contentLeft, y, context.settingContentWidth, BUTTON_HEIGHT,
                cooldownLabel, ButtonComponent.ButtonAction.TOGGLE_VERBOSE,
                description = "Time in seconds between consecutive flags for the same player and check"
            )
        )

        buttons.add(
            ButtonComponent(
                103, context.contentLeft + context.settingContentWidth + BUTTON_SPACING, y,
                BUTTON_WIDTH, BUTTON_HEIGHT, "-", ButtonComponent.ButtonAction.COOLDOWN_DEC
            )
        )

        buttons.add(
            ButtonComponent(
                104, context.contentLeft + context.settingContentWidth + BUTTON_WIDTH + BUTTON_SPACING * 2, y,
                BUTTON_WIDTH, BUTTON_HEIGHT, "+", ButtonComponent.ButtonAction.COOLDOWN_INC
            )
        )

        val cooldownDescLines = GuiUtil.wrapText(
            "Time in seconds between consecutive flags for the same player and check",
            context.fullContentWidth, context.fontRenderer
        ).size
        val cooldownDescHeight = cooldownDescLines * 10 + DESCRIPTION_PADDING

        return contentHeight + BUTTON_HEIGHT + cooldownDescHeight + BUTTON_SPACING * 2 + context.contentPadding
    }
}
