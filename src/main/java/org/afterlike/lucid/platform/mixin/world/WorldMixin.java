package org.afterlike.lucid.platform.mixin.world;

import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.afterlike.lucid.core.Lucid;
import org.afterlike.lucid.core.event.world.EntityLeaveEvent;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(World.class)
public class WorldMixin {

    @Inject(method = "onEntityAdded", at = @At("HEAD"))
    private void onEntityAdded$head(final @NotNull Entity entity, final @NotNull CallbackInfo callbackInfo) {
        Lucid.getINSTANCE().getEventBus().post(new EntityLeaveEvent(entity));
    }

    @Inject(method = "onEntityRemoved", at = @At("HEAD"))
    private void onEntityRemoved$head(final @NotNull Entity entity, final @NotNull CallbackInfo callbackInfo) {
        Lucid.getINSTANCE().getEventBus().post(new EntityLeaveEvent(entity));
    }

}