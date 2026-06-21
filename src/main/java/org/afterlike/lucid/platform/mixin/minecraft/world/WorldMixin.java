package org.afterlike.lucid.platform.mixin.minecraft.world;

import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.afterlike.lucid.Lucid;
import org.afterlike.lucid.event.impl.world.EntityJoinEvent;
import org.afterlike.lucid.event.impl.world.EntityLeaveEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(World.class)
public class WorldMixin {
	@Inject(method = "onEntityAdded", at = @At("HEAD"))
	private void lucid$onEntityAdded(final Entity entity, final CallbackInfo ci) {
		Lucid.get().getEventBus().post(new EntityJoinEvent(entity));
	}

	@Inject(method = "onEntityRemoved", at = @At("HEAD"))
	private void lucid$onEntityRemoved(final Entity entity, final CallbackInfo ci) {
		Lucid.get().getEventBus().post(new EntityLeaveEvent(entity));
	}
}
