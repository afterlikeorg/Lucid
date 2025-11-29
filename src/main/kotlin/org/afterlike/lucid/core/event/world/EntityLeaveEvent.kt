package org.afterlike.lucid.core.event.world

import best.azura.eventbus.core.Event
import net.minecraft.entity.Entity

class EntityLeaveEvent(val entity: Entity) : Event