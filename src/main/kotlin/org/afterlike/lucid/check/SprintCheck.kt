package org.afterlike.lucid.check

import net.minecraft.entity.player.EntityPlayer
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class SprintCheck : Check() {
    override val name = "Sprint"
    override val description = "Detects omnidirectional sprinting (backwards/sideways)"
    
    private val consecutiveViolations = ConcurrentHashMap<EntityPlayer, Int>()
    private val lastViolationTime = ConcurrentHashMap<EntityPlayer, Long>()

    init {
        CheckManager.register(this)
        vlThreshold = 8
    }

    override fun onUpdate(target: EntityPlayer) {
        if (target == mc.thePlayer || target.ridingEntity != null) return

        val current = getPlayerSample(target) ?: return
        val prev = getPreviousSample(target) ?: return
        val tick = current.tick

        val isSprinting = current.isSprinting
        val isGroundCollision = current.onGround

        val deltaX = current.deltaX
        val deltaZ = current.deltaZ

        val speed = max(abs(deltaX), abs(deltaZ))
        val moveYaw = getRelativeMoveAngle(deltaX, deltaZ, current.yaw)

        if (isSprinting && isGroundCollision && abs(moveYaw) > 90 && speed >= 0.2) {
            val angleDeviation = abs(abs(moveYaw) - 90.0) / 90.0  // How far beyond 90° (0.0 to 1.0)
            val speedFactor = ((speed - 0.2) / 0.3).coerceIn(0.0, 1.0)  // Speed factor (0.0 to 1.0)
            
            // Calculate base VL based on angle
            val baseVlByAngle = when {
                abs(moveYaw) > 160 -> 2.0
                abs(moveYaw) > 120 -> 1.5
                else -> 1.0
            }
            
            // Apply speed multiplier
            val speedMultiplier = 1.0 + (speedFactor * 0.5)
            
            // Apply angle deviation multiplier
            val angleMultiplier = 1.0 + (angleDeviation * 0.5)
            
            // Track consecutive violations
            val lastTime = lastViolationTime.getOrDefault(target, 0L)
            var consecutive = 0
            if (tick - lastTime < 40) {  // Within 2 seconds
                consecutive = consecutiveViolations.getOrDefault(target, 0) + 1
            } else {
                consecutive = 0
            }
            consecutiveViolations[target] = consecutive
            lastViolationTime[target] = tick
            
            // Apply consecutive multiplier (up to 2x for 5+ consecutive)
            val consecutiveMultiplier = 1.0 + (min(consecutive, 5) * 0.2)
            
            val finalVL = baseVlByAngle * speedMultiplier * angleMultiplier * consecutiveMultiplier

            val moveAngleType = when {
                abs(moveYaw) > 160 -> "backwards"
                abs(moveYaw) > 120 -> "sideways"
                else -> "illegal angle"
            }

            addVL(
                target, 
                finalVL, 
                "omnidirectional sprint | angle=${"%.1f".format(moveYaw)}° ($moveAngleType) | " +
                "speed=${"%.2f".format(speed)} | angleDev=${"%.2f".format(angleDeviation)} | " +
                "consecutive=$consecutive | vl=${"%.1f".format(finalVL)}"
            )
        } else {
            if (getPlayerVL(target) > 0) {
                val currentVL = getPlayerVL(target)
                
                val decayRate = when {
                    currentVL > vlThreshold * 0.7 -> 0.75  // Faster decay at high VL
                    currentVL > vlThreshold * 0.4 -> 0.6   // Medium decay at medium VL
                    else -> 0.4                          // Slower decay at low VL
                }
                
                decayVL(target, decayRate)
                
                // Reset consecutive violations after significant decay
                if (currentVL <= vlThreshold * 0.2 && tick - lastViolationTime.getOrDefault(target, 0L) > 60) {
                    consecutiveViolations[target] = 0
                }
            }
        }
    }
    
    override fun onPlayerRemove(player: EntityPlayer?) {
        if (player != null) {
            consecutiveViolations.remove(player)
            lastViolationTime.remove(player)
        } else {
            consecutiveViolations.clear()
            lastViolationTime.clear()
        }
        
        super.onPlayerRemove(player)
    }
} 