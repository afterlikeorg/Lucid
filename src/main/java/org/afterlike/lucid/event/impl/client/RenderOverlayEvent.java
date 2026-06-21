package org.afterlike.lucid.event.impl.client;

import re.tsuku.fastbus.Event;

public class RenderOverlayEvent implements Event {
	private final float partialTicks;
	public RenderOverlayEvent(final float partialTicks) {
		this.partialTicks = partialTicks;
	}

	public float getPartialTicks() {
		return partialTicks;
	}
}
