package org.afterlike.lucid.check

import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.Packet
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.PlayerEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger

object CheckManager {

    private val checks = CopyOnWriteArrayList<Check>()

    private val playerDimensions = ConcurrentHashMap<EntityPlayer, Int>()

    private val activePlayerSet = Collections.newSetFromMap(ConcurrentHashMap<EntityPlayer, Boolean>())

    private var lastProcessedTick = 0L

    private var lastSampleCollectionTime = 0L
    private const val SAMPLE_THROTTLE_MS = 16

    private val mc = Minecraft.getMinecraft()

    private val executorService = Executors.newFixedThreadPool(
        2,
        object : ThreadFactory {
            private val threadNumber = AtomicInteger(1)
            override fun newThread(r: Runnable): Thread {
                val thread = Thread(r, "Lucid-Check-Thread-${threadNumber.getAndIncrement()}")
                thread.isDaemon = true
                thread.priority = Thread.NORM_PRIORITY - 1
                return thread
            }
        }
    )

    private val activeFutures = ConcurrentHashMap<EntityPlayer, Future<*>>()

    fun register(check: Check) {
        try {
            if (!checks.contains(check)) {
                checks.add(check)
            }
        } catch (e: Exception) {
            System.err.println("[Lucid] Error registering check: ${e.message}")
        }
    }

    fun handlePacket(packet: Packet<*>) {
        try {
            if (checks.isEmpty()) return

            var processed = false

            for (check in checks) {
                try {
                    if (check.enabled) {
                        check.onPacket(packet)
                        processed = true
                    }
                } catch (e: Exception) {
                    System.err.println("[Lucid] Error in check ${check.name} while processing packet: ${e.message}")
                }
            }

            if (!processed) return
        } catch (e: Exception) {
            System.err.println("[Lucid] Error in packet handler: ${e.message}")
        }
    }

    @SubscribeEvent
    fun onClientTick(event: TickEvent.ClientTickEvent) {
        try {
            if (event.phase != TickEvent.Phase.END) return

            if (checks.isEmpty()) return

            val thePlayer = mc.thePlayer ?: return
            val theWorld = mc.theWorld ?: return

            val currentTick = theWorld.totalWorldTime
            lastProcessedTick = currentTick

            val currentTime = System.currentTimeMillis()
            val shouldCollectSamples = currentTime - lastSampleCollectionTime >= SAMPLE_THROTTLE_MS

            if (shouldCollectSamples) {
                lastSampleCollectionTime = currentTime

                activePlayerSet.clear()

                for (entity in theWorld.playerEntities) {
                    if (entity !== thePlayer && entity.isEntityAlive) {
                        activePlayerSet.add(entity)

                        try {
                            val currentDimension = entity.dimension
                            val previousDimension = playerDimensions.put(entity, currentDimension)

                            if (previousDimension != null && previousDimension != currentDimension) {
                                onPlayerDimensionChange(entity)
                                continue // Skip this player for this tick
                            }
                        } catch (e: Exception) {
                            System.err.println("[Lucid] Error tracking dimension for player ${entity.name}: ${e.message}")
                            continue
                        }

                        PlayerDataManager.collectSample(entity)

                        submitPlayerForAsyncChecks(entity)
                    }
                }

                cleanupDisconnectedPlayers()
            }
        } catch (e: Exception) {
            System.err.println("[Lucid] Error in client tick handler: ${e.message}")
        }
    }

    private fun submitPlayerForAsyncChecks(player: EntityPlayer) {
        activeFutures[player]?.let { future ->
            if (!future.isDone && !future.isCancelled) {
                future.cancel(false)
            }
        }

        val future = executorService.submit {
            try {
                runChecksForPlayer(player)
            } catch (e: Exception) {
                System.err.println("[Lucid] Error in async check thread for ${player.name}: ${e.message}")
            } finally {
                activeFutures.remove(player)
            }
        }

        activeFutures[player] = future
    }

