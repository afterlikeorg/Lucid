package org.afterlike.lucid.gui.component.impl

import net.minecraft.client.Minecraft
import org.afterlike.lucid.gui.component.base.BaseComponent
import org.afterlike.lucid.util.ThemeUtil
import java.awt.Color

class CloseComponent(
    id: Int,
    x: Int,
    y: Int,
    size: Int
) : BaseComponent(id, x, y, size, size) {

    override fun isMouseOver(mouseX: Int, mouseY: Int, scrollOffset: Int): Boolean {
        return mouseX >= x && mouseX <= x + width &&
                mouseY >= y && mouseY <= y + height
    }

    override fun draw(theme: ThemeUtil) {
        val xColor = if (isHovered) Color(255, 100, 100).rgb else Color(200, 200, 200).rgb
        val mc = Minecraft.getMinecraft()
        val fontRenderer = mc.fontRendererObj
        fontRenderer.drawString(
            "✕",
            (x + width / 2 - fontRenderer.getStringWidth("✕") / 2).toFloat(),
            (visualY + (height - 8) / 2).toFloat(),
            xColor,
            false
        )
    }
}

