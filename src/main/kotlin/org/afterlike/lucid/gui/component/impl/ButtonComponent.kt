package org.afterlike.lucid.gui.component.impl

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Gui
import org.afterlike.lucid.check.api.AbstractCheck
import org.afterlike.lucid.gui.component.base.BaseComponent
import org.afterlike.lucid.util.ThemeUtil
import java.awt.Color

class ButtonComponent(
    id: Int,
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    val text: String,
    val action: ButtonAction,
    val check: AbstractCheck? = null,
    val enabled: Boolean = true,
    val description: String? = null
) : BaseComponent(id, x, y, width, height) {

    override fun isMouseOver(mouseX: Int, mouseY: Int, scrollOffset: Int): Boolean {
        val effectiveY = if (id >= 100) y - scrollOffset else y
        return enabled && mouseX >= x && mouseX <= x + width &&
                mouseY >= effectiveY && mouseY <= effectiveY + height
    }

    override fun draw(theme: ThemeUtil) {
        val color = when {
            !enabled -> theme.accentColorDisabled
            isHovered -> theme.buttonHoverColor
            else -> theme.buttonColor
        }

        val textColor = if (!enabled) Color(190, 190, 190) else theme.textColor
        val borderColor = if (isHovered && enabled) theme.accentColor else theme.borderColor

        Gui.drawRect(x, visualY, x + width, visualY + height, color.rgb)

        Gui.drawRect(x, visualY, x + width, visualY + 1, borderColor.rgb)
        Gui.drawRect(x + width - 1, visualY, x + width, visualY + height, borderColor.rgb)
        Gui.drawRect(x, visualY + height - 1, x + width, visualY + height, borderColor.rgb)
        Gui.drawRect(x, visualY, x + 1, visualY + height, borderColor.rgb)

        val mc = Minecraft.getMinecraft()
        val textWidth = mc.fontRendererObj.getStringWidth(text)
        mc.fontRendererObj.drawStringWithShadow(
            text,
            (x + (width - textWidth) / 2).toFloat(),
            (visualY + (height - 8) / 2).toFloat(),
            textColor.rgb
        )
    }

    enum class ButtonAction {
        TOGGLE, DEC, INC,
        TOGGLE_SOUND, TOGGLE_VERBOSE, TOGGLE_DEBUG,
        COOLDOWN_DEC, COOLDOWN_INC,
        COLOR_PREV, COLOR_NEXT,
        TOGGLE_BOLD,
        SYMBOL_TOGGLE,
        TOGGLE_SHOW_VL,
        TOGGLE_SHOW_WDR
    }
}
