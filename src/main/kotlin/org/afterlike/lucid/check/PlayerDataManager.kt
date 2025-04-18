package org.afterlike.lucid.check

import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayer
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.sqrt

object PlayerDataManager {
    private val mc: Minecraft = Minecraft.getMinecraft()
    private val playerHistory = ConcurrentHashMap<UUID, ArrayDeque<PlayerSample>>()
    private val MAX_HISTORY = 10
    
    fun collectSample(player: EntityPlayer) {
        val sample = PlayerSample(
            posX = player.posX,
            posY = player.posY,
            posZ = player.posZ,
            prevPosX = player.prevPosX,
            prevPosY = player.prevPosY,
            prevPosZ = player.prevPosZ,
            motionX = player.motionX,
            motionY = player.motionY,
            motionZ = player.motionZ,
            yaw = player.rotationYaw,
            pitch = player.rotationPitch,
            prevYaw = player.prevRotationYaw,
            prevPitch = player.prevRotationPitch,
            onGround = player.onGround,
            isSprinting = player.isSprinting,
            hurtTime = player.hurtTime,
            tick = mc.theWorld?.totalWorldTime ?: 0,
            timeStamp = System.currentTimeMillis()
        )
        
        val history = playerHistory.getOrPut(player.uniqueID) { ArrayDeque(MAX_HISTORY) }
        history.addLast(sample)
        
        if (history.size > MAX_HISTORY) {
            history.removeFirst()
        }
    }
    
    fun getHistory(player: EntityPlayer): List<PlayerSample> {
        return playerHistory.getOrDefault(player.uniqueID, ArrayDeque()).toList()
    }
    
    fun getCurrentSample(player: EntityPlayer): PlayerSample? {
        return playerHistory[player.uniqueID]?.lastOrNull()
    }
    
    fun getPreviousSample(player: EntityPlayer): PlayerSample? {
        val history = playerHistory[player.uniqueID] ?: return null
        return if (history.size >= 2) history.elementAt(history.size - 2) else null
    }
    
    fun getSampleAt(player: EntityPlayer, index: Int): PlayerSample? {
        val history = playerHistory[player.uniqueID] ?: return null
        return if (index >= 0 && index < history.size) history.elementAt(index) else null
    }
    
    fun getTicksAgo(player: EntityPlayer, ticks: Int): PlayerSample? {
        val history = playerHistory[player.uniqueID] ?: return null
        val currentTick = mc.theWorld?.totalWorldTime ?: 0
        return history.findLast { currentTick - it.tick == ticks.toLong() }
    }
    
    fun removePlayer(player: EntityPlayer?) {
        if (player != null) {
            playerHistory.remove(player.uniqueID)
        } else {
            playerHistory.clear()
        }
    }
    
    fun hasDataFor(player: EntityPlayer): Boolean {
        return playerHistory.containsKey(player.uniqueID)
    }
}

data class PlayerSample(
    val posX: Double,
    val posY: Double,
    val posZ: Double,
    val prevPosX: Double,
    val prevPosY: Double,
    val prevPosZ: Double,
    val motionX: Double,
    val motionY: Double,
    val motionZ: Double,
    val yaw: Float,
    val pitch: Float,
    val prevYaw: Float,
    val prevPitch: Float,
    val onGround: Boolean,
    val isSprinting: Boolean,
    val hurtTime: Int,
    val tick: Long,
    val timeStamp: Long
) {
    val velocity: Double
        get() = sqrt(motionX * motionX + motionZ * motionZ)

    val deltaX: Double
        get() = posX - prevPosX

    val deltaY: Double
        get() = posY - prevPosY

    val deltaZ: Double
        get() = posZ - prevPosZ
}