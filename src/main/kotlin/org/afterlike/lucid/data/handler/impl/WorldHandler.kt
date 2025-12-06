package org.afterlike.lucid.data.handler.impl

import best.azura.eventbus.handler.EventHandler
import net.minecraft.network.play.server.S22PacketMultiBlockChange
import net.minecraft.network.play.server.S23PacketBlockChange
import net.minecraft.util.BlockPos
import org.afterlike.lucid.core.event.network.ReceivePacketEvent
import org.afterlike.lucid.data.handler.api.AbstractHandler
import org.afterlike.lucid.util.ChatUtil

object WorldHandler : AbstractHandler() {

    private val recentBlockChanges = ArrayDeque<BlockChange>(100)
    var blockChangesThisSecond = 0
        private set
    private var lastSecondTimestamp = 0L
    private var lastLogTime = 0L

    @EventHandler
    fun onReceivePacket(event: ReceivePacketEvent) {
        val packet = event.packet
        val now = System.currentTimeMillis()

        if (now - lastSecondTimestamp >= 1000) {
            // log if there were significant block changes
            if (blockChangesThisSecond > 10 && now - lastLogTime > 2000) {
                ChatUtil.sendDebug("Block changes/sec: §e$blockChangesThisSecond")
                lastLogTime = now
            }
            blockChangesThisSecond = 0
            lastSecondTimestamp = now
        }

        when (packet) {
            is S23PacketBlockChange -> {
                recordBlockChange(packet.blockPosition, now)
            }

            is S22PacketMultiBlockChange -> {
                for (data in packet.changedBlocks) {
                    recordBlockChange(data.pos, now)
                }
            }
        }
    }

    private fun recordBlockChange(pos: BlockPos, time: Long) {
        blockChangesThisSecond++
        recentBlockChanges.addLast(BlockChange(pos, time))
        if (recentBlockChanges.size > 100) recentBlockChanges.removeFirst()

        // notify nearby player contexts
        mc.theWorld?.playerEntities?.forEach { player ->
            if (player != mc.thePlayer) {
                val dist = player.getDistanceSq(pos)
                if (dist < 25) { // within 5 blocks
                    PlayerHandler.get(player)?.world?.onBlockPlace(pos)
//                    ChatUtil.sendDebug("Block near §f${player.name}§7: §e${pos.x}, ${pos.y}, ${pos.z}")
                }
            }
        }
    }

    fun getRecentBlockChanges(withinMs: Long = 1000): List<BlockChange> {
        val cutoff = System.currentTimeMillis() - withinMs
        return recentBlockChanges.filter { it.time >= cutoff }
    }

    fun getBlockChangesNear(pos: BlockPos, radius: Double, withinMs: Long = 1000): List<BlockChange> {
        val radiusSq = radius * radius
        return getRecentBlockChanges(withinMs).filter { change ->
            val dx = change.pos.x - pos.x
            val dy = change.pos.y - pos.y
            val dz = change.pos.z - pos.z
            (dx * dx + dy * dy + dz * dz) <= radiusSq
        }
    }

    fun reset() {
        recentBlockChanges.clear()
        blockChangesThisSecond = 0
        lastSecondTimestamp = 0
    }

    data class BlockChange(val pos: BlockPos, val time: Long)
}
