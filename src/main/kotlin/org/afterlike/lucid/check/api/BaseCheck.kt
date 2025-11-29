package org.afterlike.lucid.check.api

import best.azura.eventbus.handler.EventHandler
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.event.ClickEvent
import net.minecraft.event.HoverEvent
import net.minecraft.util.ChatComponentText
import net.minecraft.util.ChatStyle
import net.minecraft.util.MathHelper
import org.afterlike.lucid.core.handler.PlayerSampleHandler
import org.afterlike.lucid.core.handler.ConfigHandler
import org.afterlike.lucid.core.event.network.ReceivePacketEvent
import org.afterlike.lucid.core.event.world.EntityJoinEvent
import org.afterlike.lucid.core.event.world.EntityLeaveEvent
import org.afterlike.lucid.util.ChatUtil
import org.apache.logging.log4j.LogManager
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

abstract class BaseCheck {

    private val logger = LogManager.getLogger(BaseCheck::class.java)

    // check information
    abstract val name: String
    abstract val description: String

    open var violationLevelThreshold: Int = 10 // how much VL to send a flag. configurable
    open var enabled: Boolean = true // enabled by default

    protected val mc: Minecraft = Minecraft.getMinecraft()

    // player tracking
    private val playerLastFlagMap = ConcurrentHashMap<UUID, Long>()
    private val playerViolationLevelMap = ConcurrentHashMap<UUID, Double>()

    // event handlers
    @EventHandler
    open fun onReceivePacket(event: ReceivePacketEvent) {}
    
    open fun onPlayerJoin(event: EntityJoinEvent) {}
    
    open fun onPlayerLeave(event: EntityLeaveEvent) {}
    
    abstract fun onCheckRun(target: EntityPlayer)

    @EventHandler
    fun onEntityJoin(event: EntityJoinEvent) {
        if (event.entity !is EntityPlayer) return

        onPlayerJoin(event)
    }

    @EventHandler
    fun onEntityLeave(event: EntityLeaveEvent) {
        if (event.entity !is EntityPlayer) return
        
        val player = event.entity
        
        onPlayerLeave(event)

        try {
            playerLastFlagMap.remove(player.uniqueID)
            playerViolationLevelMap.remove(player.uniqueID)
            PlayerSampleHandler.removePlayer(player)
        } catch (e: Exception) {
            logger.error("Error cleaning up player data in {}: {}", player.name, e.message)
        }
    }

    fun getPlayerVL(player: EntityPlayer): Double {
        return playerViolationLevelMap.getOrDefault(player.uniqueID, 0.0)
    }

    private fun setPlayerVL(player: EntityPlayer, vl: Double) {
        playerViolationLevelMap[player.uniqueID] = vl
    }

    protected fun addVL(player: EntityPlayer, amount: Double, reason: String) {
        val currentVL = getPlayerVL(player)
        val newVL = currentVL + amount

        setPlayerVL(player, newVL)

        if (ConfigHandler.verboseMode.value) {
            val displayName = player.displayName?.formattedText ?: player.name

            val verboseMsg =
                "${ConfigHandler.getFormattedPrefix()}§cVerbose: $displayName §7failed §f$name §7($reason) [VL: +$amount, Total: ${
                    "%.1f".format(newVL)
                }]"
            ChatUtil.sendMessage(verboseMsg)
        }

        if (newVL >= violationLevelThreshold) {
            flag(player, reason)
            setPlayerVL(player, 0.0)
        }
    }

    protected fun decayVL(player: EntityPlayer, amount: Double) {
        val currentVL = getPlayerVL(player)
        if (currentVL > 0) {
            setPlayerVL(player, max(0.0, currentVL - amount))
        }
    }

    private fun flag(player: EntityPlayer, reason: String) {
        val uuid = player.uniqueID
        val displayName = player.displayName.formattedText ?: player.name
        val regularName = player.name

        val currentTime = System.currentTimeMillis()
        val lastFlagTime = playerLastFlagMap[uuid] ?: 0L
        val cooldownMillis = ConfigHandler.flagCooldown.value * 1000L

        val vlText = if (ConfigHandler.showVLInFlag.value) " §7[VL:${violationLevelThreshold}]" else ""
        val messageBase =
            ChatComponentText("${ConfigHandler.getFormattedPrefix()}$displayName §7failed §${ConfigHandler.messageColor.value}$name$vlText")

        if (currentTime - lastFlagTime >= cooldownMillis) {
            if (ConfigHandler.showWDR.value) {
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

            ChatUtil.sendMessage(messageBase)

            if (ConfigHandler.playSoundOnFlag.value) {
                mc.thePlayer.playSound("note.pling", 1.0f, 1.0f)
            }
        }

        playerLastFlagMap[uuid] = currentTime

    }

    protected fun getMoveAngle(deltaX: Double, deltaZ: Double): Float {
        if (abs(deltaX) < 1e-8 && abs(deltaZ) < 1e-8) {
            return 0f
        }

        return (MathHelper.atan2(deltaZ, deltaX).toFloat() * 180.0f / Math.PI.toFloat()) - 90.0f
    }

    protected fun getRelativeMoveAngle(deltaX: Double, deltaZ: Double, yaw: Float): Float {
        val moveAngle = getMoveAngle(deltaX, deltaZ)
        var relativeAngle = moveAngle - yaw

        // normalize to -180 to 180
        relativeAngle = ((relativeAngle % 360f) + 360f) % 360f
        if (relativeAngle > 180f) {
            relativeAngle -= 360f
        }

        return relativeAngle
    }

    protected fun calculateSpeed(deltaX: Double, deltaZ: Double): Double {
        return sqrt(deltaX * deltaX + deltaZ * deltaZ)
    }
}