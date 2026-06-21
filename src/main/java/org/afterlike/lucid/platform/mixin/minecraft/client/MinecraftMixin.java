package org.afterlike.lucid.platform.mixin.minecraft.client;

import net.minecraft.client.Minecraft;
import org.afterlike.lucid.Lucid;
import org.afterlike.lucid.event.api.EventPhase;
import org.afterlike.lucid.event.impl.client.GameTickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
	@Inject(method = "startGame", at = @At("HEAD"))
	private void lucid$startGame$head(final CallbackInfo callbackInfo) {
		Lucid.get().initialize();
	}

	@Inject(method = "startGame", at = @At(value = "CONSTANT", args = "stringValue=Post startup"))
	private void lucid$startGame$postStartup(final CallbackInfo ci) {
		Lucid.get().lateInitialize();
	}

	@Inject(method = "runTick", at = @At("HEAD"))
	private void lucid$runTick$head(final CallbackInfo ci) {
		Lucid.get().getEventBus().post(new GameTickEvent(EventPhase.PRE));
	}

	@Inject(method = "runTick", at = @At("RETURN"))
	private void lucid$runTick$return(final CallbackInfo ci) {
		Lucid.get().getEventBus().post(new GameTickEvent(EventPhase.POST));
	}
}
