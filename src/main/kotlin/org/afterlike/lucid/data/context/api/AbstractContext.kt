package org.afterlike.lucid.data.context.api

import net.minecraft.entity.player.EntityPlayer

abstract class AbstractContext {
    var tick: Long = 0
    var timestamp: Long = 0

    abstract fun update(player: EntityPlayer, worldTick: Long)
    abstract fun reset()
}
