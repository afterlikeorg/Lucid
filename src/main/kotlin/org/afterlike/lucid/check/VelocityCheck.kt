package org.afterlike.lucid.check

import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.Packet
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.util.BlockPos
import kotlin.math.abs
import kotlin.math.sqrt

class VelocityCheck : Check() {
    override val name = "Velocity"
    override val description = "Detects players who manipulate or reduce horizontal or vertical knockback"

    private data class HitEntry(
        val startTick: Int,
        val startPos: Triple<Double, Double, Double>,
        val startMotionH: Double
    )

    private val pendingHits = mutableMapOf<Int, HitEntry>()
    private val lastHurtTime = mutableMapOf<EntityPlayer, Int>()
    private val lastStartFall = mutableMapOf<EntityPlayer, Int>()
    private val lastStopFall = mutableMapOf<EntityPlayer, Int>()
    private val lastOnGround = mutableMapOf<EntityPlayer, Boolean>()

    private val WINDOW_TICKS = 4

    private val MIN_KNOCKBACK_V = 0.42
    private val EXPECTED_KNOCKBACK_H_STILL = 0.62
    private val EXPECTED_KNOCKBACK_H_MOVING = 0.2
    private val MOVEMENT_THRESHOLD = 0.015

    private val BASE_VL = 2.0

    init {
        CheckManager.register(this)
        vlThreshold = 8
    }

    override fun onUpdate(target: EntityPlayer) {
        val mc = Minecraft.getMinecraft()
        val world = mc.theWorld ?: return
        val tick = world.totalWorldTime.toInt()

        val prevHurt = lastHurtTime.getOrDefault(target, 0)
        val currHurt = target.hurtTime
        lastHurtTime[target] = currHurt

        if (prevHurt <= 0 && currHurt > 0) {
            val startMotionH = sqrt(target.motionX * target.motionX + target.motionZ * target.motionZ)
            pendingHits[target.entityId] = HitEntry(
                tick,
                Triple(target.posX, target.posY, target.posZ),
                startMotionH
            )
        }

        pendingHits.keys.removeIf { id ->
            tick - (pendingHits[id]?.startTick ?: 0) > WINDOW_TICKS * 2
        }

        val prevOn = lastOnGround.getOrDefault(target, true)
        val currOn = target.onGround
        if (prevOn && target.motionY < -0.1 && !currOn)    lastStartFall[target] = tick
        if (!prevOn && currOn)                             lastStopFall[target] = tick
        lastOnGround[target] = currOn

        pendingHits[target.entityId]?.let { entry ->
            if (tick - entry.startTick >= WINDOW_TICKS) {
                if (!shouldIgnore(target) && !checkSurroundingBlocks(target)) {
                    val dx = target.posX - entry.startPos.first
                    val dy = target.posY - entry.startPos.second
                    val dz = target.posZ - entry.startPos.third
                    val horizontalDist = sqrt(dx*dx + dz*dz)
                    val verticalDist = abs(dy)

                    val expectedH = if (entry.startMotionH <= MOVEMENT_THRESHOLD)
                        EXPECTED_KNOCKBACK_H_STILL
                    else
                        EXPECTED_KNOCKBACK_H_MOVING

                    if (horizontalDist < expectedH) {
                        val ratio = horizontalDist / expectedH
                        val vlToAdd = (1.0 - ratio) * BASE_VL
                        addVL(
                            target,
                            vlToAdd,
                            "reduced horizontal knockback: moved=${"%.3f".format(horizontalDist)} " +
                                    "(expected>=${"%.3f".format(expectedH)}, ${"%.0f".format(ratio*100)}%)"
                        )
                    }

                    if (verticalDist < MIN_KNOCKBACK_V) {
                        val ratioV = verticalDist / MIN_KNOCKBACK_V
                        val vlToAddV = (1.0 - ratioV) * BASE_VL
                        addVL(
                            target,
                            vlToAddV,
                            "reduced vertical knockback: moved=${"%.3f".format(verticalDist)} " +
                                    "(expected>=${"%.3f".format(MIN_KNOCKBACK_V)}, ${"%.0f".format(ratioV*100)}%)"
                        )
                    }
                }
                pendingHits.remove(target.entityId)
            }
        }
    }

    override fun onPacket(packet: Packet<*>) {
        if (packet is C02PacketUseEntity && packet.action == C02PacketUseEntity.Action.ATTACK) {
            pendingHits.remove(packet.entityId)
        }
    }

    private fun shouldIgnore(player: EntityPlayer): Boolean {
        val world = Minecraft.getMinecraft().theWorld ?: return true
        val tick = world.totalWorldTime.toInt()
        val start = lastStartFall.getOrDefault(player, 0)
        val stop = lastStopFall.getOrDefault(player, 0)
        val fallTicks = maxOf(0, stop - start)
        val sinceFall = tick - stop
        return player.isBurning || (fallTicks >= 6 && sinceFall <= 6)
    }

    private fun checkSurroundingBlocks(player: EntityPlayer): Boolean {
        val world = player.worldObj
        val pos = player.position
        val offsets = listOf(
            BlockPos(0,0,1), BlockPos(0,0,-1),
            BlockPos(1,0,0), BlockPos(-1,0,0)
        )
        for (off in offsets) {
            val side = pos.add(off)
            if (!world.getBlockState(side).block.isAir(world, side)
                || !world.getBlockState(side.up()).block.isAir(world, side.up())
            ) return true
        }
        return false
    }
}
