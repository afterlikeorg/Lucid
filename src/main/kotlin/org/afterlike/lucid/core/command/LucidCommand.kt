package org.afterlike.lucid.core.command

import net.minecraft.client.Minecraft
import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender
import org.afterlike.lucid.core.handler.DelayedTaskHandler
import org.afterlike.lucid.gui.screen.LucidGuiScreen

class LucidCommand : CommandBase() {
    override fun getCommandName(): String = "lucid"

    override fun getCommandUsage(sender: ICommandSender): String = "/lucid"

    override fun getRequiredPermissionLevel(): Int = 0

    override fun processCommand(sender: ICommandSender, args: Array<String>) {
        DelayedTaskHandler.schedule(1) {
            Minecraft.getMinecraft().displayGuiScreen(LucidGuiScreen())
        }
    }
}