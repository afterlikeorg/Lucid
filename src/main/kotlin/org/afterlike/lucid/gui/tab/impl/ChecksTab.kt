package org.afterlike.lucid.gui.tab.impl

import org.afterlike.lucid.check.handler.CheckHandler
import org.afterlike.lucid.gui.component.impl.ButtonComponent
import org.afterlike.lucid.gui.tab.api.TabContent
import org.afterlike.lucid.gui.tab.api.TabContext
import org.afterlike.lucid.util.GuiUtil

object ChecksTab : TabContent {

    override val name = "Checks"

    private const val BUTTON_HEIGHT = 22
    private const val BUTTON_WIDTH = 22
    private const val BUTTON_SPACING = 4
    private const val DESCRIPTION_PADDING = 4
    private const val BOTTOM_PADDING = 15

    override fun initButtons(context: TabContext, buttons: MutableList<ButtonComponent>): Int {
        var y = context.contentStartY + context.contentPadding
        var contentHeight = context.contentPadding

        val toggleWidth = context.fullContentWidth - (2 * BUTTON_WIDTH + 2 * BUTTON_SPACING)

        CheckHandler.getChecks().forEachIndexed { index, check ->
            val id = 100 + index * 3

            val statusText = if (check.enabled) "§aON§r" else "§cOFF§r"
            val toggleLabel = "${check.name}: $statusText [VL:${check.violationLevelThreshold}]"

            buttons.add(
                ButtonComponent(
                    id, context.contentLeft, y, toggleWidth, BUTTON_HEIGHT,
                    toggleLabel, ButtonComponent.ButtonAction.TOGGLE, check,
                    description = check.description
                )
            )

            buttons.add(
                ButtonComponent(
                    id + 1, context.contentLeft + toggleWidth + BUTTON_SPACING, y,
                    BUTTON_WIDTH, BUTTON_HEIGHT, "-", ButtonComponent.ButtonAction.DEC, check
                )
            )

            buttons.add(
                ButtonComponent(
                    id + 2, context.contentLeft + toggleWidth + BUTTON_WIDTH + BUTTON_SPACING * 2, y,
                    BUTTON_WIDTH, BUTTON_HEIGHT, "+", ButtonComponent.ButtonAction.INC, check
                )
            )

            val descLines = GuiUtil.wrapText(check.description, toggleWidth, context.fontRenderer).size
            val descriptionHeight = descLines * 10 + DESCRIPTION_PADDING

            y += BUTTON_HEIGHT + descriptionHeight + BUTTON_SPACING * 2
            contentHeight += BUTTON_HEIGHT + descriptionHeight + BUTTON_SPACING * 2
        }

        return contentHeight + context.contentPadding + BOTTOM_PADDING
    }
}
