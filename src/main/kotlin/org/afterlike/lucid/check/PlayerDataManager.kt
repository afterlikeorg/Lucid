package org.afterlike.lucid.check

import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayer
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.ArrayDeque
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.elementAt
import kotlin.collections.getOrPut
import kotlin.collections.isNotEmpty
import kotlin.collections.mutableListOf
import kotlin.collections.toList
import kotlin.math.sqrt

object PlayerDataManager {
    private val mc: Minecraft = Minecraft.getMinecraft()
    private val playerHistory = ConcurrentHashMap<UUID, ArrayDeque<PlayerSample>>()
    private const val MAX_HISTORY = 8

    private val samplePool = ConcurrentHashMap<UUID, MutableList<PlayerSample>>()
    private const val POOL_SIZE = 10

    fun collectSample(player: EntityPlayer) {
        val uuid = player.uniqueID
        val sample = getOrCreateSample(uuid).apply {
            posX = player.posX
            posY = player.posY
            posZ = player.posZ
            prevPosX = player.prevPosX
            prevPosY = player.prevPosY
            prevPosZ = player.prevPosZ
            motionX = player.motionX
            motionY = player.motionY
            motionZ = player.motionZ
            yaw = player.rotationYaw
            pitch = player.rotationPitch
            prevYaw = player.prevRotationYaw
            prevPitch = player.prevRotationPitch
            onGround = player.onGround
            isSprinting = player.isSprinting
            hurtTime = player.hurtTime
            tick = mc.theWorld?.totalWorldTime ?: 0
            timeStamp = System.currentTimeMillis()
        }

        val history = playerHistory.getOrPut(uuid) { ArrayDeque(MAX_HISTORY) }
        history.addLast(sample)

        if (history.size > MAX_HISTORY) {
            val removed = history.removeFirst()
            recycleSample(uuid, removed)
        }
    }

    private fun getOrCreateSample(uuid: UUID): PlayerSample {
        val pool = samplePool.getOrPut(uuid) { mutableListOf() }
        return if (pool.isNotEmpty()) {
            pool.removeAt(pool.size - 1)
        } else {
            PlayerSample()
        }
    }

    private fun recycleSample(uuid: UUID, sample: PlayerSample) {
        val pool = samplePool.getOrPut(uuid) { mutableListOf() }
        if (pool.size < POOL_SIZE) {
            pool.add(sample)
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
        val currentTick = mc.theWorld?.totalWorldTime ?: 0
        val targetTick = currentTick - ticks

        val history = playerHistory[player.uniqueID] ?: return null
        for (sample in history) {
            if (sample.tick == targetTick) {
                return sample
            }
        }
        return null
    }

    fun removePlayer(player: EntityPlayer?) {
        if (player != null) {
            playerHistory.remove(player.uniqueID)
            samplePool.remove(player.uniqueID)
        } else {
            playerHistory.clear()
            samplePool.clear()
        }
    }

    fun hasDataFor(player: EntityPlayer): Boolean {
        return playerHistory.containsKey(player.uniqueID)
    }
}

class PlayerSample {
    var posX: Double = 0.0
    var posY: Double = 0.0
    var posZ: Double = 0.0
    var prevPosX: Double = 0.0
    var prevPosY: Double = 0.0
    var prevPosZ: Double = 0.0
    var motionX: Double = 0.0
    var motionY: Double = 0.0
    var motionZ: Double = 0.0
    var yaw: Float = 0f
    var pitch: Float = 0f
    var prevYaw: Float = 0f
    var prevPitch: Float = 0f
    var onGround: Boolean = false
    var isSprinting: Boolean = false
    var hurtTime: Int = 0
    var tick: Long = 0
    var timeStamp: Long = 0

    private var _velocity: Double? = null

    val velocity: Double
        get() {
            if (_velocity == null) {
                _velocity = sqrt(motionX * motionX + motionZ * motionZ)
            }
            return _velocity!!
        }

    val deltaX: Double
        get() = posX - prevPosX

    val deltaY: Double
        get() = posY - prevPosY

    val deltaZ: Double
        get() = posZ - prevPosZ
}