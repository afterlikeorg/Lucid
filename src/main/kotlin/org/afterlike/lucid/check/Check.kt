package org.afterlike.lucid.check

import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.event.ClickEvent
import net.minecraft.event.HoverEvent
import net.minecraft.network.Packet
import net.minecraft.util.ChatComponentText
import net.minecraft.util.ChatStyle
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

    private var lastTickProcessed = 0L

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
                    "§3Lucid §8> §cVerbose: $displayName §7failed §f$name §7($reason) [VL: +$amount, Total: ${
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
                    val messageBase = ChatComponentText("§3Lucid §8> $displayName §7failed §b$name")
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

                    val mc = Minecraft.getMinecraft()
                    if (mc?.thePlayer != null) {
                        mc.thePlayer.addChatMessage(messageBase)

                        if (Config.playSoundOnFlag) {
                            mc.thePlayer.playSound("note.pling", 1.0f, 1.0f)
                        }
                    }
                } catch (e: Exception) {
                    sendMessage("§3Lucid §8> $displayName §7failed §d$name")
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
                    "§3Lucid §8> §cVerbose: $displayName §7failed §f$name §7- Suppressed (${remainingCooldown}s cooldown)"
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
            val mc = Minecraft.getMinecraft()
            if (mc?.thePlayer != null) {
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
}