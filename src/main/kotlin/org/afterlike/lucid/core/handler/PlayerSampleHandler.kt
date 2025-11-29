package org.afterlike.lucid.core.handler

import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.BlockPos
import org.afterlike.lucid.core.type.PlayerSample
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.floor

// this class handles saving and retrieving player samples
object PlayerSampleHandler {
    private val mc: Minecraft = Minecraft.getMinecraft()

    private const val GROUND_THRESHOLD = 0.05 // distance threshold for ground detection
    private const val MAX_HISTORY = 8

    // saves up to MAX_HISTORY previous samples
    private val playerSampleHistory = ConcurrentHashMap<UUID, ArrayDeque<PlayerSample>>()

    // used to average out previous POOL_SIZE samples
    private val playerSamplePool = ConcurrentHashMap<UUID, MutableList<PlayerSample>>()

    // retrieves sample by player or creates if player is not found
    private fun getOrCreateSample(uuid: UUID): PlayerSample {
        val pool = playerSamplePool.getOrPut(uuid) { mutableListOf() }
        return if (pool.isNotEmpty()) {
            pool.removeAt(pool.size - 1)
        } else {
            PlayerSample()
        }
    }

    // custom onGround calculation
    private fun calculateOnGround(player: EntityPlayer): Boolean {
        val world = mc.theWorld ?: return false

        try {
            val boundingBox = player.entityBoundingBox ?: return false
            val expandedBox = AxisAlignedBB(
                boundingBox.minX,
                boundingBox.minY - GROUND_THRESHOLD,
                boundingBox.minZ,
                boundingBox.maxX,
                boundingBox.minY,
                boundingBox.maxZ
            )

            val minX = floor(expandedBox.minX).toInt()
            val minY = floor(expandedBox.minY).toInt()
            val minZ = floor(expandedBox.minZ).toInt()
            val maxX = floor(expandedBox.maxX).toInt()
            val maxY = floor(expandedBox.maxY).toInt()
            val maxZ = floor(expandedBox.maxZ).toInt()

            for (x in minX..maxX) {
                for (y in minY..maxY) {
                    for (z in minZ..maxZ) {
                        val blockPos = BlockPos(x, y, z)
                        val blockState = world.getBlockState(blockPos)
                        val block = blockState?.block ?: continue
                        if (block.material.isReplaceable) continue

                        val blockBox = block.getCollisionBoundingBox(world, blockPos, blockState)

                        if (blockBox != null && expandedBox.intersectsWith(blockBox)) {
                            return true
                        }
                    }
                }
            }
        } catch (_: Exception) {
            return player.onGround
        }

        return false
    }

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

            onGround = calculateOnGround(player)
            isSprinting = player.isSprinting
            hurtTime = player.hurtTime

            tick = mc.theWorld?.totalWorldTime ?: 0
            timeStamp = System.currentTimeMillis()

        }

        val history = playerSampleHistory.getOrPut(uuid) { ArrayDeque(MAX_HISTORY) }
        history.addLast(sample)

        if (history.size > MAX_HISTORY) {
            history.removeFirst()
        }
    }

    fun getAllSamples(player: EntityPlayer): List<PlayerSample> {
        return playerSampleHistory.getOrDefault(player.uniqueID, ArrayDeque()).toList()
    }

    fun getLatestSample(player: EntityPlayer): PlayerSample? {
        return playerSampleHistory[player.uniqueID]?.lastOrNull()
    }

    fun getPreviousSample(player: EntityPlayer): PlayerSample? {
        val history = playerSampleHistory[player.uniqueID] ?: return null
        return if (history.size >= 2) history.elementAt(history.size - 2) else null
    }

    fun getSampleAt(player: EntityPlayer, index: Int): PlayerSample? {
        val history = playerSampleHistory[player.uniqueID] ?: return null
        return if (index >= 0 && index < history.size) history.elementAt(index) else null
    }

    fun getSampleTicksAgo(player: EntityPlayer, ticks: Int): PlayerSample? {
        val currentTick = mc.theWorld?.totalWorldTime ?: 0
        val targetTick = currentTick - ticks

        val history = playerSampleHistory[player.uniqueID] ?: return null
        for (sample in history) {
            if (sample.tick == targetTick) {
                return sample
            }
        }
        return null
    }

    fun removePlayer(player: EntityPlayer?) {
        if (player != null) {
            playerSampleHistory.remove(player.uniqueID)
            playerSamplePool.remove(player.uniqueID)
        } else {
            playerSampleHistory.clear()
            playerSamplePool.clear()
        }
    }

    fun hasDataFor(player: EntityPlayer): Boolean {
        return playerSampleHistory.containsKey(player.uniqueID)
    }
}