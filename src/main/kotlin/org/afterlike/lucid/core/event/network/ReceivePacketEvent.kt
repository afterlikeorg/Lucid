package org.afterlike.lucid.core.event.network

import best.azura.eventbus.core.Event
import net.minecraft.network.Packet

class ReceivePacketEvent(val packet: Packet<*>) : Event