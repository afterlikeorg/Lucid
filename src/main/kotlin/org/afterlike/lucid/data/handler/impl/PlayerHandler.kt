package org.afterlike.lucid.data.handler.impl

import best.azura.eventbus.handler.EventHandler
import net.minecraft.entity.player.EntityPlayer
import org.afterlike.lucid.core.event.world.EntityLeaveEvent
import org.afterlike.lucid.core.event.world.WorldUnloadEvent
import org.afterlike.lucid.data.context.impl.InteractionContext
import org.afterlike.lucid.data.context.impl.NetworkContext
import org.afterlike.lucid.data.context.impl.PlayerContext
import org.afterlike.lucid.data.context.impl.WorldContext
import org.afterlike.lucid.data.handler.api.AbstractHandler
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.ArrayDeque
import kotlin.collections.List
import kotlin.collections.elementAt
import kotlin.collections.forEach
import kotlin.collections.getOrPut
import kotlin.collections.indices
import kotlin.collections.toList

object PlayerHandler : AbstractHandler() {

    private val playerData = ConcurrentHashMap<UUID, PlayerData>()

    fun getOrCreate(player: EntityPlayer): PlayerData {
        return playerData.getOrPut(player.uniqueID) { PlayerData() }
    }

    fun get(player: EntityPlayer): PlayerData? = playerData[player.uniqueID]

    fun get(uuid: UUID): PlayerData? = playerData[uuid]

    fun update(player: EntityPlayer) {
        val worldTick = mc.theWorld?.totalWorldTime ?: 0
        getOrCreate(player).update(player, worldTick)
    }

    fun remove(player: EntityPlayer) {
        playerData.remove(player.uniqueID)?.reset()
    }

    fun clear() {
        playerData.values.forEach { it.reset() }
        playerData.clear()
    }

    fun hasData(player: EntityPlayer): Boolean = playerData.containsKey(player.uniqueID)

    @EventHandler
    fun onEntityLeave(event: EntityLeaveEvent) {
        if (event.entity is EntityPlayer) {
            remove(event.entity)
        }
    }

    @EventHandler
    fun onWorldUnload(event: WorldUnloadEvent) {
        clear()
    }

    class PlayerData {
        val player = PlayerContext()
        val world = WorldContext()
        val network = NetworkContext()
        val interaction = InteractionContext()

        private val playerHistory = ArrayDeque<PlayerContext>(MAX_HISTORY)

        fun update(entity: EntityPlayer, worldTick: Long) {
            // snapshot before updating
            if (player.tick > 0) {
                playerHistory.addLast(player.snapshot())
                if (playerHistory.size > MAX_HISTORY) playerHistory.removeFirst()
            }

            player.update(entity, worldTick)
            world.update(entity, worldTick)
            network.update(entity, worldTick)
            interaction.update(entity, worldTick)
        }

        fun reset() {
            player.reset()
            world.reset()
            network.reset()
            interaction.reset()
            playerHistory.clear()
        }

        fun getPlayerHistory(): List<PlayerContext> = playerHistory.toList()

        fun getPlayerAt(ticksAgo: Int): PlayerContext? {
            val index = playerHistory.size - 1 - ticksAgo
            return if (index in playerHistory.indices) playerHistory.elementAt(index) else null
        }

        private fun PlayerContext.snapshot(): PlayerContext = PlayerContext().also { c ->
            c.posX = posX; c.posY = posY; c.posZ = posZ
            c.prevPosX = prevPosX; c.prevPosY = prevPosY; c.prevPosZ = prevPosZ
            c.motionX = motionX; c.motionY = motionY; c.motionZ = motionZ
            c.yaw = yaw; c.pitch = pitch; c.prevYaw = prevYaw; c.prevPitch = prevPitch
            c.onGround = onGround; c.isSprinting = isSprinting; c.isSneaking = isSneaking
            c.isBlocking = isBlocking; c.isUsingItem = isUsingItem
            c.isSwingInProgress = isSwingInProgress; c.hurtTime = hurtTime
            c.swingProgress = swingProgress; c.prevSwingProgress = prevSwingProgress
            c.tick = tick; c.timestamp = timestamp
        }

        companion object {
            private const val MAX_HISTORY = 20
        }
    }
}
