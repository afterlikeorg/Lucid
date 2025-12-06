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

        // Play Sound on Flag
        val soundStatus = if (ConfigHandler.playSoundOnFlag) "§aON§r" else "§cOFF§r"
        buttons.add(
            ButtonComponent(
                100, context.contentLeft, y, context.fullContentWidth, BUTTON_HEIGHT,
                "Play Sound on Flag: $soundStatus", ButtonComponent.ButtonAction.TOGGLE_SOUND,
                description = "Play a sound when a player is flagged for cheating"
            )
        )
        val soundDescHeight = getDescHeight("Play a sound when a player is flagged for cheating", context)
        y += BUTTON_HEIGHT + soundDescHeight + BUTTON_SPACING * 2
        contentHeight += BUTTON_HEIGHT + soundDescHeight + BUTTON_SPACING * 2

        // Verbose Mode
        val verboseStatus = if (ConfigHandler.verboseMode) "§aON§r" else "§cOFF§r"
        buttons.add(
            ButtonComponent(
                101, context.contentLeft, y, context.fullContentWidth, BUTTON_HEIGHT,
                "Verbose Mode: $verboseStatus", ButtonComponent.ButtonAction.TOGGLE_VERBOSE,
                description = "Show detailed information about each violation"
            )
        )
        val verboseDescHeight = getDescHeight("Show detailed information about each violation", context)
        y += BUTTON_HEIGHT + verboseDescHeight + BUTTON_SPACING * 2
        contentHeight += BUTTON_HEIGHT + verboseDescHeight + BUTTON_SPACING * 2

        // Debug Mode
        val debugStatus = if (ConfigHandler.debugMode) "§aON§r" else "§cOFF§r"
        buttons.add(
            ButtonComponent(
                102, context.contentLeft, y, context.fullContentWidth, BUTTON_HEIGHT,
                "Debug Mode: $debugStatus", ButtonComponent.ButtonAction.TOGGLE_DEBUG,
                description = "Show debug messages for attack detection, block changes, and other events"
            )
        )
        val debugDescHeight =
            getDescHeight("Show debug messages for attack detection, block changes, and other events", context)
        y += BUTTON_HEIGHT + debugDescHeight + BUTTON_SPACING * 2
        contentHeight += BUTTON_HEIGHT + debugDescHeight + BUTTON_SPACING * 2

        // Flag Cooldown
        buttons.add(
            ButtonComponent(
                103, context.contentLeft, y, context.settingContentWidth, BUTTON_HEIGHT,
                "Flag Cooldown: ${ConfigHandler.flagCooldown}s", ButtonComponent.ButtonAction.TOGGLE_VERBOSE,
                description = "Time in seconds between consecutive flags for the same player and check"
            )
        )
        buttons.add(
            ButtonComponent(
                104, context.contentLeft + context.settingContentWidth + BUTTON_SPACING, y,
                BUTTON_WIDTH, BUTTON_HEIGHT, "-", ButtonComponent.ButtonAction.COOLDOWN_DEC
            )
        )
        buttons.add(
            ButtonComponent(
                105, context.contentLeft + context.settingContentWidth + BUTTON_WIDTH + BUTTON_SPACING * 2, y,
                BUTTON_WIDTH, BUTTON_HEIGHT, "+", ButtonComponent.ButtonAction.COOLDOWN_INC
            )
        )
        val cooldownDescHeight =
            getDescHeight("Time in seconds between consecutive flags for the same player and check", context)

        return contentHeight + BUTTON_HEIGHT + cooldownDescHeight + BUTTON_SPACING * 2 + context.contentPadding
    }

    private fun getDescHeight(text: String, context: TabContext): Int {
        return GuiUtil.wrapText(text, context.fullContentWidth, context.fontRenderer).size * 10 + DESCRIPTION_PADDING
    }
}
