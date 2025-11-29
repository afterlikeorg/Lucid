package org.afterlike.lucid.core.type

import org.afterlike.lucid.core.event.game.GameTickEvent

class DelayedTask(private var ticks: Int, private val runnable: () -> Unit) {

    fun tick(event: GameTickEvent): Boolean {
        if (event.phase != EventPhase.PRE) return false

        if (ticks-- <= 0) {
            runnable()
            return true
        }

        return false
    }
}