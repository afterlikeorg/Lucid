package org.afterlike.lucid.platform.mixin.network;

import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import org.afterlike.lucid.core.Lucid;
import org.afterlike.lucid.core.event.network.ReceivePacketEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetworkManager.class)
public class NetworkManagerMixin {
    @Inject(method = "channelRead0*", at = @At("HEAD"))
    private <T extends Packet<?>> void onChannelRead(ChannelHandlerContext ctx, T packet, CallbackInfo ci) {
        Lucid.getINSTANCE().getEventBus().post(new ReceivePacketEvent(packet));
    }
}