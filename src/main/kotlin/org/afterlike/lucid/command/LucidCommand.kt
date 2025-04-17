package org.afterlike.lucid.command

import net.minecraft.client.Minecraft
import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender
import org.afterlike.lucid.gui.LucidGui
import org.afterlike.lucid.util.DelayedTask

class LucidCommand : CommandBase() {
    override fun getCommandName(): String = "lucid"

    override fun getCommandUsage(sender: ICommandSender): String = "/lucid"

    override fun getRequiredPermissionLevel(): Int = 0

    override fun processCommand(sender: ICommandSender, args: Array<String>) {
        DelayedTask(1) {
            Minecraft.getMinecraft().displayGuiScreen(LucidGui())
        }
    }
}