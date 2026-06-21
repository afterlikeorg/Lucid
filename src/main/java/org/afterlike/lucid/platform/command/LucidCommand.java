package org.afterlike.lucid.platform.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;

public class LucidCommand extends CommandBase {
	@Override
	public String getCommandName() {
		return "lucid";
	}

	@Override
	public String getCommandUsage(ICommandSender sender) {
		return "/lucid";
	}

	@Override
	public int getRequiredPermissionLevel() {
		return 0;
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) throws CommandException {
		// todo: add gui
	}
}
