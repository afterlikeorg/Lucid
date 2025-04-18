package org.afterlike.lucid.check

import net.minecraft.entity.player.EntityPlayer
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class RotationCheck : Check() {
    override val name = "Rotation"
    override val description = "Detects illegal rotations and rotation patterns"

    private val lastPitchChanges = ConcurrentHashMap<EntityPlayer, MutableList<Float>>()
    private val lastYawChanges = ConcurrentHashMap<EntityPlayer, MutableList<Float>>()
    private val consecutiveViolations = ConcurrentHashMap<EntityPlayer, Int>()
    private val lastViolationType = ConcurrentHashMap<EntityPlayer, String>()
    private val lastViolationTime = ConcurrentHashMap<EntityPlayer, Long>()

    init {
        CheckManager.register(this)
        vlThreshold = 8
    }

    override fun onPlayerRemove(player: EntityPlayer?) {
        if (player != null) {
            lastPitchChanges.remove(player)
            lastYawChanges.remove(player)
            consecutiveViolations.remove(player)
            lastViolationType.remove(player)
            lastViolationTime.remove(player)
        } else {
            lastPitchChanges.clear()
            lastYawChanges.clear()
            consecutiveViolations.clear()
            lastViolationType.clear()
            lastViolationTime.clear()
        }

        super.onPlayerRemove(player)
    }

    override fun onUpdate(target: EntityPlayer) {
        try {
            if (target == mc.thePlayer) return

            val currentSample = getPlayerSample(target) ?: return
            val prevSample = getPreviousSample(target) ?: return

            val pitch = currentSample.pitch
            val prevPitch = currentSample.prevPitch
            val yaw = currentSample.yaw
            val prevYaw = currentSample.prevYaw

            val pitchDelta = abs(pitch - prevPitch)
            val yawDelta = abs(yaw - prevYaw)

            var pitchChanges = lastPitchChanges[target]
            if (pitchChanges == null) {
                pitchChanges = mutableListOf()
                lastPitchChanges[target] = pitchChanges
            }

            var yawChanges = lastYawChanges[target]
            if (yawChanges == null) {
                yawChanges = mutableListOf()
                lastYawChanges[target] = yawChanges
            }

            pitchChanges.add(0, pitchDelta)
            yawChanges.add(0, yawDelta)
            if (pitchChanges.size > 20) pitchChanges.removeAt(pitchChanges.size - 1)
            if (yawChanges.size > 20) yawChanges.removeAt(yawChanges.size - 1)

            var flagged = false
            var checkType = ""
            var vlAmount = 0.0

            if (abs(pitch) > 90) {
                val pitchExcess = (abs(pitch) - 90.0) / 90.0 * 100.0 // as a percentage of max
                
                vlAmount = when {
                    abs(pitch) > 120 -> 8.0 + (pitchExcess * 0.05).coerceAtMost(5.0) // Up to +5.0 for extreme values
                    abs(pitch) > 100 -> 6.0 + (pitchExcess * 0.03).coerceAtMost(2.0) // Up to +2.0 for high values
                    else -> 4.5 + (pitchExcess * 0.01).coerceAtMost(1.0) // Up to +1.0 for moderate values
                }

                checkType = "illegal-pitch"
                val severity = when {
                    abs(pitch) > 120 -> "extreme"
                    abs(pitch) > 100 -> "high"
                    else -> "medium"
                }
                
                val lastType = lastViolationType.getOrDefault(target, "")
                val lastTime = lastViolationTime.getOrDefault(target, 0L)
                val currentTime = currentSample.tick
                
                var consecutive = 0
                if (lastType == checkType && (currentTime - lastTime) < 20) {
                    consecutive = consecutiveViolations.getOrDefault(target, 0) + 1
                }
                consecutiveViolations[target] = consecutive
                lastViolationType[target] = checkType
                lastViolationTime[target] = currentTime
                
                // Apply consecutive multiplier (up to 2x for 5+ consecutive violations)
                val consecutiveMultiplier = 1.0 + (min(consecutive, 5) * 0.2)
                val finalVL = vlAmount * consecutiveMultiplier
                
                addVL(
                    target, 
                    finalVL, 
                    "rotation-$checkType | pitch=${"%.1f".format(pitch)}° | severity=$severity | limit=±90° | " +
                    "excess=${"%.1f".format(pitchExcess)}% | consecutive=$consecutive | vl=${"%.1f".format(finalVL)}"
                )
                flagged = true
                
            } 
            // Check for unnatural rotation speeds
            else if ((yawDelta > 40 || pitchDelta > 30) && prevYaw != 0f && prevPitch != 0f) {
                // Check for consistent patterns in rotation history
                val isConsistent = checkConsistency(pitchChanges, yawChanges)
                
                // Calculate severity scores based on yaw and pitch speeds
                val yawSeverity = ((yawDelta - 40) / 60.0).coerceIn(0.0, 1.0) // 40->0.0, 100->1.0
                val pitchSeverity = ((pitchDelta - 30) / 50.0).coerceIn(0.0, 1.0) // 30->0.0, 80->1.0
                val combinedSeverity = max(yawSeverity, pitchSeverity) * 0.7 + (yawSeverity + pitchSeverity) / 2.0 * 0.3
                
                // Base VL based on detected pattern and severity
                vlAmount = when {
                    yawDelta > 70 && pitchDelta > 50 -> 5.0 + combinedSeverity * 3.0 // Up to +3.0 for extreme values
                    isConsistent && (yawDelta > 45 || pitchDelta > 35) -> 4.0 + combinedSeverity * 2.0 // Up to +2.0
                    else -> 3.0 + combinedSeverity * 1.0 // Up to +1.0
                }

                checkType = if (isConsistent) "aimbot-pattern" else "snap-rotation"
                
                // Track consecutive violations of the same type
                val lastType = lastViolationType.getOrDefault(target, "")
                val lastTime = lastViolationTime.getOrDefault(target, 0L)
                val currentTime = currentSample.tick
                
                var consecutive = 0
                if (lastType == checkType && (currentTime - lastTime) < 20) {
                    consecutive = consecutiveViolations.getOrDefault(target, 0) + 1
                } else {
                    consecutive = 0
                }
                consecutiveViolations[target] = consecutive
                lastViolationType[target] = checkType
                lastViolationTime[target] = currentTime
                
                // Apply consecutive multiplier (up to 2x for 5+ consecutive violations)
                val consecutiveMultiplier = 1.0 + (min(consecutive, 5) * 0.2)
                val finalVL = vlAmount * consecutiveMultiplier
                
                // Get detailed consistency metrics for more informative alerts
                val consistencyInfo = getConsistencyMetrics(pitchChanges, yawChanges)
                
                addVL(
                    target, 
                    finalVL, 
                    "rotation-$checkType | yawSpeed=${"%.1f".format(yawDelta)}° | pitchSpeed=${"%.1f".format(pitchDelta)}° | " +
                    "severity=${"%.2f".format(combinedSeverity)} | $consistencyInfo | consecutive=$consecutive | vl=${"%.1f".format(finalVL)}"
                )
                flagged = true
            }

            if (!flagged && getPlayerVL(target) > 0) {
                // Get current VL for decay calculation
                val currentVL = getPlayerVL(target)
                
                val decayRate = when {
                    currentVL > vlThreshold * 0.8 -> 1.5  // Fast decay at high VL
                    currentVL > vlThreshold * 0.5 -> 1.2  // Medium decay at medium VL
                    currentVL > vlThreshold * 0.2 -> 0.8  // Slower decay at lower VL
                    else -> 0.5                           // Very slow decay at very low VL
                }
                
                decayVL(target, decayRate)
                
                // Reset consecutive violations after significant decay
                if (currentVL <= vlThreshold * 0.2) {
                    consecutiveViolations[target] = 0
                }
            }
        } catch (e: Exception) {
            logError("Error in RotationCheck: ${e.message}")
        }
    }

    private fun checkConsistency(pitchChanges: List<Float>, yawChanges: List<Float>): Boolean {
        if (pitchChanges.size < 5 || yawChanges.size < 5) return false

        val recentPitch = pitchChanges.take(5)
        val recentYaw = yawChanges.take(5)

        val pitchPattern = recentPitch.count { it > 25 } >= 3
        val yawPattern = recentYaw.count { it > 35 } >= 3

        val yawMean = recentYaw.average()
        val pitchMean = recentPitch.average()

        val yawVariance = recentYaw.map { (it - yawMean) * (it - yawMean) }.average()
        val pitchVariance = recentPitch.map { (it - pitchMean) * (it - pitchMean) }.average()

        val lowVariance = yawVariance < 12 || pitchVariance < 6

        return (pitchPattern && yawPattern) || lowVariance
    }
    
    private fun getConsistencyMetrics(pitchChanges: List<Float>, yawChanges: List<Float>): String {
        if (pitchChanges.size < 5 || yawChanges.size < 5) return "consistent=false"

        val recentPitch = pitchChanges.take(5)
        val recentYaw = yawChanges.take(5)

        val yawMean = recentYaw.average()
        val pitchMean = recentPitch.average()

        val yawVariance = recentYaw.map { (it - yawMean) * (it - yawMean) }.average()
        val pitchVariance = recentPitch.map { (it - pitchMean) * (it - pitchMean) }.average()

        val pitchPattern = recentPitch.count { it > 25 } >= 3
        val yawPattern = recentYaw.count { it > 35 } >= 3
        val lowVariance = yawVariance < 12 || pitchVariance < 6

        return "consistent=${(pitchPattern && yawPattern) || lowVariance} | " +
               "yawVar=${"%.1f".format(yawVariance)} | pitchVar=${"%.1f".format(pitchVariance)}"
    }
} 