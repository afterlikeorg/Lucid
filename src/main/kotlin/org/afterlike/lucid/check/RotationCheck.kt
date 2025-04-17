package org.afterlike.lucid.check

import net.minecraft.entity.player.EntityPlayer
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

class RotationCheck : Check() {
    override val name = "Rotation"
    override val description = "Detects illegal rotations and rotation patterns"

    private val lastPitchChanges = ConcurrentHashMap<EntityPlayer, MutableList<Float>>()
    private val lastYawChanges = ConcurrentHashMap<EntityPlayer, MutableList<Float>>()
    
    private var lastCleanupTime = System.currentTimeMillis()
    private val CLEANUP_INTERVAL = 30000L

    init {
        CheckManager.register(this)
        vlThreshold = 8
    }

    override fun onPlayerRemove(player: EntityPlayer?) {

        if (player != null) {
            lastPitchChanges.remove(player)
            lastYawChanges.remove(player)
        } else {
            lastPitchChanges.clear()
            lastYawChanges.clear()
        }
        
        super.onPlayerRemove(player)
    }

    override fun onUpdate(target: EntityPlayer) {
        try {
            val mc = net.minecraft.client.Minecraft.getMinecraft()
            val currentTime = System.currentTimeMillis()
    
            if (target == mc.thePlayer) return
            
            if (currentTime - lastCleanupTime > CLEANUP_INTERVAL) {
                cleanupRotationData()
                lastCleanupTime = currentTime
            }
    
            val pitch = target.rotationPitch
            val prevPitch = target.prevRotationPitch
            val yaw = target.rotationYaw
            val prevYaw = target.prevRotationYaw
    
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
            var reason = ""
            var vlAmount = 0.0


            if (abs(pitch) > 90) {

                vlAmount = when {
                    abs(pitch) > 120 -> 8.0
                    abs(pitch) > 100 -> 6.0
                    else -> 4.5
                }
    
                reason = "illegal pitch angle=${"%.1f".format(pitch)}° (normal range -90° to 90°)"
                flagged = true
            } else if ((yawDelta > 40 || pitchDelta > 30) && prevYaw != 0f && prevPitch != 0f) {

                val isConsistent = checkConsistency(pitchChanges, yawChanges)


                vlAmount = when {
                    yawDelta > 70 && pitchDelta > 50 -> 5.0
                    isConsistent && (yawDelta > 45 || pitchDelta > 35) -> 4.0
                    else -> 3.0
                }
    
                reason =
                    "possible aimbot, rotation speed=${"%.1f".format(yawDelta)}° yaw, ${"%.1f".format(pitchDelta)}° pitch" +
                            (if (isConsistent) ", showing consistent pattern" else "")
                flagged = true
            }


            if (flagged) {
                addVL(target, vlAmount, reason)
            } else {
                decayVL(target, 0.1)
            }
        } catch (e: Exception) {
            super.onUpdate(target)
        }
    }
    
    private fun cleanupRotationData() {
        try {
            val mc = net.minecraft.client.Minecraft.getMinecraft()
            val activePlayerIds = mc.theWorld?.playerEntities?.map { it.uniqueID }?.toSet() ?: setOf()
            
            val toRemovePitch = lastPitchChanges.keys.filter { player -> 
                !activePlayerIds.contains(player.uniqueID) || !player.isEntityAlive
            }
            
            val toRemoveYaw = lastYawChanges.keys.filter { player -> 
                !activePlayerIds.contains(player.uniqueID) || !player.isEntityAlive
            }
            
            toRemovePitch.forEach { lastPitchChanges.remove(it) }
            toRemoveYaw.forEach { lastYawChanges.remove(it) }
            
            if (lastPitchChanges.size > 50) {
                val extraPlayers = lastPitchChanges.keys.take(lastPitchChanges.size - 25)
                extraPlayers.forEach { lastPitchChanges.remove(it) }
            }
            
            if (lastYawChanges.size > 50) {
                val extraPlayers = lastYawChanges.keys.take(lastYawChanges.size - 25)
                extraPlayers.forEach { lastYawChanges.remove(it) }
            }
            
        } catch (e: Exception) {
            logError("Error cleaning up rotation data: ${e.message}")
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
} 