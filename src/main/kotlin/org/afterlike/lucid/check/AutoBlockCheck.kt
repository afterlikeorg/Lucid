package org.afterlike.lucid.check

import net.minecraft.entity.player.EntityPlayer
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

class AutoBlockCheck : Check() {
    override val name = "AutoBlock"
    override val description = "Detects attacking while blocking with a sword"

    private val swingProgress = ConcurrentHashMap<EntityPlayer, Float>()
    private val consecutiveViolations = ConcurrentHashMap<EntityPlayer, Int>()

    init {
        CheckManager.register(this)
        vlThreshold = 20
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
            
            // Calculate VL based on swing progress - more severe if further in the swing animation
            val vlMultiplier = when {
                swingValue > 0.7f -> 7.0  // Very severe - almost completed swing while blocking
                swingValue > 0.4f -> 6.0  // More severe
                swingValue > 0.2f -> 5.0  // Default
                else -> 4.0              // Less severe but still a violation
            }
            
            // Track consecutive violations for escalating penalties
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
            // Only decay if not violating in this tick
            if (getPlayerVL(target) > 0) {
                // Slower decay rate (0.25 instead of 0.5)
                decayVL(target, 0.25)
                
                // Reset consecutive violations counter after 40 ticks of no violations
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