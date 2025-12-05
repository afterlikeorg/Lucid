package org.afterlike.lucid.gui.tab.api

import org.afterlike.lucid.gui.component.impl.ButtonComponent

interface TabContent {
    val name: String

    fun initButtons(
        context: TabContext,
        buttons: MutableList<ButtonComponent>
    ): Int
}
