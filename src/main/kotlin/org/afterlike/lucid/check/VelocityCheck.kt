package org.afterlike.lucid.check

import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.Packet
import net.minecraft.network.play.server.S12PacketEntityVelocity
import net.minecraft.util.BlockPos
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

class VelocityCheck : Check() {
    override val name = "Velocity"
    override val description = "Detects players who manipulate or reduce knockback"

    private val lastPositions = mutableMapOf<EntityPlayer, MutableList<Triple<Double, Double, Double>>>()
    private val lastHurtTime = mutableMapOf<EntityPlayer, Int>()
    private val lastStartFall = mutableMapOf<EntityPlayer, Int>()
    private val lastStopFall = mutableMapOf<EntityPlayer, Int>()
    private val lastOnGround = mutableMapOf<EntityPlayer, Boolean>()
    private val consecutiveDetections = mutableMapOf<EntityPlayer, Int>()

    private val velocityPacketsReceived = mutableMapOf<Int, Long>()
    private val velocityPacketStrength = mutableMapOf<Int, Double>()

    init {
        CheckManager.register(this)
        vlThreshold = 8
    }

    override fun onUpdate(target: EntityPlayer) {
        val mc = Minecraft.getMinecraft()
        val tick = mc.theWorld.totalWorldTime.toInt()

        if (target === mc.thePlayer) return

        val positions = lastPositions.getOrDefault(target, mutableListOf())
        positions.add(0, Triple(target.posX, target.posY, target.posZ))
        if (positions.size > 20) positions.removeAt(positions.size - 1)
        lastPositions[target] = positions

        val prevHurtTime = lastHurtTime.getOrDefault(target, 0)
        val currHurtTime = target.hurtTime
        lastHurtTime[target] = currHurtTime

        val prevOnGround = lastOnGround.getOrDefault(target, true)
        val currOnGround = target.onGround

        if (target.motionY < -0.1 && prevOnGround && !currOnGround) {
            lastStartFall[target] = tick
        }

        if (!prevOnGround && currOnGround) {
            lastStopFall[target] = tick
        }

        lastOnGround[target] = currOnGround

        val velocityTime = velocityPacketsReceived[target.entityId] ?: 0L
        val currentTime = System.currentTimeMillis()
        val recentVelocity = currentTime - velocityTime < 500


        if ((currHurtTime > 0 && currHurtTime < target.maxHurtTime) || recentVelocity) {

            val startFallTick = lastStartFall.getOrDefault(target, 0)
            val stopFallTick = lastStopFall.getOrDefault(target, 0)
            val fallTicks = max(0, stopFallTick - startFallTick)
            val ticksSinceFall = tick - stopFallTick
            val recentFall = fallTicks >= 6 && ticksSinceFall <= 6

            val isBurning = target.isBurning

            if (!recentFall && !isBurning && positions.size >= 2) {
                val current = positions[0]
                val previous = positions[1]

                val deltaX = current.first - previous.first
                val deltaZ = current.third - previous.third
                val horizontalMovement = sqrt(deltaX * deltaX + deltaZ * deltaZ)

                val deltaY = current.second - previous.second

                val isCollided = checkSurroundingBlocks(target)

                val newVelocity = prevHurtTime <= 0 && currHurtTime > 0
                val velocityExpected = recentVelocity || newVelocity


                if (!isCollided && velocityExpected && horizontalMovement < 0.01 && abs(deltaY) < 0.01) {
                    val consec = consecutiveDetections.getOrDefault(target, 0) + 1
                    consecutiveDetections[target] = consec


                    if (consec >= 2) {
                        val msg = "reduced knockback movement (h:${String.format("%.3f", horizontalMovement)}, " +
                                "v:${String.format("%.3f", deltaY)}, hurt: $currHurtTime/${target.maxHurtTime})"
                        addVL(target, 2.0, msg)

                        if (consec >= 4) {
                            consecutiveDetections[target] = 0
                        }
                    }
                } else {

                    consecutiveDetections[target] = max(0, consecutiveDetections.getOrDefault(target, 0) - 1)
                }
            }
        } else if (currHurtTime == 0) {

            if (getPlayerVL(target) > 0) {
                decayVL(target, 0.2)
            }
        }
    }

    override fun onPacket(packet: Packet<*>) {

        if (packet is S12PacketEntityVelocity) {
            val entityId = packet.entityID
            val motionX = packet.motionX / 8000.0
            val motionY = packet.motionY / 8000.0
            val motionZ = packet.motionZ / 8000.0

            val magnitude = sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ)


            velocityPacketsReceived[entityId] = System.currentTimeMillis()
            velocityPacketStrength[entityId] = magnitude
        }
    }

    private fun checkSurroundingBlocks(player: EntityPlayer): Boolean {
        val mc = Minecraft.getMinecraft()
        val offsets = arrayOf(
            Pair(0.5, 0.0), Pair(-0.5, 0.0),
            Pair(0.0, 0.5), Pair(0.0, -0.5)
        )

        val bottom = player.posY.toInt()
        val middle = bottom + 1

        for (offset in offsets) {
            val offsetX = offset.first
            val offsetZ = offset.second

            val blockLeg = mc.theWorld.getBlockState(
                BlockPos(
                    (player.posX + offsetX).toInt(),
                    bottom,
                    (player.posZ + offsetZ).toInt()
                )
            ).block.localizedName

            val blockTorso = mc.theWorld.getBlockState(
                BlockPos(
                    (player.posX + offsetX).toInt(),
                    middle,
                    (player.posZ + offsetZ).toInt()
                )
            ).block.localizedName

            if (blockLeg != "tile.air" || blockTorso != "tile.air") {
                return true
            }
        }

        return false
    }

    private fun isPlayerCollided(player: EntityPlayer): Boolean {
        val pos = player.position
        val offsets = arrayOf(
            BlockPos(0, 0, 1),
            BlockPos(0, 0, -1),
            BlockPos(1, 0, 0),
            BlockPos(-1, 0, 0)
        )

        val world = player.worldObj


        for (offset in offsets) {
            val legBlock = world.getBlockState(pos.add(offset))
            val headBlock = world.getBlockState(pos.add(offset).up())

            if (!legBlock.block.isPassable(world, pos.add(offset)) ||
                !headBlock.block.isPassable(world, pos.add(offset).up())
            ) {
                return true
            }
        }

        return false
    }
} 