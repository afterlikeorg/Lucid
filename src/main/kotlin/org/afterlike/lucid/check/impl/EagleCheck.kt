package org.afterlike.lucid.check.impl

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemBlock
import org.afterlike.lucid.check.api.AbstractCheck
import org.afterlike.lucid.core.event.world.EntityLeaveEvent
import org.afterlike.lucid.data.handler.impl.PlayerHandler
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.*

// TODO: clean up this check more, remove less accurate checks
class EagleCheck : AbstractCheck() {
    override val name = "Eagle"
    override val description = "Detects mechanical patterns commonly used in speed-bridging cheats"
    override var violationLevelThreshold = 10

    override val decayConfig = DecayConfig(
        baseRate = 0.3,
        mediumRate = 0.5,
        highRate = 0.8,
        criticalRate = 1.0,
        resetThreshold = 0.2
    )

    private val latestSneakStart = ConcurrentHashMap<EntityPlayer, Long>()
    private val latestSneakEnd = ConcurrentHashMap<EntityPlayer, Long>()
    private val wasSneaking = ConcurrentHashMap<EntityPlayer, Boolean>()
    private val latestSwing = ConcurrentHashMap<EntityPlayer, Long>()
    private val improbableFlags = ConcurrentHashMap<EntityPlayer, Int>()
    private val latestImprobableFlag = ConcurrentHashMap<EntityPlayer, Long>()
    private val latestMovementYaw = ConcurrentHashMap<EntityPlayer, Float>()
    private val sneakDurations = ConcurrentHashMap<EntityPlayer, MutableList<Int>>()
    private val consecutiveViolations = ConcurrentHashMap<EntityPlayer, Int>()

