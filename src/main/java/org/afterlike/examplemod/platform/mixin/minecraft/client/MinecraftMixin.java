package org.afterlike.examplemod.platform.mixin.minecraft.client;

import net.minecraft.client.Minecraft;
import org.afterlike.examplemod.ExampleMod;
import org.afterlike.examplemod.event.api.EventPhase;
import org.afterlike.examplemod.event.impl.GameTickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
	@Inject(method = "startGame", at = @At("HEAD"))
	private void startGame$head(final CallbackInfo callbackInfo) {
		ExampleMod.get().initialize();
	}

	@Inject(method = "startGame", at = @At(value = "CONSTANT", args = "stringValue=Post startup"))
	private void ou$startGame$postStartup(final CallbackInfo ci) {
		ExampleMod.get().lateInitialize();
	}

	@Inject(method = "runTick", at = @At("RETURN"))
	private void ou$runTick$return(final CallbackInfo ci) {
		ExampleMod.get().getEventBus().post(new GameTickEvent(EventPhase.POST));
	}
}
