package org.afterlike.lucid.data.context.impl

import net.minecraft.block.Block
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.BlockPos
import org.afterlike.lucid.data.context.api.AbstractContext

// tracks world state
class WorldContext : AbstractContext() {

    var blockBelow: Block? = null
    var blockBelowPos: BlockPos? = null
    var blockAtFeet: Block? = null
    var blockAtHead: Block? = null

    var blocksPlacedNearby = 0
    var lastBlockPlaceTime = 0L

    var isInLiquid = false
    var isOnLadder = false
    var isOnIce = false
    var isOnSlime = false

    override fun update(player: EntityPlayer, worldTick: Long) {
        tick = worldTick
        timestamp = System.currentTimeMillis()

        val world = Minecraft.getMinecraft().theWorld ?: return

        val feetPos = BlockPos(player.posX, player.posY, player.posZ)
        val belowPos = BlockPos(player.posX, player.posY - 0.5, player.posZ)
        val headPos = BlockPos(player.posX, player.posY + player.eyeHeight, player.posZ)

        blockAtFeet = world.getBlockState(feetPos)?.block
        blockBelow = world.getBlockState(belowPos)?.block
        blockBelowPos = belowPos
        blockAtHead = world.getBlockState(headPos)?.block

        isInLiquid = player.isInWater || player.isInLava
        isOnLadder = player.isOnLadder

        blockBelow?.let { block ->
            val name = block.unlocalizedName
            isOnIce = name.contains("ice", ignoreCase = true)
            isOnSlime = name.contains("slime", ignoreCase = true)
        }
    }

    override fun reset() {
        blockBelow = null; blockBelowPos = null
        blockAtFeet = null; blockAtHead = null
        blocksPlacedNearby = 0; lastBlockPlaceTime = 0
        isInLiquid = false; isOnLadder = false
        isOnIce = false; isOnSlime = false
        tick = 0; timestamp = 0
    }

    fun onBlockPlace(pos: BlockPos) {
        blocksPlacedNearby++
        lastBlockPlaceTime = System.currentTimeMillis()
    }
}