    override fun onCheckRun(target: EntityPlayer) {
        val data = PlayerHandler.get(target) ?: return
        val ctx = data.player

        val latestTick = ctx.tick
        val deltaX = ctx.deltaX
        val deltaZ = ctx.deltaZ
        val horizontalDelta = sqrt(deltaX.pow(2.0) + deltaZ.pow(2.0))

        val wasSneaking = this.wasSneaking.getOrDefault(target, false)
        val isSneaking = ctx.isSneaking

        // start sneaking
        if (isSneaking && !wasSneaking) {
            latestSneakStart[target] = latestTick
        }

        val sneakDurations = this.sneakDurations.getOrPut(target) { mutableListOf() }

        // stop sneaking
        if (!isSneaking && wasSneaking) {
            latestSneakEnd[target] = latestTick

            val sneakStartingTick = latestSneakStart[target] ?: (latestTick - 1)
            val sneakDuration = (latestTick - sneakStartingTick).toInt()

            sneakDurations.add(0, sneakDuration)
            if (sneakDurations.size > 10) sneakDurations.removeLast()
        }

        this.wasSneaking[target] = isSneaking

        // swung this tick
        if (ctx.isSwingInProgress && ctx.prevSwingProgress != ctx.swingProgress) {
            latestSwing[target] = latestTick
        }

        val movementYaw = getRelativeMoveAngle(deltaX, deltaZ, ctx.yaw)
        latestMovementYaw[target] = movementYaw

        val sneakStartTick = latestSneakStart[target] ?: 0
        val sneakEndTick = latestSneakEnd[target] ?: 0
        val swingTick = latestSwing[target] ?: Long.MIN_VALUE
        val sneakDuration = (sneakEndTick - sneakStartTick).toInt()

        val fastSneakStop = sneakDuration in 1..2
        val minimalOffset = swingTick in (sneakEndTick - 1)..(sneakEndTick + 1)
        val holdingBlock = target.heldItem?.item is ItemBlock
        val lookingDown = ctx.pitch >= 70f
        val onGround = ctx.onGround
        val movingBackwards = abs(movementYaw) >= 90f

        // early exit conditions - not bridging
        if (!lookingDown || !holdingBlock || !onGround) {
            if ((latestTick - latestImprobableFlag.getOrDefault(target, 0)) > 15) {
                improbableFlags[target] = 0
            }
            if (handleNoViolation(target)) {
                consecutiveViolations[target] = 0
            }
            return
        }

        var flagged = false

        if (fastSneakStop && minimalOffset) {
            val checkType = "improbable-speed"
            val allFast = sneakDurations.size >= 3 && sneakDurations.take(3).all { it <= 2 }

            val consistencyScore = if (sneakDurations.size >= 5) {
                val mean = sneakDurations.take(5).average()
                val variance = sneakDurations.take(5).map { (it - mean) * (it - mean) }.average()
                max(0.0, min(1.0, 1.0 - (variance / 4.0)))
            } else 0.0

            val vlAmount = 2.0

            val swingTimingMultiplier = if (swingTick == sneakEndTick) 1.4 else 1.0
            val movementMultiplier = if (movingBackwards) 1.5 else 1.0
            val consistencyMultiplier = 1.0 + (consistencyScore * 0.5)

            val latestImprobable = latestImprobableFlag.getOrDefault(target, 0L)
            var improbableCount = improbableFlags.getOrDefault(target, 0)

            if (latestTick - latestImprobable > 15) improbableCount = 0
            improbableCount++

            latestImprobableFlag[target] = latestTick
            improbableFlags[target] = improbableCount

            val consecutive = if (improbableCount >= 2) {
                val current = consecutiveViolations.getOrDefault(target, 0) + 1
                consecutiveViolations[target] = current
                current
            } else 0

            val consecutiveMultiplier = 1.0 + (min(consecutive, 5) * 0.15)
            val finalVL =
                vlAmount * swingTimingMultiplier * movementMultiplier * consistencyMultiplier * consecutiveMultiplier

            if (improbableCount >= 2) {
                val itemName = target.heldItem?.displayName ?: "unknown block"
                val patternInfo = if (allFast) sneakDurations.take(3).joinToString(",") + "t" else "n/a"

                addVL(
                    target, finalVL,
                    "eagle-$checkType | angle=${"%.1f".format(movementYaw)}° | pitch=${"%.1f".format(ctx.pitch)}° | " +
                            "crouch=${sneakDuration}t | swingTiming=${(swingTick - sneakEndTick)}t | edgeOffset=$horizontalDelta" +
                            "patterns=$improbableCount | durations=$patternInfo | item=$itemName | consistency=${
                                "%.2f".format(
                                    consistencyScore
                                )
                            } | vl=${"%.1f".format(finalVL)}"
                )
                flagged = true
                improbableFlags[target] = 0
            }
        } else if (swingTick == latestTick && sneakEndTick >= latestTick - 1 && sneakStartTick == sneakEndTick - 1) {
            val checkType = "instant"

            val pitchMultiplier = when {
                ctx.pitch >= 85f -> 1.6
                ctx.pitch >= 80f -> 1.3
                else -> 1.0
            }

            val movementMultiplier = if (movingBackwards) 1.2 else 1.0

            val consecutive = consecutiveViolations.getOrDefault(target, 0) + 1
            consecutiveViolations[target] = consecutive
            val consecutiveMultiplier = 1.0 + (min(consecutive, 5) * 0.15)

            val vlAmount = 3.5 * pitchMultiplier * movementMultiplier * consecutiveMultiplier
            val itemName = target.heldItem?.displayName ?: "unknown block"

            addVL(
                target, vlAmount,
                "eagle-$checkType | crouch=${sneakDuration}t | item=$itemName | pitch=${"%.1f".format(ctx.pitch)}° | angle=${
                    "%.1f".format(
                        movementYaw
                    )
                }° | consecutive=$consecutive | vl=${"%.1f".format(vlAmount)}"
            )
            flagged = true
            improbableFlags[target] = 0
        } else {
            if ((latestTick - latestImprobableFlag.getOrDefault(target, 0)) > 15) {
                improbableFlags[target] = 0
            }
        }

        // decay when no violation
        if (!flagged && handleNoViolation(target)) {
            consecutiveViolations[target] = 0
        }
    }

    override fun onPlayerLeave(event: EntityLeaveEvent) {
        val player = event.entity
        latestSneakStart.remove(player)
        latestSneakEnd.remove(player)
        wasSneaking.remove(player)
        latestSwing.remove(player)
        improbableFlags.remove(player)
        latestImprobableFlag.remove(player)
        latestMovementYaw.remove(player)
        sneakDurations.remove(player)
        consecutiveViolations.remove(player)
    }
}
