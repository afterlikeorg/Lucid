package org.afterlike.lucid.check

import net.minecraft.entity.player.EntityPlayer
import java.util.concurrent.ConcurrentHashMap

class AutoBlockCheck : Check() {
    override val name = "AutoBlock"
    override val description = "Detects attacking while blocking with a sword"

    private val swingProgress = ConcurrentHashMap<EntityPlayer, Float>()
    private val consecutiveViolations = ConcurrentHashMap<EntityPlayer, Int>()

    init {
        CheckManager.register(this)
        vlThreshold = 16
    }

    override fun onUpdate(target: EntityPlayer) {
        if (target == mc.thePlayer) return

        val currentSample = getPlayerSample(target) ?: return

        val isBlocking = target.isBlocking
        val current = target.swingProgress
        val previous = swingProgress[target] ?: 0f

        if (isBlocking && current > 0f && previous == 0f) {
            val heldItemName = target.heldItem?.displayName ?: "unknown item"
            val swingValue = current

            val vlMultiplier = when {
                swingValue > 0.7f -> 7.0
                swingValue > 0.4f -> 6.0
                swingValue > 0.2f -> 5.0
                else -> 4.0
            }

            // Track consecutive violations
            val consecutive = consecutiveViolations.getOrDefault(target, 0) + 1
            consecutiveViolations[target] = consecutive

            // Increase VL for repeated violations
            val finalVL = vlMultiplier * (1.0 + (consecutive * 0.1).coerceAtMost(1.0))

            addVL(
                target,
                finalVL,
                "autoblock | item=$heldItemName | swing=${"%.2f".format(swingValue)} | blocking=true | consecutive=$consecutive"
            )
        } else {
            if (getPlayerVL(target) > 0) {
                decayVL(target, 0.5)

                val currentVL = getPlayerVL(target)
                if (currentVL <= vlThreshold * 0.2) {
                    consecutiveViolations[target] = 0
                }
            }
        }

        swingProgress[target] = current
    }

    override fun onPlayerRemove(player: EntityPlayer?) {
        if (player != null) {
            swingProgress.remove(player)
            consecutiveViolations.remove(player)
        } else {
            swingProgress.clear()
            consecutiveViolations.clear()
        }

        super.onPlayerRemove(player)
    }
} 