package org.afterlike.lucid.check

import net.minecraft.network.Packet

object PacketHandler {
    @JvmStatic
    fun handle(packet: Packet<*>) {
        CheckManager.handlePacket(packet)
    }
}