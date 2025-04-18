package org.afterlike.lucid.util

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import java.util.LinkedList

/**
 * Tracks server TPS
 */
object TPSTracker {
    private val tpsHistory = LinkedList<Double>()
    private var lastTickTime = System.currentTimeMillis()
    private const val MAX_SAMPLES = 120
    private const val DEFAULT_TPS = 20.0
    
    /**
     * Gets the average TPS over the last few seconds.
     * Returns 20.0 if not enough data is available.
     */
    fun getAverageTPS(): Double {
        if (tpsHistory.isEmpty()) return DEFAULT_TPS
        val recentSamples = tpsHistory.takeLast(minOf(20, tpsHistory.size))
        return recentSamples.average()
    }
    
    /**
     * Updates the TPS calculation on server tick
     */
    @SubscribeEvent
    fun onServerTick(event: TickEvent.ServerTickEvent) {
        if (event.phase != TickEvent.Phase.END) return
        
        val currentTime = System.currentTimeMillis()
        val deltaTime = currentTime - lastTickTime
        
        if (deltaTime > 0) {
            val currentTPS = 1000.0 / deltaTime
            val cappedTPS = minOf(currentTPS, DEFAULT_TPS)
            
            tpsHistory.add(cappedTPS)
            
            while (tpsHistory.size > MAX_SAMPLES) {
                tpsHistory.removeFirst()
            }
        }
        
        lastTickTime = currentTime
    }
    
    /**
     * Estimate the current server lag in milliseconds
     */
    fun getServerLag(): Int {
        val currentTPS = getAverageTPS()
        if (currentTPS >= DEFAULT_TPS) return 0
        
        return ((1000.0 / currentTPS) - (1000.0 / DEFAULT_TPS)).toInt()
    }
} 