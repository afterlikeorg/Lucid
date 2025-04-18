package org.afterlike.lucid.check

import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemBlock
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class ScaffoldCheck : Check() {
    override val name = "Scaffold"
    override val description = "Detects illegal bridging patterns"
    
    private val consecutiveViolations = ConcurrentHashMap<EntityPlayer, Int>()
    private val lastViolationType = ConcurrentHashMap<EntityPlayer, String>()
    private val lastViolationTime = ConcurrentHashMap<EntityPlayer, Long>()

    init {
        CheckManager.register(this)
        vlThreshold = 12
    }

    override fun onUpdate(target: EntityPlayer) {
        val mc = Minecraft.getMinecraft()
        if (target === mc.thePlayer || target.isRiding) return

        val samples = getPlayerHistory(target)
        if (samples.size < 4) return

        val currentSample = samples.last()
        val tick = currentSample.tick
        val pitch = currentSample.pitch.toDouble()
        
        val dx = currentSample.deltaX * 20.0
        val dz = currentSample.deltaZ * 20.0
        val speedXZsq = dx*dx + dz*dz
        val speedXZ = sqrt(speedXZsq)
        
        val speedY = (samples[3].posY - samples[2].posY) * 20.0
        val avgAccelY = 50.0 * (samples[0].posY - samples[1].posY - samples[2].posY + samples[3].posY)
        
        val angleDiff = abs(getMoveLookAngleDiff(currentSample))

        var flagged = false
        var checkType = ""

        if (target.isSwingInProgress && target.hurtTime == 0 &&
            pitch > 50.0 && speedXZsq > 9.0 &&
            target.heldItem?.item is ItemBlock &&
            angleDiff > 165.0 && speedXZsq < 100.0 &&
            !isAlmostZero(avgAccelY)
        ) {
            // Calculate severity factors
            val pitchFactor = ((pitch - 50.0) / 40.0).coerceIn(0.0, 1.0)  // 50->0.0, 90->1.0
            val angleFactor = ((angleDiff - 165.0) / 15.0).coerceIn(0.0, 1.0)  // 165->0.0, 180->1.0
            val speedFactor = ((speedXZ - 3.0) / 7.0).coerceIn(0.0, 1.0)  // Higher speed is more suspicious
            
            // Calculate base VL using severity factors
            var baseVL = 0.0
            var severity = 0.0
            
            // Check for tower scaffold
            if (speedY in 4.0..15.0 && avgAccelY > -25.0) {
                checkType = "tower"
                
                // Calculate tower-specific severity
                val ySpeedFactor = ((speedY - 4.0) / 11.0).coerceIn(0.0, 1.0)  // 4->0.0, 15->1.0
                val accelFactor = ((avgAccelY + 25.0) / 25.0).coerceIn(0.0, 1.0)  // Higher accel is more suspicious
                
                // Weighted severity calculation
                severity = (pitchFactor * 0.3 + angleFactor * 0.3 + ySpeedFactor * 0.3 + accelFactor * 0.1).coerceIn(0.0, 1.0)
                baseVL = 3.0 + (severity * 3.0)  // Base 3.0, up to +3.0 for high severity
            }
            // Check for horizontal scaffold
            else if (speedY in -1.0..4.0 &&
                abs(speedY) > 0.005 &&
                speedXZsq > 25.0
            ) {
                checkType = "horizontal"
                
                // Calculate horizontal-specific severity
                val hSpeedFactor = ((speedXZ - 5.0) / 5.0).coerceIn(0.0, 1.0)  // How fast they're moving horizontally
                
                // Weighted severity calculation
                severity = (pitchFactor * 0.3 + angleFactor * 0.3 + hSpeedFactor * 0.4).coerceIn(0.0, 1.0)
                baseVL = 3.0 + (severity * 3.0)  // Base 3.0, up to +3.0 for high severity
            }
            
            // Apply violation if a check was triggered
            if (checkType.isNotEmpty()) {
                // Track consecutive violations of the same type
                val lastType = lastViolationType.getOrDefault(target, "")
                val lastTime = lastViolationTime.getOrDefault(target, 0L)
                
                var consecutive = 0
                if (lastType == checkType && (tick - lastTime) < 40) {
                    consecutive = consecutiveViolations.getOrDefault(target, 0) + 1
                } else {
                    consecutive = 0
                }
                consecutiveViolations[target] = consecutive
                lastViolationType[target] = checkType
                lastViolationTime[target] = tick
                
                // Apply consecutive multiplier (up to 2x for 5+ consecutive violations)
                val consecutiveMultiplier = 1.0 + (min(consecutive, 5) * 0.2)
                val finalVL = baseVL * consecutiveMultiplier
                
                val reason = buildStandardReason(
                    checkType,
                    pitch,
                    speedXZ,
                    angleDiff,
                    speedY,
                    avgAccelY,
                    severity,
                    consecutive,
                    finalVL
                )
                
                addVL(target, finalVL, reason)
                flagged = true
            }
        }

        if (!flagged) {
            val currentVL = getPlayerVL(target)
            if (currentVL > 0) {
                val decayRate = when {
                    currentVL > vlThreshold * 0.75 -> 0.75  // Faster decay at high VL
                    currentVL > vlThreshold * 0.5 -> 0.6   // Medium decay at medium VL
                    currentVL > vlThreshold * 0.25 -> 0.5  // Standard decay at lower VL
                    else -> 0.3                           // Slow decay at very low VL
                }
                
                decayVL(target, decayRate)
                
                // Reset consecutive violations counter after significant decay
                if (currentVL <= vlThreshold * 0.2 && tick - lastViolationTime.getOrDefault(target, 0L) > 60) {
                    consecutiveViolations[target] = 0
                }
            }
        }
    }

    private fun buildStandardReason(
        type: String,
        pitch: Double,
        speedXZ: Double,
        angleDiff: Double,
        speedY: Double,
        avgAccelY: Double,
        severity: Double,
        consecutive: Int,
        vlAmount: Double
    ): String {
        val itemName = mc.thePlayer.heldItem?.displayName ?: "unknown block"
        
        return "scaffold-$type | pitch=${"%.1f".format(pitch)}° | angle=${"%.1f".format(angleDiff)}° | " +
               "speedXZ=${"%.2f".format(speedXZ)} | speedY=${"%.2f".format(speedY)} | " + 
               "accelY=${"%.2f".format(avgAccelY)} | severity=${"%.2f".format(severity)} | " +
               "consecutive=$consecutive | vl=${"%.1f".format(vlAmount)} | item=$itemName"
    }

    private fun isAlmostZero(d: Double) = abs(d) < 0.001

    private fun getMoveLookAngleDiff(sample: PlayerSample): Double {
        val dx = sample.deltaX
        val dz = sample.deltaZ
        val move = Math.toDegrees(Math.atan2(dz, dx)) - 90.0
        val look = sample.yaw.toDouble()
        var diff = ((move - look) % 360.0 + 360.0) % 360.0
        if (diff > 180.0) diff -= 360.0
        return diff
    }
    
    override fun onPlayerRemove(player: EntityPlayer?) {
        if (player != null) {
            consecutiveViolations.remove(player)
            lastViolationType.remove(player)
            lastViolationTime.remove(player)
        } else {
            consecutiveViolations.clear()
            lastViolationType.clear()
            lastViolationTime.clear()
        }
        
        super.onPlayerRemove(player)
    }
}