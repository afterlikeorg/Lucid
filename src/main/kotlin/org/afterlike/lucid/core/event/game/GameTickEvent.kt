package org.afterlike.lucid.core.event.game

import best.azura.eventbus.core.Event
import org.afterlike.lucid.core.type.EventPhase

class GameTickEvent(val phase: EventPhase) : Event