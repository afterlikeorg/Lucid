package org.afterlike.lucid.check.impl

import net.minecraft.entity.player.EntityPlayer
import org.afterlike.lucid.check.api.AbstractCheck
import org.afterlike.lucid.core.event.world.EntityLeaveEvent
import org.afterlike.lucid.data.handler.impl.PlayerHandler
import java.util.concurrent.ConcurrentHashMap

class AutoBlockCheck : AbstractCheck() {
    override val name = "AutoBlock"
    override val description = "Detects attacking while blocking with a sword"
    override var violationLevelThreshold = 16

    override val decayConfig = DecayConfig(
        baseRate = 0.5,
        mediumRate = 0.8,
        highRate = 1.2,
        criticalRate = 1.5,
        resetThreshold = 0.2
    )

    private val consecutiveViolations = ConcurrentHashMap<EntityPlayer, Int>()

    // TODO: this needs to be rewritten for blink autoblocks, false flags too much.
    override fun onCheckRun(target: EntityPlayer) {
        if (target == mc.thePlayer) return

        val data = PlayerHandler.get(target) ?: return
        val ctx = data.player

        val isBlocking = ctx.isBlocking
        val current = ctx.swingProgress
        val previous = ctx.prevSwingProgress

        if (isBlocking && current > 0f && previous == 0f) {
            val heldItemName = target.heldItem?.displayName ?: "unknown"

            val vlMultiplier = when {
                current > 0.7f -> 7.0
                current > 0.4f -> 6.0
                current > 0.2f -> 5.0
                else -> 4.0
            }

            val consecutive = consecutiveViolations.getOrDefault(target, 0) + 1
            consecutiveViolations[target] = consecutive

            val finalVL = vlMultiplier * (1.0 + (consecutive * 0.1).coerceAtMost(1.0))

            addVL(
                target,
                finalVL,
                "autoblock | item=$heldItemName | swing=${"%.2f".format(current)} | blocking=true | consecutive=$consecutive"
            )
        } else {
            if (handleNoViolation(target)) {
                consecutiveViolations[target] = 0
            }
        }
    }

    override fun onPlayerLeave(event: EntityLeaveEvent) {
        consecutiveViolations.remove(event.entity)
    }
}
