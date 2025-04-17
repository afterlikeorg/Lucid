package org.afterlike.lucid.util

import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

class DelayedTask(private val ticks: Int, private val runnable: () -> Unit) {
    private var counter = ticks

    init {
        MinecraftForge.EVENT_BUS.register(this)
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START) return

        if (counter <= 0) {
            MinecraftForge.EVENT_BUS.unregister(this)
            runnable.invoke()
        } else {
            counter--
        }
    }
}
