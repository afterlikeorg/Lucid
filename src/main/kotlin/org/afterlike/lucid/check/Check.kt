package org.afterlike.lucid.check

import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.event.ClickEvent
import net.minecraft.event.HoverEvent
import net.minecraft.network.Packet
import net.minecraft.util.ChatComponentText
import net.minecraft.util.ChatStyle
import net.minecraft.util.MathHelper
import org.afterlike.lucid.util.Config
import java.util.*
import java.util.concurrent.ConcurrentHashMap

abstract class Check {
    abstract val name: String
    abstract val description: String
    var enabled: Boolean = true

    private val playerVLs = ConcurrentHashMap<UUID, Double>()

    var vlThreshold: Int = 10

    var lastDebugInfo: String = ""

    private val lastFlagTimes = ConcurrentHashMap<UUID, Long>()

    private val MAX_PLAYERS_TRACKED = 100

    protected val mc: Minecraft = Minecraft.getMinecraft()

    open fun onPacket(packet: Packet<*>) {
        try {

        } catch (e: Exception) {
            logError("Error processing packet in ${name}: ${e.message}")
        }
    }

    open fun onUpdate(target: EntityPlayer) {
        try {

        } catch (e: Exception) {
            logError("Error updating ${name} for ${target.name}: ${e.message}")
        }
    }

    open fun onPlayerRemove(player: EntityPlayer?) {
        try {
            if (player != null) {
                lastFlagTimes.remove(player.uniqueID)
                playerVLs.remove(player.uniqueID)
            } else {
                playerVLs.clear()
                lastFlagTimes.clear()
            }
        } catch (e: Exception) {
            logError("Error cleaning up player data in ${name}: ${e.message}")
        }
    }

    protected fun getPlayerVL(player: EntityPlayer): Double {
        return playerVLs.getOrDefault(player.uniqueID, 0.0)
    }

    private fun setPlayerVL(player: EntityPlayer, vl: Double) {
        if (playerVLs.size > MAX_PLAYERS_TRACKED && !playerVLs.containsKey(player.uniqueID)) {
            cleanupOldEntries()
        }
        playerVLs[player.uniqueID] = vl
    }

    protected fun addVL(player: EntityPlayer, amount: Double, reason: String) {
        if (!enabled) return

        try {
            val currentVL = getPlayerVL(player)
            val newVL = currentVL + amount

            setPlayerVL(player, newVL)

            lastDebugInfo = reason

            if (Config.verboseMode) {
                val displayName = player.displayName?.formattedText ?: player.name

                val verboseMsg =
                    "${Config.getFormattedPrefix()}§cVerbose: $displayName §7failed §f$name §7($reason) [VL: +$amount, Total: ${
                        "%.1f".format(newVL)
                    }]"
                sendMessage(verboseMsg)
            }

