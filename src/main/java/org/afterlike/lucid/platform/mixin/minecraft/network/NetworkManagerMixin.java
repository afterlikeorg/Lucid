package org.afterlike.lucid.platform.mixin.minecraft.network;

import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import org.afterlike.lucid.Lucid;
import org.afterlike.lucid.event.impl.network.ReceivePacketEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetworkManager.class)
public class NetworkManagerMixin {
	@Inject(method = "channelRead0*", at = @At("HEAD"))
	public void lucid$channelRead0(final ChannelHandlerContext ctx, final Packet<?> packet,
			final CallbackInfo ci) {
		Lucid.get().getEventBus().post(new ReceivePacketEvent(packet));
	}
}
