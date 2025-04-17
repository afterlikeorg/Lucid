package org.afterlike.lucid.event

import org.afterlike.lucid.check.CheckManager

class EventHandler {

    fun registerEvents() {

        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(CheckManager)
    }
}