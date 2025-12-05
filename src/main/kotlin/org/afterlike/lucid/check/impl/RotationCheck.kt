package org.afterlike.lucid.check.impl

import net.minecraft.entity.player.EntityPlayer
import org.afterlike.lucid.check.api.BaseCheck
import org.afterlike.lucid.core.event.world.EntityLeaveEvent
import org.afterlike.lucid.core.handler.PlayerSampleHandler
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class RotationCheck : BaseCheck() {
    override val name = "Rotation"
    override val description = "Detects illegal rotations and unnatural patterns"
    override var violationLevelThreshold = 16

    private val lastPitchChanges = ConcurrentHashMap<EntityPlayer, MutableList<Float>>()
    private val lastYawChanges = ConcurrentHashMap<EntityPlayer, MutableList<Float>>()
    private val consecutiveViolations = ConcurrentHashMap<EntityPlayer, Int>()
    private val lastViolationType = ConcurrentHashMap<EntityPlayer, String>()
    private val lastViolationTime = ConcurrentHashMap<EntityPlayer, Long>()

    override fun onPlayerLeave(event: EntityLeaveEvent) {
        val player = event.entity

        lastPitchChanges.remove(player)
        lastYawChanges.remove(player)
        consecutiveViolations.remove(player)
        lastViolationType.remove(player)
        lastViolationTime.remove(player)
    }

    override fun onCheckRun(target: EntityPlayer) {
        val currentSample = PlayerSampleHandler.getLatestSample(target) ?: return

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
        val checkType: String
        val vlAmount: Double

        if (abs(pitch) > 90) {
            val pitchExcess = (abs(pitch) - 90.0) / 90.0 * 100.0

            vlAmount = when {
                abs(pitch) > 120 -> 8.0 + (pitchExcess * 0.05).coerceAtMost(5.0)
                abs(pitch) > 100 -> 6.0 + (pitchExcess * 0.03).coerceAtMost(2.0)
                else -> 4.5 + (pitchExcess * 0.01).coerceAtMost(1.0)
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

            val consecutiveMultiplier = 1.0 + (min(consecutive, 5) * 0.2)
            val finalVL = vlAmount * consecutiveMultiplier

            addVL(
                target,
                finalVL,
                "rotation-$checkType | pitch=${"%.1f".format(pitch)}° | severity=$severity | limit=±90° | " +
                        "excess=${"%.1f".format(pitchExcess)}% | consecutive=$consecutive | vl=${
                            "%.1f".format(
                                finalVL
                            )
                        }"
            )
            flagged = true

        } else if ((yawDelta > 40 || pitchDelta > 30) && prevYaw != 0f && prevPitch != 0f) {
            val isConsistent = checkConsistency(pitchChanges, yawChanges)

            val yawSeverity = ((yawDelta - 40) / 60.0).coerceIn(0.0, 1.0)
            val pitchSeverity = ((pitchDelta - 30) / 50.0).coerceIn(0.0, 1.0)
            val combinedSeverity = max(yawSeverity, pitchSeverity) * 0.7 + (yawSeverity + pitchSeverity) / 2.0 * 0.3

            vlAmount = when {
                yawDelta > 70 && pitchDelta > 50 -> 5.0 + combinedSeverity * 3.0
                isConsistent && (yawDelta > 45 || pitchDelta > 35) -> 4.0 + combinedSeverity * 2.0
                else -> 3.0 + combinedSeverity * 1.0
            }

            checkType = if (isConsistent) "aimbot-pattern" else "snap-rotation"

            val lastType = lastViolationType.getOrDefault(target, "")
            val lastTime = lastViolationTime.getOrDefault(target, 0L)
            val currentTime = currentSample.tick
            val consecutive: Int = if (lastType == checkType && (currentTime - lastTime) < 20) {
                consecutiveViolations.getOrDefault(target, 0) + 1
            } else {
                0
            }
            consecutiveViolations[target] = consecutive
            lastViolationType[target] = checkType
            lastViolationTime[target] = currentTime

            val consecutiveMultiplier = 1.0 + (min(consecutive, 5) * 0.2)
            val finalVL = vlAmount * consecutiveMultiplier

            val consistencyInfo = getConsistencyMetrics(pitchChanges, yawChanges)

            addVL(
                target,
                finalVL,
                "rotation-$checkType | yawSpeed=${"%.1f".format(yawDelta)}° | pitchSpeed=${"%.1f".format(pitchDelta)}° | " +
                        "severity=${"%.2f".format(combinedSeverity)} | $consistencyInfo | consecutive=$consecutive | vl=${
                            "%.1f".format(
                                finalVL
                            )
                        }"
            )
            flagged = true
        }

        if (!flagged && getPlayerVL(target) > 0) {
            val currentVL = getPlayerVL(target)

            val decayRate = when {
                currentVL > violationLevelThreshold * 0.8 -> 1.5
                currentVL > violationLevelThreshold * 0.5 -> 1.2
                currentVL > violationLevelThreshold * 0.2 -> 0.8
                else -> 0.5
            }

            decayVL(target, decayRate)

            if (currentVL <= violationLevelThreshold * 0.2) {
                consecutiveViolations[target] = 0
            }
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