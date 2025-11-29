package org.afterlike.lucid.core.handler

import best.azura.eventbus.handler.EventHandler
import org.afterlike.lucid.core.event.game.GameTickEvent
import org.afterlike.lucid.core.type.DelayedTask

object DelayedTaskHandler {

    private val tasks = mutableListOf<DelayedTask>()

    fun schedule(ticks: Int, block: () -> Unit) {
        tasks.add(DelayedTask(ticks, block))
    }

    @EventHandler
    fun onTick(event: GameTickEvent) {
        val iterator = tasks.iterator()

        while (iterator.hasNext()) {
            val task = iterator.next()
            if (task.tick(event)) {
                iterator.remove()
            }
        }
    }
}