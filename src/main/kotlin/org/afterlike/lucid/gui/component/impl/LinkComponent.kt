package org.afterlike.lucid.gui.component.impl

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiConfirmOpenLink
import net.minecraft.client.gui.GuiScreen
import org.afterlike.lucid.gui.component.base.BaseComponent
import org.afterlike.lucid.util.ThemeUtil
import java.awt.Color

class LinkComponent(
    id: Int,
    x: Int,
    y: Int,
    val text: String,
    val url: String,
    val parentScreen: GuiScreen,
    val centered: Boolean = true
) : BaseComponent(id, x, y, 0, 8) {

    private val mc = Minecraft.getMinecraft()
    private var actualX = x
    private var textWidth = 0

    init {
        textWidth = mc.fontRendererObj.getStringWidth(text)
        actualX = if (centered) x - textWidth / 2 else x
    }

    override fun isMouseOver(mouseX: Int, mouseY: Int, scrollOffset: Int): Boolean {
        return mouseX >= actualX && mouseX <= actualX + textWidth &&
                mouseY >= y && mouseY <= y + height
    }

    override fun draw(theme: ThemeUtil) {
        val color = if (isHovered) Color(100, 180, 255) else theme.textSecondaryColor
        mc.fontRendererObj.drawString(
            text,
            actualX.toFloat(),
            visualY.toFloat(),
            color.rgb,
            false
        )
    }

    fun onClick() {
        val confirmGui = GuiConfirmOpenLink(parentScreen, url, 0, true)
        mc.displayGuiScreen(confirmGui)
    }
}
