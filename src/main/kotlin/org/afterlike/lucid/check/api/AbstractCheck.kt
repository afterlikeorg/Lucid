package org.afterlike.lucid.check.api

import best.azura.eventbus.handler.EventHandler
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.event.ClickEvent
import net.minecraft.event.HoverEvent
import net.minecraft.util.ChatComponentText
import net.minecraft.util.ChatStyle
import net.minecraft.util.MathHelper
import org.afterlike.lucid.core.event.network.ReceivePacketEvent
import org.afterlike.lucid.core.event.world.EntityJoinEvent
import org.afterlike.lucid.core.event.world.EntityLeaveEvent
import org.afterlike.lucid.core.handler.ConfigHandler
import org.afterlike.lucid.util.ChatUtil
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

abstract class AbstractCheck {

    // check information
    abstract val name: String
    abstract val description: String

    open var violationLevelThreshold: Int = 10 // how much VL to send a flag. configurable
    open var enabled: Boolean = true // enabled by default

    protected val mc: Minecraft = Minecraft.getMinecraft()

    // player tracking
    private val playerLastFlagMap = ConcurrentHashMap<UUID, Long>()
    private val playerViolationLevelMap = ConcurrentHashMap<UUID, Double>()

    // decay config, override to customize
    open val decayConfig: DecayConfig = DecayConfig()

    data class DecayConfig(
        val baseRate: Double = 0.5,
        val mediumRate: Double = 0.8, // when VL > 25% of threshold
        val highRate: Double = 1.2, // when VL > 50% of threshold
        val criticalRate: Double = 1.5, // when VL > 80% of threshold
        val resetThreshold: Double = 0.2 // reset consecutive at this % of threshold
    )

    // event handlers
    @EventHandler
    open fun onReceivePacket(event: ReceivePacketEvent) {
    }

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

        playerLastFlagMap.remove(player.uniqueID)
        playerViolationLevelMap.remove(player.uniqueID)
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

        if (ConfigHandler.verboseMode) {
            val displayName = player.displayName?.formattedText ?: player.name
            val verboseMsg =
                "${ConfigHandler.getFormattedPrefix()}§cVerbose: $displayName §7failed §f$name §7($reason) [VL: +$amount, Total: ${
                    "%.1f".format(
                        newVL
                    )
                }]"
            ChatUtil.sendMessage(verboseMsg)
        }

        if (newVL >= violationLevelThreshold) {
            flag(player, reason)
            setPlayerVL(player, 0.0)
        }
    }

    // decay based on % to threshold
    protected fun decayVL(player: EntityPlayer, config: DecayConfig = decayConfig): Boolean {
        val currentVL = getPlayerVL(player)
        if (currentVL <= 0) return true

        val vlPercent = currentVL / violationLevelThreshold

        val rate = when {
            vlPercent > 0.8 -> config.criticalRate
            vlPercent > 0.5 -> config.highRate
            vlPercent > 0.25 -> config.mediumRate
            else -> config.baseRate
        }

        val newVL = max(0.0, currentVL - rate)
        setPlayerVL(player, newVL)

        return newVL <= violationLevelThreshold * config.resetThreshold
    }


    // call this in onCheckRun when no violation occurred
    protected fun handleNoViolation(player: EntityPlayer): Boolean {
        val currentVL = getPlayerVL(player)
        if (currentVL <= 0) return false
        return decayVL(player)
    }

    private fun flag(player: EntityPlayer, reason: String) {
        val uuid = player.uniqueID
        val displayName = player.displayName.formattedText ?: player.name
        val regularName = player.name

        val currentTime = System.currentTimeMillis()
        val lastFlagTime = playerLastFlagMap[uuid] ?: 0L
        val cooldownMillis = ConfigHandler.flagCooldown * 1000L

        val vlText = if (ConfigHandler.showVLInFlag) " §7[VL:${violationLevelThreshold}]" else ""
        val messageBase =
            ChatComponentText("${ConfigHandler.getFormattedPrefix()}$displayName §7failed §${ConfigHandler.messageColor}$name$vlText")

        if (currentTime - lastFlagTime >= cooldownMillis) {
            if (ConfigHandler.showWDR) {
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

            if (ConfigHandler.playSoundOnFlag) {
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
