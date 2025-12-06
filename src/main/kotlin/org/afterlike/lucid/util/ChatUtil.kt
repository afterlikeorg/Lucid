package org.afterlike.lucid.util

import net.minecraft.client.Minecraft
import net.minecraft.util.ChatComponentText
import net.minecraft.util.IChatComponent
import org.afterlike.lucid.core.handler.ConfigHandler

object ChatUtil {
    private val mc = Minecraft.getMinecraft()

    fun sendMessage(message: String) {
        if (mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(ChatComponentText(message))
        }
    }

    fun sendMessage(message: IChatComponent) {
        if (mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(message)
        }
    }

    fun sendDebug(message: String) {
        if (ConfigHandler.debugMode) {
            sendMessage("§c[DEBUG] §r$message")
        }
    }

    fun sendChatAsPlayer(message: String) {
        if (mc.thePlayer != null) {
            mc.thePlayer.sendChatMessage(message)
        }
    }
}
