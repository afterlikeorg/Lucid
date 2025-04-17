package org.afterlike.lucid.check

import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.Packet
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.PlayerEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

object CheckManager {

    private val checks = CopyOnWriteArrayList<Check>()

    private val playerDimensions = ConcurrentHashMap<EntityPlayer, Int>()

    private var lastProcessedTick = 0L

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
            for (check in checks) {
                try {
                    if (check.enabled) {
                        check.onPacket(packet)
                    }
                } catch (e: Exception) {
                    System.err.println("[Lucid] Error in check ${check.name} while processing packet: ${e.message}")
                }
            }
        } catch (e: Exception) {
            System.err.println("[Lucid] Error in packet handler: ${e.message}")
        }
    }

    @SubscribeEvent
    fun onClientTick(event: TickEvent.ClientTickEvent) {
        try {

            if (event.phase != TickEvent.Phase.END) return

            val mc = Minecraft.getMinecraft()
            val thePlayer = mc?.thePlayer ?: return
            val theWorld = mc.theWorld ?: return

            val currentTick = theWorld.totalWorldTime
            lastProcessedTick = currentTick

            trackPlayerDimensions(theWorld, thePlayer)

            for (entity in theWorld.playerEntities) {
                if (entity !== thePlayer && entity.isEntityAlive) {

                    for (check in checks) {
                        if (check.enabled) {
                            try {
                                check.onUpdate(entity)
                            } catch (e: Exception) {
                                System.err.println("[Lucid] Error in check ${check.name} for player ${entity.name}: ${e.message}")
                            }
                        }
                    }
                }
            }

            cleanupDisconnectedPlayers(theWorld)
        } catch (e: Exception) {
            System.err.println("[Lucid] Error in client tick handler: ${e.message}")
        }
    }

    private fun trackPlayerDimensions(theWorld: net.minecraft.world.World, thePlayer: EntityPlayer) {
        for (entity in theWorld.playerEntities) {
            if (entity !== thePlayer && entity.isEntityAlive) {
                try {
                    val currentDimension = entity.dimension
                    val previousDimension = playerDimensions.put(entity, currentDimension)


                    if (previousDimension != null && previousDimension != currentDimension) {
                        onPlayerDimensionChange(entity)
                    }
                } catch (e: Exception) {
                    System.err.println("[Lucid] Error tracking dimension for player ${entity.name}: ${e.message}")
                }
            }
        }
    }

    @SubscribeEvent
    fun onPlayerLogout(event: PlayerEvent.PlayerLoggedOutEvent) {
        try {
            val player = event.player
            if (player != null) {

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
            System.err.println("[Lucid] Error in player logout handler: ${e.message}")
        }
    }

    @SubscribeEvent
    fun onWorldUnload(event: WorldEvent.Unload) {
        try {

            if (!event.world.isRemote) return


            playerDimensions.clear()


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

    private fun cleanupDisconnectedPlayers(world: net.minecraft.world.World) {
        try {
            val playerEntities = world.playerEntities.toSet()

            val playersToRemove = playerDimensions.keys.filter { !playerEntities.contains(it) }


            for (player in playersToRemove) {
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
}