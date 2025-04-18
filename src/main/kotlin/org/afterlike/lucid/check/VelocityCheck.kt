package org.afterlike.lucid.check

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.Packet
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.util.BlockPos
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.sqrt

class VelocityCheck : Check() {
    override val name = "Velocity"
    override val description = "Detects players who manipulate or reduce horizontal or vertical knockback"

    private class HitEntry {
        var startTick: Long = 0
        var startPosX: Double = 0.0
        var startPosY: Double = 0.0
        var startPosZ: Double = 0.0
        var startMotionH: Double = 0.0
        var wasInAir: Boolean = false
        var consecutiveHits: Int = 0
    }

    private val hitEntryPool = mutableListOf<HitEntry>()
    private val pendingHits = ConcurrentHashMap<Int, HitEntry>()
    private val lastHitTimes = ConcurrentHashMap<Int, Long>()
    private val hitCounter = ConcurrentHashMap<Int, Int>()

    private data class BlockCacheKey(val worldId: Int, val x: Int, val y: Int, val z: Int)
    private data class BlockCacheEntry(val timestamp: Long, val isAir: Boolean)

    private val blockStateCache = ConcurrentHashMap<BlockCacheKey, BlockCacheEntry>()
    private var lastCleanupTime = System.currentTimeMillis()

    companion object {
        private const val WINDOW_TICKS = 5
        private const val COMBO_WINDOW_TICKS = 20

        private const val MIN_KNOCKBACK_V = 0.35
        private const val EXPECTED_KNOCKBACK_H_STILL = 0.62
        private const val EXPECTED_KNOCKBACK_H_MOVING = 0.2
        private const val MOVEMENT_THRESHOLD = 0.015

        private const val BASE_VL = 1.5
        private const val MAX_POOL_SIZE = 20

        private const val BLOCK_CACHE_SIZE = 512
        private const val BLOCK_CACHE_EXPIRY_MS = 500
        private const val CACHE_CLEANUP_INTERVAL_MS = 5000
    }

    init {
        CheckManager.register(this)
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
        val entityId = target.entityId

        cleanBlockCacheIfNeeded()

        if (prevHurt <= 0 && currHurt > 0) {
            val lastHitTime = lastHitTimes.getOrDefault(entityId, 0L)
            val timeSinceLastHit = currentTick - lastHitTime

            val hitCount = if (timeSinceLastHit <= COMBO_WINDOW_TICKS) {
                hitCounter.getOrDefault(entityId, 0) + 1
            } else {
                1
            }
            hitCounter[entityId] = hitCount

            lastHitTimes[entityId] = currentTick

            val entry = getHitEntry().apply {
                startTick = currentSample.tick
                startPosX = currentSample.posX
                startPosY = currentSample.posY
                startPosZ = currentSample.posZ
                startMotionH = currentSample.velocity
                wasInAir = !currentSample.onGround
                consecutiveHits = hitCount
            }

            pendingHits[entityId] = entry
        }

        val hitsToRemove = mutableListOf<Int>()
        for ((id, entry) in pendingHits) {
            if (currentSample.tick - entry.startTick > WINDOW_TICKS * 2) {
                hitsToRemove.add(id)
            }
        }
        for (id in hitsToRemove) {
            val entry = pendingHits.remove(id)
            if (entry != null) {
                recycleHitEntry(entry)
            }
        }

        pendingHits[entityId]?.let { entry ->
            if (currentSample.tick - entry.startTick >= WINDOW_TICKS) {
                if (!shouldIgnore(target, history) && !checkSurroundingBlocks(target)) {
                    val dx = currentSample.posX - entry.startPosX
                    val dy = currentSample.posY - entry.startPosY
                    val dz = currentSample.posZ - entry.startPosZ
                    val horizontalDist = sqrt(dx * dx + dz * dz)
                    val verticalDist = abs(dy)

                    val isMovingFast = entry.startMotionH > 0.1
                    val expectedH = when {
                        entry.consecutiveHits > 1 -> EXPECTED_KNOCKBACK_H_MOVING * 0.8
                        isMovingFast -> EXPECTED_KNOCKBACK_H_MOVING
                        else -> EXPECTED_KNOCKBACK_H_STILL
                    }

                    val expectedV = if (entry.wasInAir || entry.consecutiveHits > 1) {
                        MIN_KNOCKBACK_V * 0.7
                    } else {
                        MIN_KNOCKBACK_V
                    }

                    if (horizontalDist < expectedH * 0.5) {
                        val ratio = horizontalDist / expectedH
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
                val entry = pendingHits.remove(entityId)
                if (entry != null) {
                    recycleHitEntry(entry)
                }
            }
        }
    }

    private fun cleanBlockCacheIfNeeded() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastCleanupTime > CACHE_CLEANUP_INTERVAL_MS) {
            lastCleanupTime = currentTime

            val expiredKeys = mutableListOf<BlockCacheKey>()
            for ((key, entry) in blockStateCache) {
                if (currentTime - entry.timestamp > BLOCK_CACHE_EXPIRY_MS) {
                    expiredKeys.add(key)
                }
            }

            for (key in expiredKeys) {
                blockStateCache.remove(key)
            }

            if (blockStateCache.size > BLOCK_CACHE_SIZE) {
                val sortedEntries = blockStateCache.entries.sortedBy { it.value.timestamp }
                val toRemove = sortedEntries.take(blockStateCache.size - BLOCK_CACHE_SIZE)
                for (entry in toRemove) {
                    blockStateCache.remove(entry.key)
                }
            }
        }
    }

