package org.afterlike.lucid.check.impl

import net.minecraft.entity.player.EntityPlayer
import org.afterlike.lucid.check.api.AbstractCheck
import org.afterlike.lucid.core.event.world.EntityLeaveEvent
import org.afterlike.lucid.data.handler.impl.PlayerHandler
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.min

class RotationCheck : AbstractCheck() {
    override val name = "Rotation"
    override val description = "Detects illegal rotations"
    override var violationLevelThreshold = 12

    override val decayConfig = DecayConfig(
        baseRate = 1.0,
        mediumRate = 1.5,
        highRate = 2.0,
        criticalRate = 3.0,
        resetThreshold = 0.2
    )

    private val consecutiveViolations = ConcurrentHashMap<EntityPlayer, Int>()

    override fun onPlayerLeave(event: EntityLeaveEvent) {
        consecutiveViolations.remove(event.entity)
    }

    override fun onCheckRun(target: EntityPlayer) {
        val data = PlayerHandler.get(target) ?: return
        val ctx = data.player

        val pitch = ctx.pitch

        // we only check for illegal pitch
        if (abs(pitch) > 90) {
            val pitchExcess = abs(pitch) - 90.0

            val vlAmount = when {
                pitchExcess > 30 -> 12.0
                pitchExcess > 10 -> 8.0
                else -> 5.0
            }

            val severity = when {
                pitchExcess > 30 -> "extreme"
                pitchExcess > 10 -> "high"
                else -> "medium"
            }

            val consecutive = consecutiveViolations.getOrDefault(target, 0) + 1
            consecutiveViolations[target] = consecutive

            val consecutiveMultiplier = 1.0 + (min(consecutive, 5) * 0.2)
            val finalVL = vlAmount * consecutiveMultiplier

            addVL(
                target, finalVL,
                "illegal-pitch | pitch=${"%.1f".format(pitch)}° | severity=$severity | limit=±90° | excess=${
                    "%.1f".format(
                        pitchExcess
                    )
                }° | consecutive=$consecutive"
            )
        } else {
            if (handleNoViolation(target)) {
                consecutiveViolations[target] = 0
            }
        }
    }
}
