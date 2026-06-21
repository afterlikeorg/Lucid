package org.afterlike.lucid.event.impl.client;

import org.afterlike.lucid.event.api.EventPhase;
import re.tsuku.fastbus.Event;

public class GameTickEvent implements Event {
	private final EventPhase phase;
	public GameTickEvent(final EventPhase phase) {
		this.phase = phase;
	}

	public EventPhase getPhase() {
		return phase;
	}
}
