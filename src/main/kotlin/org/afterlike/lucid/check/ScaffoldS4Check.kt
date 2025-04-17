package org.afterlike.lucid.check

import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemBlock
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.sqrt

class ScaffoldS4Check : Check() {
    override val name = "Scaffold S4"
    override val description = "Improved scaffold/tower check"

    private data class YSample(val posY: Double)
    private val ySamples = ConcurrentHashMap<EntityPlayer, MutableList<YSample>>()

    init {
        CheckManager.register(this)
        vlThreshold = 12
    }

    override fun onUpdate(target: EntityPlayer) {
        val mc = Minecraft.getMinecraft()
        if (target === mc.thePlayer || target.isRiding) return

        val samples = ySamples.getOrPut(target) { mutableListOf() }
        samples.add(0, YSample(target.posY))
        if (samples.size > 4) samples.removeAt(samples.lastIndex)
        if (samples.size < 4) return

        val pitch     = target.rotationPitch.toDouble()
        val dx        = (target.posX  - target.prevPosX) * 20.0
        val dz        = (target.posZ  - target.prevPosZ) * 20.0
        val speedXZsq = dx*dx + dz*dz
        val speedXZ   = sqrt(speedXZsq)
        val speedY    = (samples[0].posY - samples[1].posY) * 20.0
        val avgAccelY = 50.0 * (samples[3].posY - samples[2].posY - samples[1].posY + samples[0].posY)
        val angleDiff = abs(getMoveLookAngleDiff(target))

        var flagged = false

        if (target.isSwingInProgress && target.hurtTime == 0 &&
            pitch > 50.0 && speedXZsq > 9.0 &&
            target.heldItem?.item is ItemBlock &&
            angleDiff > 165.0 && speedXZsq < 100.0 &&
            !isAlmostZero(avgAccelY)
        ) {
            // tower
            if (speedY in 4.0..15.0 && avgAccelY > -25.0) {
                val reason = buildReason(pitch, speedXZ, angleDiff, speedY, avgAccelY)
                addVL(target, 4.0, reason)
                flagged = true
            }
            // horizontal scaffold
            else if (speedY in -1.0..4.0 &&
                abs(speedY) > 0.005 &&
                speedXZsq > 25.0
            ) {
                val reason = buildReason(pitch, speedXZ, angleDiff, speedY, avgAccelY)
                addVL(target, 4.0, reason)
                flagged = true
            }
        }

        if (!flagged) {
            decayVL(target, 0.5)
        }
    }

    private fun buildReason(
        pitch: Double,
        speedXZ: Double,
        angleDiff: Double,
        speedY: Double,
        avgAccelY: Double
    ): String {
        return " | pitch ${"%.2f".format(pitch)}" +
                " | speedXZ ${"%.2f".format(speedXZ)}" +
                " | angleDiff ${"%.2f".format(angleDiff)}" +
                " | speedY ${"%.2f".format(speedY)}" +
                " | avgAccelY ${"%.2f".format(avgAccelY)}"
    }

    private fun isAlmostZero(d: Double) = abs(d) < 0.001

    private fun getMoveLookAngleDiff(player: EntityPlayer): Double {
        val dx   = player.posX   - player.prevPosX
        val dz   = player.posZ   - player.prevPosZ
        val move = Math.toDegrees(Math.atan2(dz, dx)) - 90.0
        val look = player.rotationYaw.toDouble()
        var diff = ((move - look) % 360.0 + 360.0) % 360.0
        if (diff > 180.0) diff -= 360.0
        return diff
    }

    override fun onPlayerRemove(player: EntityPlayer?) {
        if (player != null) ySamples.remove(player)
        else                ySamples.clear()
        super.onPlayerRemove(player)
    }
}