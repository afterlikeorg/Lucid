package org.afterlike.lucid.check.example

import net.minecraft.entity.player.EntityPlayer
import org.afterlike.lucid.check.Check
import org.afterlike.lucid.check.CheckManager
import java.util.concurrent.ConcurrentHashMap

class ScaffoldCheck : Check() {
    override val name = "Scaffold"
    override val description = "Detects unusual block placement patterns"

    private val lastPlacementTime = ConcurrentHashMap<EntityPlayer, Long>()
    private val placementFrequency = ConcurrentHashMap<EntityPlayer, Int>()
    private val consistentPlacementAngles = ConcurrentHashMap<EntityPlayer, Int>()
    private val bridgingStreak = ConcurrentHashMap<EntityPlayer, Int>()
    private val sneaking = ConcurrentHashMap<EntityPlayer, Boolean>()
    private val wasSwinging = ConcurrentHashMap<EntityPlayer, Boolean>()
    private var lastCleanupTime = System.currentTimeMillis()
    private val CLEANUP_INTERVAL = 30000L

    init {
        CheckManager.register(this)
        vlThreshold = 8
    }

    override fun onPlayerRemove(player: EntityPlayer?) {

            if (player != null) {
            lastPlacementTime.remove(player)
            placementFrequency.remove(player)
            consistentPlacementAngles.remove(player)
            bridgingStreak.remove(player)
            sneaking.remove(player)
            wasSwinging.remove(player)
            } else {
            lastPlacementTime.clear()
            placementFrequency.clear()
            consistentPlacementAngles.clear()
            bridgingStreak.clear()
            sneaking.clear()
            wasSwinging.clear()
        }

        super.onPlayerRemove(player)
    }

    override fun onUpdate(target: EntityPlayer) {
        try {
            val mc = net.minecraft.client.Minecraft.getMinecraft()
            val currentTime = System.currentTimeMillis()
            
            if (currentTime - lastCleanupTime > CLEANUP_INTERVAL) {
                cleanupOldData()
                lastCleanupTime = currentTime
            }

            if (target == mc.thePlayer) return

            val hasBlock = target.heldItem != null && target.heldItem.item.javaClass.simpleName.contains("ItemBlock")

            val isSneaking = target.isSneaking
            val isSwinging = target.isSwingInProgress
            val wasPlayerSneaking = sneaking.getOrDefault(target, false)
            val wasPlayerSwinging = wasSwinging.getOrDefault(target, false)


            sneaking[target] = isSneaking
            wasSwinging[target] = isSwinging


            if (wasPlayerSwinging && !isSwinging && hasBlock) {
                val currentTime = System.currentTimeMillis()
                val lastTime = lastPlacementTime.getOrDefault(target, 0L)
                val timeDelta = currentTime - lastTime


                lastPlacementTime[target] = currentTime


                if (timeDelta < 300) {
                    placementFrequency[target] = placementFrequency.getOrDefault(target, 0) + 1


                    if (placementFrequency.getOrDefault(target, 0) > 3) {
                        addVL(
                            target,
                            2.5,
                            "placing blocks too rapidly (${placementFrequency[target]} in quick succession)"
                        )
                    }
                } else {
                    placementFrequency[target] = Math.max(0, placementFrequency.getOrDefault(target, 0) - 1)
                }


                if (timeDelta > 3000) {
                    placementFrequency[target] = 0
                }

                val isLookingDown = target.rotationPitch > 40

                if (isLookingDown && isSneaking) {
                    bridgingStreak[target] = bridgingStreak.getOrDefault(target, 0) + 1

                    if (bridgingStreak.getOrDefault(target, 0) > 6) {
                        addVL(target, 2.0, "continuous bridging")


                        if (Math.abs(target.rotationPitch - target.prevRotationPitch) < 1 &&
                            Math.abs(target.rotationYaw - target.prevRotationYaw) < 1
                        ) {

                            consistentPlacementAngles[target] = consistentPlacementAngles.getOrDefault(target, 0) + 1

                            if (consistentPlacementAngles.getOrDefault(target, 0) > 4) {
                                addVL(target, 3.5, "suspiciously consistent block placements")
                            }
                        } else {
                            consistentPlacementAngles[target] =
                                Math.max(0, consistentPlacementAngles.getOrDefault(target, 0) - 1)
                        }
                    }
                } else {
                    bridgingStreak[target] = Math.max(0, bridgingStreak.getOrDefault(target, 0) - 1)
                }
            }


            decayVL(target, 0.1)

        } catch (e: Exception) {
            super.onUpdate(target)
        }
    }
    
    private fun cleanupOldData() {
        try {
            val mc = net.minecraft.client.Minecraft.getMinecraft()
            val worldPlayers = mc.theWorld?.playerEntities ?: listOf()
            
            val allPlayers = mutableSetOf<EntityPlayer>()
            allPlayers.addAll(worldPlayers)
            
            val toRemove = mutableSetOf<EntityPlayer>()
            
            lastPlacementTime.keys.forEach { player -> 
                if (!allPlayers.contains(player)) toRemove.add(player) 
            }
            placementFrequency.keys.forEach { player -> 
                if (!allPlayers.contains(player)) toRemove.add(player) 
            }
            consistentPlacementAngles.keys.forEach { player -> 
                if (!allPlayers.contains(player)) toRemove.add(player) 
            }
            bridgingStreak.keys.forEach { player -> 
                if (!allPlayers.contains(player)) toRemove.add(player) 
            }
            sneaking.keys.forEach { player -> 
                if (!allPlayers.contains(player)) toRemove.add(player) 
            }
            wasSwinging.keys.forEach { player -> 
                if (!allPlayers.contains(player)) toRemove.add(player) 
            }
            
            toRemove.forEach { player ->
                onPlayerRemove(player)
            }

        } catch (e: Exception) {
            logError("Error cleaning up scaffold data: ${e.message}")
        }
    }
}