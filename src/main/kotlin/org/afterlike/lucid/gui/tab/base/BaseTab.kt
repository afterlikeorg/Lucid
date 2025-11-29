package org.afterlike.lucid.gui.tab.base

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Gui
import org.afterlike.lucid.gui.component.base.BaseComponent
import org.afterlike.lucid.util.ThemeUtil

class BaseTab(
    id: Int,
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    val text: String,
    val action: TabAction,
    val isActive: Boolean = false
) : BaseComponent(id, x, y, width, height) {

    override fun isMouseOver(mouseX: Int, mouseY: Int, scrollOffset: Int): Boolean {
        return !isActive && mouseX >= x && mouseX <= x + width &&
                mouseY >= y && mouseY <= y + height
    }

    override fun draw(theme: ThemeUtil) {
        val color = if (isActive) theme.activeTabColor else theme.buttonColor
        val textColor = theme.textColor
        val borderColor = if (isHovered) theme.accentColor else theme.borderColor

        Gui.drawRect(x, visualY, x + width, visualY + height, color.rgb)

        if (isActive) {
            Gui.drawRect(x, visualY, x + width, visualY + 1, borderColor.rgb)
            Gui.drawRect(x + width - 1, visualY, x + width, visualY + height, borderColor.rgb)
            Gui.drawRect(x, visualY + height - 1, x + width, visualY + height, borderColor.rgb)
            Gui.drawRect(x, visualY, x + 1, visualY + height, borderColor.rgb)
        } else {
            Gui.drawRect(x, visualY + height - 1, x + width, visualY + height, borderColor.rgb)
        }

        val mc = Minecraft.getMinecraft()
        val textWidth = mc.fontRendererObj.getStringWidth(text)
        mc.fontRendererObj.drawStringWithShadow(
            text,
            (x + (width - textWidth) / 2).toFloat(),
            (visualY + (height - 8) / 2).toFloat(),
            textColor.rgb
        )
    }

    enum class TabAction {
        CHECKS, APPEARANCE, SETTINGS
    }
}