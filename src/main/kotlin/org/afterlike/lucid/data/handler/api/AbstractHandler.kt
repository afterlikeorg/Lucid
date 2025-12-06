package org.afterlike.lucid.data.handler.api

import net.minecraft.client.Minecraft

abstract class AbstractHandler {
    protected val mc: Minecraft = Minecraft.getMinecraft()
}
