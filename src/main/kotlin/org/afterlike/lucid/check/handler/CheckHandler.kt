package org.afterlike.lucid.check.handler

import best.azura.eventbus.handler.EventHandler
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayer
import org.afterlike.lucid.core.Lucid
import org.afterlike.lucid.check.api.BaseCheck
import org.afterlike.lucid.check.impl.AutoBlockCheck
import org.afterlike.lucid.check.impl.EagleCheck
import org.afterlike.lucid.check.impl.NoSlowCheck
import org.afterlike.lucid.check.impl.RotationCheck
import org.afterlike.lucid.check.impl.ScaffoldCheck
import org.afterlike.lucid.check.impl.SprintCheck
import org.afterlike.lucid.check.impl.VelocityCheck
import org.afterlike.lucid.core.type.EventPhase
import org.afterlike.lucid.core.event.game.GameTickEvent
import org.afterlike.lucid.core.event.world.WorldUnloadEvent
import org.afterlike.lucid.core.handler.PlayerSampleHandler
import org.apache.logging.log4j.LogManager
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger

object CheckHandler {

    private val logger = LogManager.getLogger(CheckHandler::class.java)

    private val checks = CopyOnWriteArrayList<BaseCheck>()
    private val activePlayerSet = Collections.newSetFromMap(ConcurrentHashMap<EntityPlayer, Boolean>())
    private val activeFutureMap = ConcurrentHashMap<EntityPlayer, Future<*>>()

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

    fun initialize() {
        logger.info("Registering checks...")

        val checkConstructors = listOf(
            ::AutoBlockCheck,
            ::EagleCheck,
            ::NoSlowCheck,
            ::RotationCheck,
            ::ScaffoldCheck,
            ::SprintCheck,
            ::VelocityCheck
        )

        var count = 0

        for (constructor in checkConstructors) {
            try {
                val check = constructor()
                registerCheck(check)
                count++
            } catch (e: Exception) {
                logger.error("Failed to init ${constructor::class.simpleName}", e)
            }
        }

        logger.info("Successfully initialized $count checks.")
    }


    @EventHandler
    fun onGameTick(event: GameTickEvent) {
        if (event.phase != EventPhase.POST) return

        try {
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

                        PlayerSampleHandler.collectSample(entity)

                        checkPlayer(entity)
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Error in client tick handler: ${e.message}")
        }
    }

    @EventHandler
    fun onWorldUnload(event: WorldUnloadEvent) {
        for (future in activeFutureMap.values) {
            if (!future.isDone && !future.isCancelled) {
                future.cancel(false)
            }
        }
        activeFutureMap.clear()
        activePlayerSet.clear()
        
        PlayerSampleHandler.removePlayer(null)
    }

    // submit player for async checks
    private fun checkPlayer(player: EntityPlayer) {
        activeFutureMap[player]?.let { future ->
            if (!future.isDone && !future.isCancelled) {
                future.cancel(false)
            }
        }

        val future = executorService.submit {
            try {
                runChecksForPlayer(player)
            } catch (e: Exception) {
                logger.error("Error in async check thread for {}: {}", player.name, e.message)
            } finally {
                activeFutureMap.remove(player)
            }
        }

        activeFutureMap[player] = future
    }

    private fun runChecksForPlayer(player: EntityPlayer) {
        for (check in checks) {
            if (check.enabled) {
                check.onCheckRun(player)
            }
        }
    }

    fun registerCheck(check: BaseCheck) {
        if (!checks.contains(check)) {
            Lucid.INSTANCE.eventBus.subscribe(check)
            checks.add(check)
        }
    }

    fun getChecks(): List<BaseCheck> {
        return checks.toList()
    }
}