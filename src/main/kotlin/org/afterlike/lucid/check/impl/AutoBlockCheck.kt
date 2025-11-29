package org.afterlike.lucid.check.impl

import net.minecraft.entity.player.EntityPlayer
import org.afterlike.lucid.check.api.BaseCheck
import org.afterlike.lucid.core.event.world.EntityLeaveEvent
import java.util.concurrent.ConcurrentHashMap

class AutoBlockCheck : BaseCheck() {
    override val name = "AutoBlock"
    override val description = "Detects attacking while blocking with a sword"
    override var violationLevelThreshold = 16

    private val swingProgress = ConcurrentHashMap<EntityPlayer, Float>()
    private val consecutiveViolations = ConcurrentHashMap<EntityPlayer, Int>()

    override fun onCheckRun(target: EntityPlayer) {
        if (target == mc.thePlayer) return

        val isBlocking = target.isBlocking
        val current = target.swingProgress
        val previous = swingProgress[target] ?: 0f

        if (isBlocking && current > 0f && previous == 0f) {
            val heldItemName = target.heldItem?.displayName ?: "unknown item"

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
            if (getPlayerVL(target) > 0) {
                decayVL(target, 0.5)

                val currentVL = getPlayerVL(target)
                if (currentVL <= violationLevelThreshold * 0.2) {
                    consecutiveViolations[target] = 0
                }
            }
        }

        swingProgress[target] = current
    }

    override fun onPlayerLeave(event: EntityLeaveEvent) {
        val player = event.entity

        swingProgress.remove(player)
        consecutiveViolations.remove(player)
    }
} 