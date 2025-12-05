package org.afterlike.lucid.gui.tab.api

import net.minecraft.client.gui.FontRenderer

data class TabContext(
    val panelLeft: Int,
    val panelTop: Int,
    val headerHeight: Int,
    val tabHeight: Int,
    val contentPadding: Int,
    val fullContentWidth: Int,
    val settingContentWidth: Int,
    val fontRenderer: FontRenderer
) {
    val contentLeft: Int get() = panelLeft + contentPadding
    val contentStartY: Int get() = panelTop + headerHeight + tabHeight
}