    private fun runChecksForPlayer(player: EntityPlayer) {
        for (check in checks) {
            if (check.enabled) {
                try {
                    check.onUpdate(player)
                } catch (e: Exception) {
                    System.err.println("[Lucid] Error in check ${check.name} for player ${player.name}: ${e.message}")
                }
            }
        }
    }

    @SubscribeEvent
    fun onPlayerLogout(event: PlayerEvent.PlayerLoggedOutEvent) {
        try {
            val player = event.player
            if (player != null) {
                activeFutures[player]?.let { future ->
                    if (!future.isDone && !future.isCancelled) {
                        future.cancel(false)
                    }
                }
                activeFutures.remove(player)

                PlayerDataManager.removePlayer(player)

                for (check in checks) {
                    try {
                        check.onPlayerRemove(player)
                    } catch (e: Exception) {
                        System.err.println("[Lucid] Error cleaning up player ${player.name} in check ${check.name}: ${e.message}")
                    }
                }

                playerDimensions.remove(player)
                activePlayerSet.remove(player)
            }
        } catch (e: Exception) {
            System.err.println("[Lucid] Error in player logout handler: ${e.message}")
        }
    }

    @SubscribeEvent
    fun onWorldUnload(event: WorldEvent.Unload) {
        try {
            if (!event.world.isRemote) return

            for (future in activeFutures.values) {
                if (!future.isDone && !future.isCancelled) {
                    future.cancel(false)
                }
            }
            activeFutures.clear()

            playerDimensions.clear()
            activePlayerSet.clear()

            PlayerDataManager.removePlayer(null)

            for (check in checks) {
                try {
                    check.onPlayerRemove(null)
                } catch (e: Exception) {
                    System.err.println("[Lucid] Error cleaning up in check ${check.name}: ${e.message}")
                }
            }
        } catch (e: Exception) {
            System.err.println("[Lucid] Error in world unload handler: ${e.message}")
        }
    }

    private fun onPlayerDimensionChange(player: EntityPlayer) {
        activeFutures[player]?.let { future ->
            if (!future.isDone && !future.isCancelled) {
                future.cancel(false)
            }
        }
        activeFutures.remove(player)

        PlayerDataManager.removePlayer(player)

        for (check in checks) {
            try {
                if (check.enabled) {
                    check.onPlayerRemove(player)
                }
            } catch (e: Exception) {
                System.err.println("[Lucid] Error handling dimension change for ${player.name} in check ${check.name}: ${e.message}")
            }
        }
    }

    private fun cleanupDisconnectedPlayers() {
        try {
            val toRemove = ArrayList<EntityPlayer>(4)

            for (player in playerDimensions.keys) {
                if (!activePlayerSet.contains(player)) {
                    toRemove.add(player)
                }
            }

            for (player in toRemove) {
                activeFutures[player]?.let { future ->
                    if (!future.isDone && !future.isCancelled) {
                        future.cancel(false)
                    }
                }
                activeFutures.remove(player)

                PlayerDataManager.removePlayer(player)

                for (check in checks) {
                    try {
                        check.onPlayerRemove(player)
                    } catch (e: Exception) {
                        System.err.println("[Lucid] Error cleaning up player ${player.name} in check ${check.name}: ${e.message}")
                    }
                }

                playerDimensions.remove(player)
            }
        } catch (e: Exception) {
            System.err.println("[Lucid] Error cleaning up disconnected players: ${e.message}")
        }
    }

    fun allChecks(): List<Check> {
        return checks.toList()
    }

    fun shutdown() {
        try {
            for (future in activeFutures.values) {
                if (!future.isDone && !future.isCancelled) {
                    future.cancel(false)
                }
            }
            activeFutures.clear()

            executorService.shutdown()
        } catch (e: Exception) {
            System.err.println("[Lucid] Error shutting down thread pool: ${e.message}")
        }
    }
}