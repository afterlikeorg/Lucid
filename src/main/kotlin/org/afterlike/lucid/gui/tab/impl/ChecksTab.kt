package org.afterlike.lucid.gui.tab.impl

import net.minecraft.client.gui.FontRenderer
import org.afterlike.lucid.check.handler.CheckHandler
import org.afterlike.lucid.gui.component.impl.ButtonComponent
import org.afterlike.lucid.util.GuiUtil

object ChecksTab {
    
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
        contentWidth: Int,
        fontRenderer: FontRenderer,
        buttons: MutableList<ButtonComponent>
    ): Int {
        val contentLeft = panelLeft + CONTENT_PADDING
        val contentStartY = panelTop + headerHeight + tabHeight
        var y = contentStartY + CONTENT_PADDING
        var contentHeight = CONTENT_PADDING
        
        val toggleWidth = contentWidth - (2 * BUTTON_WIDTH + 2 * BUTTON_SPACING)
        
        CheckHandler.getChecks().forEachIndexed { index, check ->
            val id = 100 + index * 3
            
            val statusText = if (check.enabled) "§aON§r" else "§cOFF§r"
            val toggleLabel = "${check.name}: $statusText [VL:${check.violationLevelThreshold}]"
            
            buttons.add(
                ButtonComponent(
                    id, contentLeft, y, toggleWidth, BUTTON_HEIGHT,
                    toggleLabel, ButtonComponent.ButtonAction.TOGGLE, check,
                    description = check.description
                )
            )
            
            buttons.add(
                ButtonComponent(
                    id + 1, contentLeft + toggleWidth + BUTTON_SPACING, y,
                    BUTTON_WIDTH, BUTTON_HEIGHT, "-", ButtonComponent.ButtonAction.DEC, check
                )
            )
            
            buttons.add(
                ButtonComponent(
                    id + 2, contentLeft + toggleWidth + BUTTON_WIDTH + BUTTON_SPACING * 2, y,
                    BUTTON_WIDTH, BUTTON_HEIGHT, "+", ButtonComponent.ButtonAction.INC, check
                )
            )
            
            val descLines = GuiUtil.wrapText(check.description, toggleWidth, fontRenderer).size
            val descriptionHeight = descLines * 10 + DESCRIPTION_PADDING
            
            y += BUTTON_HEIGHT + descriptionHeight + BUTTON_SPACING * 2
            contentHeight += BUTTON_HEIGHT + descriptionHeight + BUTTON_SPACING * 2
        }
        
        return contentHeight + CONTENT_PADDING + BOTTOM_PADDING
    }
}