            if (newVL >= vlThreshold) {
                flag(player, reason)
                setPlayerVL(player, 0.0)
            }
        } catch (e: Exception) {
            logError("Error adding violation level in ${name}: ${e.message}")
        }
    }

    protected fun decayVL(player: EntityPlayer, amount: Double) {
        try {
            val currentVL = getPlayerVL(player)
            if (currentVL > 0) {
                setPlayerVL(player, Math.max(0.0, currentVL - amount))
            }
        } catch (e: Exception) {
            logError("Error decaying VL: ${e.message}")
        }
    }

    private fun flag(player: EntityPlayer, reason: String) {
        try {
            val uuid = player.uniqueID
            val currentTime = System.currentTimeMillis()
            val lastFlagTime = lastFlagTimes[uuid] ?: 0L
            val cooldownMillis = Config.flagCooldown * 1000L

            if (currentTime - lastFlagTime >= cooldownMillis) {
                val displayName = player.displayName?.formattedText ?: player.name
                val regularName = player.name

                try {
                    val vlText = if (Config.showVLInFlag) " §7[VL: ${vlThreshold}]" else ""
                    val messageBase = ChatComponentText("${Config.getFormattedPrefix()}$displayName §7failed §${Config.messageColor}$name$vlText")
                    
                    if (Config.showWDR) {
                        val wdrButton = ChatComponentText(" §c[WDR]")
                        val wdrStyle = ChatStyle()
                            .setChatClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, "/wdr $regularName cheating"))
                            .setChatHoverEvent(
                                HoverEvent(
                                    HoverEvent.Action.SHOW_TEXT,
                                    ChatComponentText("§bClick to report $displayName §bfor cheating")
                                )
                            )
                        wdrButton.chatStyle = wdrStyle
                        messageBase.appendSibling(wdrButton)
                    }

                    if (mc.thePlayer != null) {
                        mc.thePlayer.addChatMessage(messageBase)

                        if (Config.playSoundOnFlag) {
                            mc.thePlayer.playSound("note.pling", 1.0f, 1.0f)
                        }
                    }
                } catch (e: Exception) {
                    sendMessage("${Config.getFormattedPrefix()}$displayName §7failed §${Config.messageColor}$name")
                    logError("Error sending chat components: ${e.message}")
                }

                lastFlagTimes[uuid] = currentTime

                if (lastFlagTimes.size > MAX_PLAYERS_TRACKED) {
                    cleanupOldEntries()
                }
            } else if (Config.verboseMode) {
                val remainingCooldown = (cooldownMillis - (currentTime - lastFlagTime)) / 1000
                val displayName = player.displayName?.formattedText ?: player.name
                val cooldownMsg =
                    "${Config.getFormattedPrefix()}§cVerbose: $displayName §7failed §f$name §7- Suppressed (${remainingCooldown}s cooldown)"
                sendMessage(cooldownMsg)
            }
        } catch (e: Exception) {
            logError("Error flagging player in ${name}: ${e.message}")
        }
    }

    private fun cleanupOldEntries() {
        try {
            val currentTime = System.currentTimeMillis()
            val entriesToRemove = lastFlagTimes.entries
                .sortedBy { it.value }
                .take(lastFlagTimes.size - MAX_PLAYERS_TRACKED / 2)
                .map { it.key }

            entriesToRemove.forEach {
                lastFlagTimes.remove(it)
                playerVLs.remove(it)
            }
        } catch (e: Exception) {
            logError("Error cleaning up old entries: ${e.message}")
        }
    }

    private fun sendMessage(message: String) {
        try {
            if (mc.thePlayer != null) {
                mc.thePlayer.addChatMessage(ChatComponentText(message))
            }
        } catch (e: Exception) {
            logError("Error sending chat message: ${e.message}")
        }
    }

    protected fun logError(message: String) {
        try {
            System.err.println("[Lucid] $message")
        } catch (e: Exception) {
        }
    }
    
    protected fun getPlayerSample(player: EntityPlayer): PlayerSample? {
        return PlayerDataManager.getCurrentSample(player)
    }
    
    protected fun getPreviousSample(player: EntityPlayer): PlayerSample? {
        return PlayerDataManager.getPreviousSample(player)
    }
    
    protected fun getPlayerHistory(player: EntityPlayer): List<PlayerSample> {
        return PlayerDataManager.getHistory(player)
    }
    
    protected fun getSampleTicksAgo(player: EntityPlayer, ticks: Int): PlayerSample? {
        return PlayerDataManager.getTicksAgo(player, ticks)
    }
    
    protected fun getMoveAngle(deltaX: Double, deltaZ: Double): Float {
        if (Math.abs(deltaX) < 1e-8 && Math.abs(deltaZ) < 1e-8) {
            return 0f
        }
        
        return (MathHelper.atan2(deltaZ, deltaX).toFloat() * 180.0f / Math.PI.toFloat()) - 90.0f
    }
    
    protected fun getRelativeMoveAngle(deltaX: Double, deltaZ: Double, yaw: Float): Float {
        val moveAngle = getMoveAngle(deltaX, deltaZ)
        var relativeAngle = moveAngle - yaw
        
        // Normalize to -180 to 180
        relativeAngle = ((relativeAngle % 360f) + 360f) % 360f
        if (relativeAngle > 180f) {
            relativeAngle -= 360f
        }
        
        return relativeAngle
    }
    
    protected fun calculateSpeed(deltaX: Double, deltaZ: Double): Double {
        return Math.sqrt(deltaX * deltaX + deltaZ * deltaZ)
    }
    
    protected fun checkSurroundingBlocks(player: EntityPlayer, height: Int = 1): Boolean {
        val world = player.worldObj
        val pos = player.position
        val offsets = listOf(
            net.minecraft.util.BlockPos(0, 0, 1), 
            net.minecraft.util.BlockPos(0, 0, -1),
            net.minecraft.util.BlockPos(1, 0, 0), 
            net.minecraft.util.BlockPos(-1, 0, 0)
        )
        
        for (off in offsets) {
            val side = pos.add(off)
            var isBlocked = false
            
            for (y in 0 until height) {
                val blockPos = side.add(0, y, 0)
                if (!world.getBlockState(blockPos).block.isAir(world, blockPos)) {
                    isBlocked = true
                    break
                }
            }
            
            if (isBlocked) return true
        }
        
        return false
    }
}