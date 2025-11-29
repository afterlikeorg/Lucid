package org.afterlike.lucid.gui.component.base

import org.afterlike.lucid.util.ThemeUtil

abstract class BaseComponent(
    val id: Int,
    val x: Int,
    var y: Int,
    val width: Int,
    val height: Int
) {
    var isHovered = false
    var visualY: Int = y

    abstract fun isMouseOver(mouseX: Int, mouseY: Int, scrollOffset: Int = 0): Boolean
    abstract fun draw(theme: ThemeUtil)
}