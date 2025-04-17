package org.afterlike.lucid.check

import net.minecraft.entity.player.EntityPlayer

class AutoBlockCheck : Check() {
    override val name = "AutoBlock"
    override val description = "Detects attacking while blocking with a sword"

    private val swingProgress = mutableMapOf<EntityPlayer, Float>()
    private var lastCleanupTime = System.currentTimeMillis()
    private val CLEANUP_INTERVAL = 30000L

    init {
        CheckManager.register(this)
        vlThreshold = 20
    }

    override fun onUpdate(target: EntityPlayer) {
        val mc = net.minecraft.client.Minecraft.getMinecraft()
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastCleanupTime > CLEANUP_INTERVAL) {
            cleanupOldData()
            lastCleanupTime = currentTime
        }

        if (target == mc.thePlayer) return

        val isBlocking = target.isBlocking
        val current = target.swingProgress
        val previous = swingProgress[target] ?: 0f

        if (isBlocking && current > 0f && previous == 0f) {
            addVL(target, 5.0, "attacking while blocking")
        } else {
            decayVL(target, 0.5)
        }

        swingProgress[target] = current
    }

    private fun cleanupOldData() {
        val mc = net.minecraft.client.Minecraft.getMinecraft()
        val worldPlayers = mc.theWorld?.playerEntities ?: listOf()
        
        val allPlayers = mutableSetOf<EntityPlayer>()
        allPlayers.addAll(worldPlayers)
        
        val toRemove = mutableSetOf<EntityPlayer>()
        swingProgress.keys.forEach { if (!allPlayers.contains(it)) toRemove.add(it) }
        
        toRemove.forEach { onPlayerRemove(it) }
    }

    override fun onPlayerRemove(player: EntityPlayer?) {
        if (player != null) {
            swingProgress.remove(player)
        } else {
            swingProgress.clear()
        }
        
        super.onPlayerRemove(player)
    }
} 