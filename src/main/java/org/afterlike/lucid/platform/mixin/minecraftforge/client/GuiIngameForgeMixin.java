package org.afterlike.lucid.platform.mixin.minecraftforge.client;

import net.minecraftforge.client.GuiIngameForge;
import org.afterlike.lucid.Lucid;
import org.afterlike.lucid.event.impl.RenderOverlayEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiIngameForge.class)
public class GuiIngameForgeMixin {
	@Inject(method = "renderGameOverlay",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraftforge/client/GuiIngameForge;renderTitle(IIF)V",
					shift = At.Shift.AFTER, remap = false))
	private void renderGameOverlay(final float partialTicks, final CallbackInfo callbackInfo) {
		Lucid.get().getEventBus().post(new RenderOverlayEvent(partialTicks));
	}
}