package org.afterlike.lucid.platform.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import org.afterlike.lucid.core.Lucid;
import org.afterlike.lucid.core.type.EventPhase;
import org.afterlike.lucid.core.event.game.GameTickEvent;
import org.afterlike.lucid.core.event.world.WorldLoadEvent;
import org.afterlike.lucid.core.event.world.WorldUnloadEvent;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Inject(method = "startGame", at = @At("HEAD"))
    private void startGame$head(final @NotNull CallbackInfo callbackInfo) {
        Lucid.getINSTANCE().initialize();
    }

    @Inject(method = "startGame", at = @At(value = "CONSTANT", args = "stringValue=Post startup"))
    private void startGame$ldc$PostStartup(final @NotNull CallbackInfo callbackInfo) {
        Lucid.getINSTANCE().lateInitialize();
    }

    @Inject(method = "runTick", at = @At("HEAD"))
    private void runTick$head(final @NotNull CallbackInfo callbackInfo) {
        Lucid.getINSTANCE().getEventBus().post(new GameTickEvent(EventPhase.PRE));
    }

    @Inject(method = "runTick", at = @At("TAIL"))
    private void runTick$tail(final @NotNull CallbackInfo callbackInfo) {
        Lucid.getINSTANCE().getEventBus().post(new GameTickEvent(EventPhase.POST));
    }

    @Inject(
            method = {"loadWorld(Lnet/minecraft/client/multiplayer/WorldClient;Ljava/lang/String;)V"},
            at = {@At("HEAD")}
    )
    private void loadWorld$head(WorldClient worldClient, String string, CallbackInfo callbackInfo) {
        if (worldClient == null) {
            Lucid.getINSTANCE().getEventBus().post(new WorldUnloadEvent());
        } else {
            Lucid.getINSTANCE().getEventBus().post(new WorldLoadEvent());
        }
    }
}
