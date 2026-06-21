package org.afterlike.lucid.event.impl.world;

import net.minecraft.entity.Entity;
import re.tsuku.fastbus.Event;

public class EntityLeaveEvent implements Event {
	private final Entity entity;
	public EntityLeaveEvent(final Entity entity) {
		this.entity = entity;
	}

	public Entity getEntity() {
		return entity;
	}
}
