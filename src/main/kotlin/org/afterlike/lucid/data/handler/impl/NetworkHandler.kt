package org.afterlike.lucid.data.handler.impl

import best.azura.eventbus.handler.EventHandler
import net.minecraft.network.play.server.S03PacketTimeUpdate
import org.afterlike.lucid.core.event.network.ReceivePacketEvent
import org.afterlike.lucid.data.handler.api.AbstractHandler
import org.afterlike.lucid.util.ChatUtil


object NetworkHandler : AbstractHandler() {

    var serverTps = 20.0
        private set

    private var lastTimeUpdateTime = 0L
    private var lastWorldTime = 0L
    private val tpsSamples = ArrayDeque<Double>(20)
    private var lastTpsLogTime = 0L

    @EventHandler
    fun onReceivePacket(event: ReceivePacketEvent) {
        val packet = event.packet

        if (packet is S03PacketTimeUpdate) {
            val now = System.currentTimeMillis()
            if (lastTimeUpdateTime > 0) {
                val timeDelta = now - lastTimeUpdateTime
                if (timeDelta > 0) {
                    val tickDelta = packet.worldTime - lastWorldTime
                    if (tickDelta > 0) {
                        val tps = (tickDelta * 1000.0) / timeDelta
                        tpsSamples.addLast(tps.coerceIn(0.0, 20.0))
                        if (tpsSamples.size > 20) tpsSamples.removeFirst()

                        val oldTps = serverTps
                        serverTps = tpsSamples.average()

                        // log significant TPS changes
                        if (now - lastTpsLogTime > 5000 && kotlin.math.abs(serverTps - oldTps) > 1.0) {
                            ChatUtil.sendDebug("Server TPS: §e${"%.1f".format(serverTps)}")
                            lastTpsLogTime = now
                        }
                    }
                }
            }
            lastTimeUpdateTime = now
            lastWorldTime = packet.worldTime
        }

        PlayerHandler.get(mc.thePlayer)?.network?.onPacketReceived()
    }

    fun reset() {
        serverTps = 20.0
        lastTimeUpdateTime = 0
        lastWorldTime = 0
        tpsSamples.clear()
    }
}
