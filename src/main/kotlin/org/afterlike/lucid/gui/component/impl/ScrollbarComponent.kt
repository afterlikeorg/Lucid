package org.afterlike.lucid.gui.component.impl

import net.minecraft.client.gui.Gui
import org.afterlike.lucid.gui.component.base.BaseComponent
import org.afterlike.lucid.util.ThemeUtil
import kotlin.math.max

class ScrollbarComponent(
    id: Int,
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    private val scrollOffset: () -> Int,
    private val maxScrollOffset: () -> Int,
    private val contentHeight: () -> Int
) : BaseComponent(id, x, y, width, height) {

    private val trackPadding = 5

    override fun isMouseOver(mouseX: Int, mouseY: Int, scrollOffset: Int): Boolean {
        return false
    }

    override fun draw(theme: ThemeUtil) {
        val trackTop = y + trackPadding
        val trackBottom = y + height - trackPadding
        val trackHeight = trackBottom - trackTop

        val scrollbarHeight = max(30, trackHeight * trackHeight / contentHeight())
        val scrollProgress = if (maxScrollOffset() > 0) {
            scrollOffset().toFloat() / maxScrollOffset().toFloat()
        } else {
            0f
        }
        val scrollbarY = trackTop + ((trackHeight - scrollbarHeight) * scrollProgress).toInt()

        Gui.drawRect(x, trackTop, x + width, trackBottom, theme.scrollTrackColor.rgb)
        Gui.drawRect(x, scrollbarY, x + width, scrollbarY + scrollbarHeight, theme.scrollThumbColor.rgb)
    }
}

