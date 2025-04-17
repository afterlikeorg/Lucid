package org.afterlike.lucid.check

import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemBlock
import kotlin.math.abs

class EagleCheck : Check() {
    override val name = "Eagle"
    override val description = "Detects mechanical patterns commonly used in speed-bridging cheats"

    private val lastCrouchStart = mutableMapOf<EntityPlayer, Int>()
    private val lastCrouchEnd = mutableMapOf<EntityPlayer, Int>()
    private val wasSneaking = mutableMapOf<EntityPlayer, Boolean>()
    private val lastSwingTick = mutableMapOf<EntityPlayer, Int>()
    private val patternCount = mutableMapOf<EntityPlayer, Int>()
    private val lastPatternTick = mutableMapOf<EntityPlayer, Int>()
    private val lastPositions = mutableMapOf<EntityPlayer, Pair<Double, Double>>()
    private val lastMoveYaw = mutableMapOf<EntityPlayer, Float>()
    private val crouchDurations = mutableMapOf<EntityPlayer, MutableList<Int>>()
    private var lastCleanupTime = System.currentTimeMillis()
    private val CLEANUP_INTERVAL = 30000L

    init {
        CheckManager.register(this)
        vlThreshold = 10
    }

    override fun onUpdate(target: EntityPlayer) {
        val mc = Minecraft.getMinecraft()
        val tick = mc.theWorld.totalWorldTime.toInt()
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastCleanupTime > CLEANUP_INTERVAL) {
            cleanupOldData()
            lastCleanupTime = currentTime
        }

        if (target === mc.thePlayer) return

        val prevSneak = wasSneaking.getOrDefault(target, false)
        val currSneak = target.isSneaking
        if (currSneak && !prevSneak) {
            lastCrouchStart[target] = tick
        }
        if (!currSneak && prevSneak) {
            lastCrouchEnd[target] = tick

            val durations = crouchDurations.getOrDefault(target, mutableListOf())
            val startTick = lastCrouchStart[target] ?: (tick - 1)
            val crouchDuration = tick - startTick

            durations.add(0, crouchDuration)
            if (durations.size > 10) durations.removeAt(durations.size - 1)
            crouchDurations[target] = durations
        }
        wasSneaking[target] = currSneak


        if (target.isSwingInProgress && target.prevSwingProgress != target.swingProgress) {
            lastSwingTick[target] = tick
        }

        val lastPos = lastPositions.getOrDefault(target, Pair(target.posX, target.posZ))
        val deltaX = target.posX - lastPos.first
        val deltaZ = target.posZ - lastPos.second
        lastPositions[target] = Pair(target.posX, target.posZ)

        val moveYaw = if (abs(deltaX) < 1e-8 && abs(deltaZ) < 1e-8) {
            lastMoveYaw.getOrDefault(target, 0f)
        } else {
            val moveAngle = Math.toDegrees(Math.atan2(deltaZ, deltaX)).toFloat() - 90f
            val relativeAngle = moveAngle - target.rotationYaw
            val normalizedAngle = ((relativeAngle % 360 + 360) % 360).toFloat()
            val finalAngle = if (normalizedAngle > 180) normalizedAngle - 360 else normalizedAngle
            lastMoveYaw[target] = finalAngle
            finalAngle
        }

        val startTick = lastCrouchStart[target] ?: 0
        val endTick = lastCrouchEnd[target] ?: 0
        val swingTick = lastSwingTick[target] ?: Int.MIN_VALUE
        val crouchDuration = endTick - startTick

        val quickCrouch = crouchDuration <= 2 && crouchDuration > 0
        val swingOnCrouch = swingTick in endTick..(endTick + 1)
        val holdingBlock = target.heldItem?.item is ItemBlock
        val lookingDown = target.rotationPitch >= 70f
        val onGround = target.onGround
        val movingBackwards = abs(moveYaw) >= 90f

        var flagged = false
        var reason = ""
        var vlAmount = 0.0


        if (lookingDown && onGround && holdingBlock) {

            if (quickCrouch && swingOnCrouch) {
                val durations = crouchDurations.getOrDefault(target, mutableListOf())
                val hasConsistentPattern = durations.size >= 3 &&
                        durations.take(3).all { it <= 2 }

                reason = "mechanical crouch-place with ${crouchDuration}t crouch, " +
                        "pitch=${String.format("%.1f", target.rotationPitch)}°"
                vlAmount = 2.0


                if (movingBackwards) {
                    reason = "backwards speed-bridging (moveYaw: ${String.format("%.1f", moveYaw)}°, " +
                            "crouch: ${crouchDuration}t, swing timing: ${swingTick - endTick}t)"
                    vlAmount = 3.0
                } else if (hasConsistentPattern) {
                    reason += ", showing consistent pattern of ${durations.take(3).joinToString(", ")}t crouches"
                    vlAmount = 2.5
                }

                val lastPat = lastPatternTick.getOrDefault(target, Int.MIN_VALUE)
                var count = patternCount.getOrDefault(target, 0)
                if (tick - lastPat > 15) {
                    count = 0
                }
                count++
                lastPatternTick[target] = tick
                patternCount[target] = count


                if (count >= 2) {

                    reason = "$reason (consecutive pattern #$count)"
                    flagged = true
                    patternCount[target] = 0
                }
            } else if (swingTick == tick && endTick >= tick - 1 && startTick == endTick - 1) {

                reason = "instant crouch-place-uncrouch sequence (crouch duration: 1t)"
                vlAmount = 3.5
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

        if (flagged) {
            addVL(target, vlAmount, reason)
        } else if (getPlayerVL(target) > 0) {
            decayVL(target, 0.1)
        }
    }

    private fun cleanupOldData() {
        try {
            val mc = Minecraft.getMinecraft()
            val worldPlayers = mc.theWorld?.playerEntities ?: listOf()

            val allPlayers = mutableSetOf<EntityPlayer>()
            allPlayers.addAll(worldPlayers)

            val toRemove = mutableSetOf<EntityPlayer>()
            lastCrouchStart.keys.forEach { if (!allPlayers.contains(it)) toRemove.add(it) }
            lastCrouchEnd.keys.forEach { if (!allPlayers.contains(it)) toRemove.add(it) }
            wasSneaking.keys.forEach { if (!allPlayers.contains(it)) toRemove.add(it) }
            lastSwingTick.keys.forEach { if (!allPlayers.contains(it)) toRemove.add(it) }
            patternCount.keys.forEach { if (!allPlayers.contains(it)) toRemove.add(it) }
            lastPatternTick.keys.forEach { if (!allPlayers.contains(it)) toRemove.add(it) }
            lastPositions.keys.forEach { if (!allPlayers.contains(it)) toRemove.add(it) }
            lastMoveYaw.keys.forEach { if (!allPlayers.contains(it)) toRemove.add(it) }
            crouchDurations.keys.forEach { if (!allPlayers.contains(it)) toRemove.add(it) }

            toRemove.forEach { onPlayerRemove(it) }

        } catch (e: Exception) {
            logError("Error cleaning up eagle data: ${e.message}")
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
        }

        super.onPlayerRemove(player)
    }
}
