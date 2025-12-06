package org.afterlike.lucid.data.context.impl

import net.minecraft.entity.player.EntityPlayer
import org.afterlike.lucid.data.context.api.AbstractContext

// tracks network connection and server stability
class NetworkContext : AbstractContext() {

    var totalPacketsReceived = 0L
    var packetsThisSecond = 0
    var packetsLastSecond = 0
    private var lastSecondTimestamp = 0L

    var estimatedTps = 20.0
    var lastTickTime = 0L
    var ticksSinceLastUpdate = 0

    override fun update(player: EntityPlayer, worldTick: Long) {
        tick = worldTick
        timestamp = System.currentTimeMillis()

        // update packets per second
        if (timestamp - lastSecondTimestamp >= 1000) {
            packetsLastSecond = packetsThisSecond
            packetsThisSecond = 0
            lastSecondTimestamp = timestamp
        }

        // estimate tps
        if (lastTickTime > 0) {
            val tickDelta = timestamp - lastTickTime
            if (tickDelta > 0) {
                ticksSinceLastUpdate++
                if (ticksSinceLastUpdate >= 20) {
                    estimatedTps = (ticksSinceLastUpdate * 1000.0) / tickDelta
                    ticksSinceLastUpdate = 0
                }
            }
        }
        lastTickTime = timestamp
    }

    override fun reset() {
        totalPacketsReceived = 0
        packetsThisSecond = 0
        packetsLastSecond = 0
        lastSecondTimestamp = 0
        estimatedTps = 20.0
        lastTickTime = 0
        ticksSinceLastUpdate = 0
        tick = 0; timestamp = 0
    }

    fun onPacketReceived() {
        totalPacketsReceived++
        packetsThisSecond++
    }
}