    private fun getHitEntry(): HitEntry {
        return if (hitEntryPool.isNotEmpty()) {
            hitEntryPool.removeAt(hitEntryPool.size - 1)
        } else {
            HitEntry()
        }
    }

    private fun recycleHitEntry(entry: HitEntry) {
        if (hitEntryPool.size < MAX_POOL_SIZE) {
            hitEntryPool.add(entry)
        }
    }

    private fun calculateSeverityMultiplier(ratio: Double): Double {
        return when {
            ratio < 0.1 -> 2.0
            ratio < 0.3 -> 1.5
            ratio < 0.5 -> 1.0
            else -> 0.5
        }
    }

    private fun getConsecutiveHitMultiplier(hits: Int): Double {
        return when {
            hits >= 3 -> 0.3
            hits == 2 -> 0.5
            else -> 1.0
        }
    }

    override fun onPacket(packet: Packet<*>) {
        if (packet is C02PacketUseEntity && packet.action == C02PacketUseEntity.Action.ATTACK) {
            val entry = pendingHits.remove(packet.entityId)
            if (entry != null) {
                recycleHitEntry(entry)
            }
        }
    }

    private fun shouldIgnore(player: EntityPlayer, history: List<PlayerSample>): Boolean {
        if (player.isBurning) return true

        if (history.size >= 6) {
            val recentHistory = history.takeLast(6)
            var hasStartFall = false
            var hasStopFall = false

            for (i in 1 until recentHistory.size) {
                val prev = recentHistory[i - 1]
                val curr = recentHistory[i]

                if (prev.onGround && curr.motionY < -0.1 && !curr.onGround) {
                    hasStartFall = true
                }

                if (!prev.onGround && curr.onGround) {
                    hasStopFall = true
                }
            }

            if (hasStartFall && hasStopFall) return true
        }

        if (history.size >= 3) {
            for (i in history.size - 3 until history.size) {
                if (history[i].velocity > 0.4) return true
            }
        }

        if (history.size >= 4) {
            val samples = history.takeLast(4)
            var directionChanges = 0
            for (i in 1 until samples.size) {
                val a = samples[i - 1]
                val b = samples[i]
                val angleA = getMoveAngle(a.deltaX, a.deltaZ)
                val angleB = getMoveAngle(b.deltaX, b.deltaZ)
                val diff = abs((angleA - angleB + 180) % 360 - 180)
                if (diff > 90) {
                    directionChanges++
                    if (directionChanges >= 2) return true
                }
            }
        }

        return false
    }

    private fun isAirCached(world: net.minecraft.world.World, pos: BlockPos): Boolean {
        val worldId = world.provider.dimensionId
        val key = BlockCacheKey(worldId, pos.x, pos.y, pos.z)

        val currentTime = System.currentTimeMillis()
        val cacheEntry = blockStateCache[key]

        if (cacheEntry != null && currentTime - cacheEntry.timestamp < BLOCK_CACHE_EXPIRY_MS) {
            return cacheEntry.isAir
        }

        val isAir = world.getBlockState(pos).block.isAir(world, pos)
        blockStateCache[key] = BlockCacheEntry(currentTime, isAir)

        return isAir
    }

    private fun checkSurroundingBlocks(player: EntityPlayer): Boolean {
        val world = player.worldObj
        val pos = player.position

        if (!isAirCached(world, pos.add(0, 0, 1)) ||
            !isAirCached(world, pos.add(0, 0, -1)) ||
            !isAirCached(world, pos.add(1, 0, 0)) ||
            !isAirCached(world, pos.add(-1, 0, 0))
        ) {
            return true
        }

        if (!isAirCached(world, pos.add(1, 0, 1)) ||
            !isAirCached(world, pos.add(-1, 0, -1)) ||
            !isAirCached(world, pos.add(1, 0, -1)) ||
            !isAirCached(world, pos.add(-1, 0, 1))
        ) {
            return true
        }

        if (!isAirCached(world, pos.up().add(0, 0, 1)) ||
            !isAirCached(world, pos.up().add(0, 0, -1)) ||
            !isAirCached(world, pos.up().add(1, 0, 0)) ||
            !isAirCached(world, pos.up().add(-1, 0, 0)) ||
            !isAirCached(world, pos.up().add(1, 0, 1)) ||
            !isAirCached(world, pos.up().add(-1, 0, -1)) ||
            !isAirCached(world, pos.up().add(1, 0, -1)) ||
            !isAirCached(world, pos.up().add(-1, 0, 1))
        ) {
            return true
        }

        return false
    }

    override fun onPlayerRemove(player: EntityPlayer?) {
        if (player != null) {
            val entry = pendingHits.remove(player.entityId)
            if (entry != null) {
                recycleHitEntry(entry)
            }
            lastHitTimes.remove(player.entityId)
            hitCounter.remove(player.entityId)
        } else {
            pendingHits.values.forEach { recycleHitEntry(it) }
            pendingHits.clear()
            lastHitTimes.clear()
            hitCounter.clear()
            blockStateCache.clear()
        }
        super.onPlayerRemove(player)
    }
}

