package org.afterlike.lucid.check

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemBlock
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class EagleCheck : Check() {
    override val name = "Eagle"
    override val description = "Detects mechanical patterns commonly used in speed-bridging cheats"

    private val lastCrouchStart = ConcurrentHashMap<EntityPlayer, Long>()
    private val lastCrouchEnd = ConcurrentHashMap<EntityPlayer, Long>()
    private val wasSneaking = ConcurrentHashMap<EntityPlayer, Boolean>()
    private val lastSwingTick = ConcurrentHashMap<EntityPlayer, Long>()
    private val patternCount = ConcurrentHashMap<EntityPlayer, Int>()
    private val lastPatternTick = ConcurrentHashMap<EntityPlayer, Long>()
    private val lastPositions = ConcurrentHashMap<EntityPlayer, Pair<Double, Double>>()
    private val lastMoveYaw = ConcurrentHashMap<EntityPlayer, Float>()
    private val crouchDurations = ConcurrentHashMap<EntityPlayer, MutableList<Int>>()
    private val consecutiveViolations = ConcurrentHashMap<EntityPlayer, Int>()

    init {
        CheckManager.register(this)
        vlThreshold = 10
    }

    override fun onUpdate(target: EntityPlayer) {
        if (target === mc.thePlayer) return

        val currentSample = getPlayerSample(target) ?: return
        val tick = currentSample.tick

        val prevSneak = wasSneaking.getOrDefault(target, false)
        val currSneak = target.isSneaking
        if (currSneak && !prevSneak) {
            lastCrouchStart[target] = tick
        }
        if (!currSneak && prevSneak) {
            lastCrouchEnd[target] = tick

            val durations = crouchDurations.getOrDefault(target, mutableListOf())
            val startTick = lastCrouchStart[target] ?: (tick - 1)
            val crouchDuration = (tick - startTick).toInt()

            durations.add(0, crouchDuration)
            if (durations.size > 10) durations.removeAt(durations.size - 1)
            crouchDurations[target] = durations
        }
        wasSneaking[target] = currSneak

        if (target.isSwingInProgress && target.prevSwingProgress != target.swingProgress) {
            lastSwingTick[target] = tick
        }

        val lastPos = lastPositions.getOrDefault(target, Pair(currentSample.posX, currentSample.posZ))
        val deltaX = currentSample.posX - lastPos.first
        val deltaZ = currentSample.posZ - lastPos.second
        lastPositions[target] = Pair(currentSample.posX, currentSample.posZ)

        val moveYaw = getRelativeMoveAngle(deltaX, deltaZ, currentSample.yaw)
        lastMoveYaw[target] = moveYaw

        val startTick = lastCrouchStart[target] ?: 0
        val endTick = lastCrouchEnd[target] ?: 0
        val swingTick = lastSwingTick[target] ?: Long.MIN_VALUE
        val crouchDuration = (endTick - startTick).toInt()

        val quickCrouch = crouchDuration in 1..2
        val swingOnCrouch = swingTick in endTick..(endTick + 1)
        val holdingBlock = target.heldItem?.item is ItemBlock
        val lookingDown = currentSample.pitch >= 70f
        val extremelyLookingDown = currentSample.pitch >= 85f
        val onGround = currentSample.onGround
        val movingBackwards = abs(moveYaw) >= 90f
        val movingDirectlyBackwards = abs(moveYaw) >= 160f

        var flagged = false
        var checkType: String
        val vlAmount: Double

        if (lookingDown && onGround && holdingBlock) {
            if (quickCrouch && swingOnCrouch) {
                val durations = crouchDurations.getOrDefault(target, mutableListOf())
                val hasConsistentPattern = durations.size >= 3 &&
                        durations.take(3).all { it <= 2 }

                // Calculate consistency score from 0.0 to 1.0
                val consistencyScore = if (durations.size >= 3) {
                    val mean = durations.take(3).average()
                    val variance = durations.take(3).map { (it - mean) * (it - mean) }.average()
                    // Lower variance = higher consistency
                    max(0.0, min(1.0, 1.0 - (variance / 4.0)))
                } else 0.0

                // Base VL calculation
                checkType = "mechanical-pattern"
                vlAmount = 2.0

                // Apply multipliers based on various factors
                val pitchMultiplier = if (extremelyLookingDown) 1.5 else 1.0
                val swingTimingMultiplier = if (swingTick == endTick) 1.4 else 1.0
                val movementMultiplier = when {
                    movingDirectlyBackwards -> 1.8
                    movingBackwards -> 1.5
                    else -> 1.0
                }
                val consistencyMultiplier = 1.0 + (consistencyScore * 0.5)

                if (movingBackwards) {
                    checkType = "backwards-bridging"
                } else if (hasConsistentPattern) {
                    checkType = "consistent-pattern"
                }

                val lastPat = lastPatternTick.getOrDefault(target, 0L)
                var count = patternCount.getOrDefault(target, 0)
                if (tick - lastPat > 15) {
                    count = 0
                }
                count++
                lastPatternTick[target] = tick
                patternCount[target] = count

                // Track consecutive violations
                val consecutive = if (count >= 2) {
                    val current = consecutiveViolations.getOrDefault(target, 0) + 1
                    consecutiveViolations[target] = current
                    current
                } else 0

                // Apply final VL calculation with consecutive bonus
                val consecutiveMultiplier = 1.0 + (min(consecutive, 5) * 0.15)
                val finalVL = vlAmount * pitchMultiplier * swingTimingMultiplier *
                        movementMultiplier * consistencyMultiplier * consecutiveMultiplier

                if (count >= 2) {
                    val itemName = target.heldItem?.displayName ?: "unknown block"
                    val patternInfo = if (hasConsistentPattern) durations.take(3).joinToString(",") + "t" else "n/a"

                    addVL(
                        target,
                        finalVL,
                        "eagle-$checkType | angle=${"%.1f".format(moveYaw)}° | pitch=${"%.1f".format(currentSample.pitch)}° | " +
                                "crouch=${crouchDuration}t | swingTiming=${(swingTick - endTick)}t | " +
                                "patterns=$count | durations=$patternInfo | item=$itemName | consistency=${
                                    "%.2f".format(
                                        consistencyScore
                                    )
                                } | vl=${"%.1f".format(finalVL)}"
                    )
                    flagged = true
                    patternCount[target] = 0
                }
            } else if (swingTick == tick && endTick >= tick - 1 && startTick == endTick - 1) {
                checkType = "instant-sequence"

                // Apply multipliers based on pitch severity
                val pitchMultiplier = when {
                    extremelyLookingDown -> 1.6
                    currentSample.pitch >= 80f -> 1.3
                    else -> 1.0
                }

                // Apply multipliers based on movement direction
                val movementMultiplier = when {
                    movingDirectlyBackwards -> 1.5
                    movingBackwards -> 1.2
                    else -> 1.0
                }

                // Get consecutive violations
                val consecutive = consecutiveViolations.getOrDefault(target, 0) + 1
                consecutiveViolations[target] = consecutive
                val consecutiveMultiplier = 1.0 + (min(consecutive, 5) * 0.15)

                vlAmount = 3.5 * pitchMultiplier * movementMultiplier * consecutiveMultiplier
                val itemName = target.heldItem?.displayName ?: "unknown block"

                addVL(
                    target,
                    vlAmount,
                    "eagle-$checkType | crouch=${crouchDuration}t | item=$itemName | pitch=${"%.1f".format(currentSample.pitch)}° | angle=${
                        "%.1f".format(
                            moveYaw
                        )
                    }° | consecutive=$consecutive | vl=${"%.1f".format(vlAmount)}"
                )
                flagged = true
                patternCount[target] = 0
            } else {
                if ((tick - lastPatternTick.getOrDefault(target, 0)) > 15) {
                    patternCount[target] = 0
                }
            }
        } else {
            if ((tick - lastPatternTick.getOrDefault(target, 0)) > 15) {
                patternCount[target] = 0
            }
        }

        if (!flagged && getPlayerVL(target) > 0) {
            val currentVL = getPlayerVL(target)

            val decayRate = when {
                currentVL > 8.0 -> 0.2
                currentVL > 5.0 -> 0.15
                currentVL > 2.0 -> 0.1
                else -> 0.05
            }

            decayVL(target, decayRate)

            if (currentVL <= vlThreshold * 0.25) {
                consecutiveViolations[target] = 0
            }
        }
    }

    override fun onPlayerRemove(player: EntityPlayer?) {
        if (player != null) {
            lastCrouchStart.remove(player)
            lastCrouchEnd.remove(player)
            wasSneaking.remove(player)
            lastSwingTick.remove(player)
            patternCount.remove(player)
            lastPatternTick.remove(player)
            lastPositions.remove(player)
            lastMoveYaw.remove(player)
            crouchDurations.remove(player)
            consecutiveViolations.remove(player)
        } else {
            lastCrouchStart.clear()
            lastCrouchEnd.clear()
            wasSneaking.clear()
            lastSwingTick.clear()
            patternCount.clear()
            lastPatternTick.clear()
            lastPositions.clear()
            lastMoveYaw.clear()
            crouchDurations.clear()
            consecutiveViolations.clear()
        }

        super.onPlayerRemove(player)
    }
}
