package org.afterlike.lucid.gui.tab.impl

import net.minecraft.client.gui.FontRenderer
import org.afterlike.lucid.core.handler.ConfigHandler
import org.afterlike.lucid.gui.component.impl.ButtonComponent
import org.afterlike.lucid.util.GuiUtil

object SettingsTab {
    
    private const val CONTENT_PADDING = 15
    private const val BUTTON_HEIGHT = 22
    private const val BUTTON_WIDTH = 22
    private const val BUTTON_SPACING = 4
    private const val DESCRIPTION_PADDING = 4
    
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
        
        val soundStatus = if (ConfigHandler.playSoundOnFlag.value) "§aON§r" else "§cOFF§r"
        val soundLabel = "Play Sound on Flag: $soundStatus"
        
        buttons.add(
            ButtonComponent(
                100, contentLeft, y, fullContentWidth, BUTTON_HEIGHT,
                soundLabel, ButtonComponent.ButtonAction.TOGGLE_SOUND,
                description = "Play a sound when a player is flagged for cheating"
            )
        )
        
        val soundDescLines = GuiUtil.wrapText("Play a sound when a player is flagged for cheating", fullContentWidth, fontRenderer).size
        val soundDescHeight = soundDescLines * 10 + DESCRIPTION_PADDING
        y += BUTTON_HEIGHT + soundDescHeight + BUTTON_SPACING * 2
        contentHeight += BUTTON_HEIGHT + soundDescHeight + BUTTON_SPACING * 2
        
        val verboseStatus = if (ConfigHandler.verboseMode.value) "§aON§r" else "§cOFF§r"
        val verboseLabel = "Verbose Mode: $verboseStatus"
        
        buttons.add(
            ButtonComponent(
                101, contentLeft, y, fullContentWidth, BUTTON_HEIGHT,
                verboseLabel, ButtonComponent.ButtonAction.TOGGLE_VERBOSE,
                description = "Show detailed information about each violation"
            )
        )
        
        val verboseDescLines = GuiUtil.wrapText("Show detailed information about each violation", fullContentWidth, fontRenderer).size
        val verboseDescHeight = verboseDescLines * 10 + DESCRIPTION_PADDING
        y += BUTTON_HEIGHT + verboseDescHeight + BUTTON_SPACING * 2
        contentHeight += BUTTON_HEIGHT + verboseDescHeight + BUTTON_SPACING * 2
        
        val cooldownLabel = "Flag Cooldown: ${ConfigHandler.flagCooldown.value}s"
        
        buttons.add(
            ButtonComponent(
                102, contentLeft, y, settingContentWidth, BUTTON_HEIGHT,
                cooldownLabel, ButtonComponent.ButtonAction.TOGGLE_VERBOSE,
                description = "Time in seconds between consecutive flags for the same player and check"
            )
        )
        
        buttons.add(
            ButtonComponent(
                103, contentLeft + settingContentWidth + BUTTON_SPACING, y,
                BUTTON_WIDTH, BUTTON_HEIGHT, "-", ButtonComponent.ButtonAction.COOLDOWN_DEC
            )
        )
        
        buttons.add(
            ButtonComponent(
                104, contentLeft + settingContentWidth + BUTTON_WIDTH + BUTTON_SPACING * 2, y,
                BUTTON_WIDTH, BUTTON_HEIGHT, "+", ButtonComponent.ButtonAction.COOLDOWN_INC
            )
        )
        
        val cooldownDescLines = GuiUtil.wrapText(
            "Time in seconds between consecutive flags for the same player and check",
            fullContentWidth, fontRenderer
        ).size
        val cooldownDescHeight = cooldownDescLines * 10 + DESCRIPTION_PADDING
        
        return contentHeight + BUTTON_HEIGHT + cooldownDescHeight + BUTTON_SPACING * 2 + CONTENT_PADDING
    }
}

