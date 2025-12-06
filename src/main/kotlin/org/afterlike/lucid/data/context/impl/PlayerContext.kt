package org.afterlike.lucid.data.context.impl

import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.BlockPos
import org.afterlike.lucid.data.context.api.AbstractContext
import kotlin.math.floor

// tracks player state
class PlayerContext : AbstractContext() {

    var posX = 0.0
    var posY = 0.0
    var posZ = 0.0
    var prevPosX = 0.0
    var prevPosY = 0.0
    var prevPosZ = 0.0
    var motionX = 0.0
    var motionY = 0.0
    var motionZ = 0.0

    var yaw = 0f
    var pitch = 0f
    var prevYaw = 0f
    var prevPitch = 0f

    var onGround = false
    var isSprinting = false
    var isSneaking = false
    var isBlocking = false
    var isUsingItem = false
    var isSwingInProgress = false

    var hurtTime = 0
    var swingProgress = 0f
    var prevSwingProgress = 0f

    val deltaX: Double get() = posX - prevPosX
    val deltaY: Double get() = posY - prevPosY
    val deltaZ: Double get() = posZ - prevPosZ
    val deltaYaw: Float get() = yaw - prevYaw
    val deltaPitch: Float get() = pitch - prevPitch

    override fun update(player: EntityPlayer, worldTick: Long) {
        tick = worldTick
        timestamp = System.currentTimeMillis()

        prevPosX = posX; prevPosY = posY; prevPosZ = posZ
        posX = player.posX; posY = player.posY; posZ = player.posZ
        motionX = player.motionX; motionY = player.motionY; motionZ = player.motionZ

        prevYaw = yaw; prevPitch = pitch
        yaw = player.rotationYaw; pitch = player.rotationPitch

        onGround = calculateOnGround(player)
        isSprinting = player.isSprinting
        isSneaking = player.isSneaking
        isBlocking = player.isBlocking
        isUsingItem = player.isUsingItem
        isSwingInProgress = player.isSwingInProgress

        hurtTime = player.hurtTime
        prevSwingProgress = swingProgress
        swingProgress = player.swingProgress
    }

    override fun reset() {
        posX = 0.0; posY = 0.0; posZ = 0.0
        prevPosX = 0.0; prevPosY = 0.0; prevPosZ = 0.0
        motionX = 0.0; motionY = 0.0; motionZ = 0.0
        yaw = 0f; pitch = 0f; prevYaw = 0f; prevPitch = 0f
        onGround = false; isSprinting = false; isSneaking = false
        isBlocking = false; isUsingItem = false; isSwingInProgress = false
        hurtTime = 0; swingProgress = 0f; prevSwingProgress = 0f
        tick = 0; timestamp = 0
    }

    private fun calculateOnGround(player: EntityPlayer): Boolean {
        val world = Minecraft.getMinecraft().theWorld ?: return player.onGround
        val boundingBox = player.entityBoundingBox ?: return player.onGround

        val expandedBox = AxisAlignedBB(
            boundingBox.minX, boundingBox.minY - 0.05, boundingBox.minZ,
            boundingBox.maxX, boundingBox.minY, boundingBox.maxZ
        )

        for (x in floor(expandedBox.minX).toInt()..floor(expandedBox.maxX).toInt()) {
            for (y in floor(expandedBox.minY).toInt()..floor(expandedBox.maxY).toInt()) {
                for (z in floor(expandedBox.minZ).toInt()..floor(expandedBox.maxZ).toInt()) {
                    val pos = BlockPos(x, y, z)
                    val state = world.getBlockState(pos) ?: continue
                    if (state.block.material.isReplaceable) continue
                    val box = state.block.getCollisionBoundingBox(world, pos, state)
                    if (box != null && expandedBox.intersectsWith(box)) return true
                }
            }
        }
        return false
    }
}
