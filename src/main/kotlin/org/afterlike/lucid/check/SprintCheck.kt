package org.afterlike.lucid.check

import net.minecraft.entity.player.EntityPlayer
import kotlin.math.abs
import kotlin.math.max

class SprintCheck : Check() {
    override val name = "Sprint"
    override val description = "Detects omnidirectional sprinting (backwards/sideways)"
    
    private val playerData = mutableMapOf<EntityPlayer, Double>()
    private var lastCleanupTime = System.currentTimeMillis()
    private val CLEANUP_INTERVAL = 30000L

    init {
        CheckManager.register(this)
        vlThreshold = 8
    }

    override fun onUpdate(target: EntityPlayer) {
        val mc = net.minecraft.client.Minecraft.getMinecraft()
        val currentTime = System.currentTimeMillis()
        
        if (currentTime - lastCleanupTime > CLEANUP_INTERVAL) {
            cleanupOldData()
            lastCleanupTime = currentTime
        }

        if (target == mc.thePlayer || target.ridingEntity != null) return

        val isSprinting = target.isSprinting
        val isGroundCollision = target.onGround

        val current = target.positionVector
        val prev = target.prevPosX to target.prevPosZ

        val deltaX = current.xCoord - prev.first
        val deltaZ = current.zCoord - prev.second

        val speed = max(abs(deltaX), abs(deltaZ))

        val rotationYaw = target.rotationYaw
        val moveYaw = getMoveYaw(deltaX, deltaZ, rotationYaw)


        if (isSprinting && isGroundCollision && abs(moveYaw) > 90 && speed >= 0.2) {

            val vlAmount = when {
                abs(moveYaw) > 160 -> 2.0
                abs(moveYaw) > 120 -> 1.5
                else -> 1.0
            }

            addVL(target, vlAmount, "omnidirectional sprint angle=${"%.1f".format(moveYaw)}")
        } else {

            if (getPlayerVL(target) > 0) decayVL(target, 0.5)
        }
    }

    private fun getMoveYaw(deltaX: Double, deltaZ: Double, playerYaw: Float): Float {
        if (abs(deltaX) < 1e-8 && abs(deltaZ) < 1e-8) {
            return 0f
        }

        val moveAngle = Math.toDegrees(Math.atan2(deltaZ, deltaX)).toFloat() - 90f
        var relativeAngle = moveAngle - playerYaw


        relativeAngle = ((relativeAngle % 360f) + 360f) % 360f
        if (relativeAngle > 180f) {
            relativeAngle -= 360f
        }

        return relativeAngle
    }
    
    private fun cleanupOldData() {
        try {
            val mc = net.minecraft.client.Minecraft.getMinecraft()
            val worldPlayers = mc.theWorld?.playerEntities ?: listOf()
            
            val toRemove = mutableListOf<EntityPlayer>()
            playerData.keys.forEach { player ->
                if (!worldPlayers.contains(player) || !player.isEntityAlive) {
                    toRemove.add(player)
                }
            }
            
            toRemove.forEach { player ->
                playerData.remove(player)
                onPlayerRemove(player)
            }
            
        } catch (e: Exception) {
            logError("Error cleaning up sprint data: ${e.message}")
        }
    }
    
    override fun onPlayerRemove(player: EntityPlayer?) {
        if (player != null) {
            playerData.remove(player)
        } else {
            playerData.clear()
        }
        
        super.onPlayerRemove(player)
    }
} 