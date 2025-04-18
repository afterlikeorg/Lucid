package org.afterlike.lucid.check

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.Packet
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.util.BlockPos
import org.afterlike.lucid.util.Config
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.sqrt

class VelocityCheck : Check() {
    override val name = "Velocity"
    override val description = "Detects players who manipulate or reduce horizontal or vertical knockback"

    private data class HitEntry(
        val startTick: Long,
        val startPos: Triple<Double, Double, Double>,
        val startMotionH: Double
    )

    private val pendingHits = ConcurrentHashMap<Int, HitEntry>()

    private val WINDOW_TICKS = 4

    private val MIN_KNOCKBACK_V = 0.42
    private val EXPECTED_KNOCKBACK_H_STILL = 0.62
    private val EXPECTED_KNOCKBACK_H_MOVING = 0.2
    private val MOVEMENT_THRESHOLD = 0.015

    private val BASE_VL = 2.0

    init {
        CheckManager.register(this)
        vlThreshold = 8
    }

    override fun onUpdate(target: EntityPlayer) {
        val currentSample = getPlayerSample(target) ?: return
        val prevSample = getPreviousSample(target) ?: return
        val history = getPlayerHistory(target)
        if (history.size < 2) return

        val prevHurt = prevSample.hurtTime
        val currHurt = currentSample.hurtTime

        if (prevHurt <= 0 && currHurt > 0) {
            val startMotionH = currentSample.velocity
            val startPos = Triple(currentSample.posX, currentSample.posY, currentSample.posZ)
            pendingHits[target.entityId] = HitEntry(
                currentSample.tick,
                startPos,
                startMotionH
            )
        }

        pendingHits.keys.removeIf { id ->
            currentSample.tick - (pendingHits[id]?.startTick ?: 0) > WINDOW_TICKS * 2
        }

        pendingHits[target.entityId]?.let { entry ->
            if (currentSample.tick - entry.startTick >= WINDOW_TICKS) {
                if (!shouldIgnore(target, history) && !checkSurroundingBlocks(target)) {
                    val dx = currentSample.posX - entry.startPos.first
                    val dy = currentSample.posY - entry.startPos.second
                    val dz = currentSample.posZ - entry.startPos.third
                    val horizontalDist = sqrt(dx * dx + dz * dz)
                    val verticalDist = abs(dy)

                    val expectedH = if (entry.startMotionH <= MOVEMENT_THRESHOLD)
                        EXPECTED_KNOCKBACK_H_STILL
                    else
                        EXPECTED_KNOCKBACK_H_MOVING

                    // Horizontal check
                    if (horizontalDist < expectedH) {
                        val ratio = horizontalDist / expectedH
                        val vlToAdd = (1.0 - ratio) * BASE_VL * calculateSeverityMultiplier(ratio)
                        
                        addVL(
                            target,
                            vlToAdd,
                            "reduced horizontal knockback | moved=${"%.3f".format(horizontalDist)} | " +
                                    "expected≥${"%.3f".format(expectedH)} | ratio=${"%.0f".format(ratio * 100)}% | " +
                                    "vl=${"%.1f".format(vlToAdd)}"
                        )
                    }

                    // Vertical check
                    if (verticalDist < MIN_KNOCKBACK_V) {
                        val ratio = verticalDist / MIN_KNOCKBACK_V
                        val vlToAdd = (1.0 - ratio) * BASE_VL * calculateSeverityMultiplier(ratio)
                        
                        addVL(
                            target,
                            vlToAdd,
                            "reduced vertical knockback | moved=${"%.3f".format(verticalDist)} | " +
                                    "expected≥${"%.3f".format(MIN_KNOCKBACK_V)} | ratio=${"%.0f".format(ratio * 100)}% | " +
                                    "vl=${"%.1f".format(vlToAdd)}"
                        )
                    }
                }
                pendingHits.remove(target.entityId)
            }
        }
    }

    private fun calculateSeverityMultiplier(ratio: Double): Double {
        return when {
            ratio < 0.2 -> 2.0  // Less than 20% of expected - very severe
            ratio < 0.4 -> 1.5  // 20-40% of expected - severe
            ratio < 0.6 -> 1.2  // 40-60% of expected - moderate
            else -> 1.0         // 60-100% of expected - minor
        }
    }

    override fun onPacket(packet: Packet<*>) {
        if (packet is C02PacketUseEntity && packet.action == C02PacketUseEntity.Action.ATTACK) {
            pendingHits.remove(packet.entityId)
        }
    }

    private fun shouldIgnore(player: EntityPlayer, history: List<PlayerSample>): Boolean {
        // Check if player is burning
        if (player.isBurning) return true
        
        // Check if player recently fell
        if (history.size >= 6) {
            val recentHistory = history.takeLast(6)
            val hasStartFall = recentHistory.zipWithNext().any { (prev, curr) -> 
                prev.onGround && curr.motionY < -0.1 && !curr.onGround 
            }
            
            val hasStopFall = recentHistory.zipWithNext().any { (prev, curr) -> 
                !prev.onGround && curr.onGround 
            }
            
            val fallDetected = hasStartFall && hasStopFall
            
            return fallDetected
        }
        
        return false
    }

    private fun checkSurroundingBlocks(player: EntityPlayer): Boolean {
        val world = player.worldObj
        val pos = player.position
        val offsets = listOf(
            BlockPos(0, 0, 1), BlockPos(0, 0, -1),
            BlockPos(1, 0, 0), BlockPos(-1, 0, 0)
        )
        for (off in offsets) {
            val side = pos.add(off)
            if (!world.getBlockState(side).block.isAir(world, side)
                || !world.getBlockState(side.up()).block.isAir(world, side.up())
            ) return true
        }
        return false
    }
    
    override fun onPlayerRemove(player: EntityPlayer?) {
        if (player != null) {
            pendingHits.remove(player.entityId)
        } else {
            pendingHits.clear()
        }
        super.onPlayerRemove(player)
    }
}

