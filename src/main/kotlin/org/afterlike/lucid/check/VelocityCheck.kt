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
        val startMotionH: Double,
        val wasInAir: Boolean,
        val consecutiveHits: Int
    )

    private val pendingHits = ConcurrentHashMap<Int, HitEntry>()
    private val lastHitTimes = ConcurrentHashMap<Int, Long>()
    private val hitCounter = ConcurrentHashMap<Int, Int>()

    private val WINDOW_TICKS = 5
    private val COMBO_WINDOW_TICKS = 20
    
    // Lowered vertical threshold for better detection
    private val MIN_KNOCKBACK_V = 0.35
    private val EXPECTED_KNOCKBACK_H_STILL = 0.62
    private val EXPECTED_KNOCKBACK_H_MOVING = 0.2
    private val MOVEMENT_THRESHOLD = 0.015
    
    // Lower BASE_VL means violations accumulate more slowly
    private val BASE_VL = 1.5

    init {
        CheckManager.register(this)
        // Increase VL threshold to require more violations
        vlThreshold = 12
    }

    override fun onUpdate(target: EntityPlayer) {
        val currentSample = getPlayerSample(target) ?: return
        val prevSample = getPreviousSample(target) ?: return
        val history = getPlayerHistory(target)
        if (history.size < 2) return

        val prevHurt = prevSample.hurtTime
        val currHurt = currentSample.hurtTime
        val currentTick = currentSample.tick

        // Track consecutive hits
        if (prevHurt <= 0 && currHurt > 0) {
            val lastHitTime = lastHitTimes.getOrDefault(target.entityId, 0L)
            val timeSinceLastHit = currentTick - lastHitTime
            
            // Update consecutive hit counter
            if (timeSinceLastHit <= COMBO_WINDOW_TICKS) {
                hitCounter[target.entityId] = hitCounter.getOrDefault(target.entityId, 0) + 1
            } else {
                hitCounter[target.entityId] = 1
            }
            
            lastHitTimes[target.entityId] = currentTick
            
            val startMotionH = currentSample.velocity
            val startPos = Triple(currentSample.posX, currentSample.posY, currentSample.posZ)
            pendingHits[target.entityId] = HitEntry(
                currentSample.tick,
                startPos,
                startMotionH,
                !currentSample.onGround,
                hitCounter.getOrDefault(target.entityId, 1)
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

                    // Adjust expected knockback based on player state
                    val isMovingFast = entry.startMotionH > 0.1
                    val expectedH = when {
                        entry.consecutiveHits > 1 -> EXPECTED_KNOCKBACK_H_MOVING * 0.8  // Reduce expected knockback for combos
                        isMovingFast -> EXPECTED_KNOCKBACK_H_MOVING
                        else -> EXPECTED_KNOCKBACK_H_STILL
                    }
                    
                    // Adjust vertical knockback expectation for players in air (combos)
                    val expectedV = if (entry.wasInAir || entry.consecutiveHits > 1) {
                        MIN_KNOCKBACK_V * 0.7  // Reduce expected vertical knockback for air hits
                    } else {
                        MIN_KNOCKBACK_V
                    }

                    // Horizontal check - only if ratio is very low (more lenient)
                    if (horizontalDist < expectedH * 0.5) {
                        val ratio = horizontalDist / expectedH
                        // Lower VL addition for consecutive hits
                        val vlToAdd = (1.0 - ratio) * BASE_VL * calculateSeverityMultiplier(ratio) * 
                                getConsecutiveHitMultiplier(entry.consecutiveHits)
                        
                        addVL(
                            target,
                            vlToAdd,
                            "reduced horizontal knockback | moved=${"%.3f".format(horizontalDist)} | " +
                                    "expected≥${"%.3f".format(expectedH)} | ratio=${"%.0f".format(ratio * 100)}% | " +
                                    "combo=${entry.consecutiveHits} | vl=${"%.1f".format(vlToAdd)}"
                        )
                    }

                    // Vertical check - only if ratio is very low and not in combo
                    if (verticalDist < expectedV * 0.5 && entry.consecutiveHits <= 1 && !entry.wasInAir) {
                        val ratio = verticalDist / expectedV
                        val vlToAdd = (1.0 - ratio) * BASE_VL * calculateSeverityMultiplier(ratio)
                        
                        addVL(
                            target,
                            vlToAdd,
                            "reduced vertical knockback | moved=${"%.3f".format(verticalDist)} | " +
                                    "expected≥${"%.3f".format(expectedV)} | ratio=${"%.0f".format(ratio * 100)}% | " +
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
            ratio < 0.1 -> 2.0  // Less than 10% of expected - very severe
            ratio < 0.3 -> 1.5  // 10-30% of expected - severe
            ratio < 0.5 -> 1.0  // 30-50% of expected - moderate
            else -> 0.5         // Over 50% - minor
        }
    }
    
    private fun getConsecutiveHitMultiplier(hits: Int): Double {
        return when {
            hits >= 3 -> 0.3  // Third or more hit in combo - very low VL
            hits == 2 -> 0.5  // Second hit in combo - low VL
            else -> 1.0       // First hit - normal VL
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
            
            if (fallDetected) return true
        }
        
        // Check if player is moving too fast (possibly speed/velocity potion effect)
        if (history.size >= 3) {
            val recentHistory = history.takeLast(3)
            val hasHighSpeed = recentHistory.any { it.velocity > 0.4 }
            if (hasHighSpeed) return true
        }
        
        // Check for player directional change which can reduce knockback
        if (history.size >= 4) {
            val samples = history.takeLast(4)
            val directionChanges = samples.zipWithNext().count { (a, b) ->
                val angleA = getMoveAngle(a.deltaX, a.deltaZ)
                val angleB = getMoveAngle(b.deltaX, b.deltaZ)
                val diff = abs((angleA - angleB + 180) % 360 - 180)
                diff > 90
            }
            if (directionChanges >= 2) return true
        }
        
        return false
    }

    private fun checkSurroundingBlocks(player: EntityPlayer): Boolean {
        val world = player.worldObj
        val pos = player.position
        val offsets = listOf(
            BlockPos(0, 0, 1), BlockPos(0, 0, -1),
            BlockPos(1, 0, 0), BlockPos(-1, 0, 0),
            BlockPos(1, 0, 1), BlockPos(-1, 0, -1),
            BlockPos(1, 0, -1), BlockPos(-1, 0, 1)
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
            lastHitTimes.remove(player.entityId)
            hitCounter.remove(player.entityId)
        } else {
            pendingHits.clear()
            lastHitTimes.clear()
            hitCounter.clear()
        }
        super.onPlayerRemove(player)
    }
}

