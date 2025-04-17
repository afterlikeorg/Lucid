package org.afterlike.lucid.check

import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.Packet
import net.minecraft.network.play.server.S12PacketEntityVelocity
import net.minecraft.util.Vec3
import kotlin.math.sqrt

class ReachCheck : Check() {
    override val name = "Reach"
    override val description = "Detects abnormal hit distances between players"

    private val maxReachDistance = 3.1

    private val maxPossibleReachDistance = 3.8

    private val lastAttackTimes = mutableMapOf<EntityPlayer, Long>()
    private val attackCooldowns = mutableMapOf<EntityPlayer, Int>()
    private val targetHurtTimes = mutableMapOf<EntityPlayer, MutableList<EntityPlayer>>()
    private val recentlyDamaged = mutableMapOf<EntityPlayer, Long>()

    private val recentVelocity = mutableMapOf<Int, Long>()

    init {
        CheckManager.register(this)
        vlThreshold = 3
    }

    override fun onUpdate(target: EntityPlayer) {
        val mc = Minecraft.getMinecraft()
        val currentTime = System.currentTimeMillis()


        if (target === mc.thePlayer) return


        recentVelocity.entries.removeIf { currentTime - it.value > 500 }

        val isAttacking = target.isSwingInProgress &&
                !target.isBlocking &&
                target.swingProgress > 0 &&
                target.swingProgress < 0.5 &&
                attackCooldowns.getOrDefault(target, 0) <= 0

        if (isAttacking) {

            attackCooldowns[target] = 10

            val nearbyPlayers = mc.theWorld.playerEntities.filter {
                it !== target && it !== mc.thePlayer &&
                        it.getDistanceToEntity(target) <= maxPossibleReachDistance &&
                        !it.isDead &&
                        target.canEntityBeSeen(it)
            }

            val hurtPlayers = nearbyPlayers.filter {
                val wasRecentlyDamaged = it.hurtTime > 0 && it.hurtTime < 10
                if (wasRecentlyDamaged) {
                    recentlyDamaged[it] = currentTime
                }
                wasRecentlyDamaged
            }

            if (hurtPlayers.isNotEmpty()) {

                val nearestHurt = hurtPlayers.minByOrNull { target.getDistanceToEntity(it) }

                if (nearestHurt != null) {

                    val attackerTargets = targetHurtTimes.getOrDefault(target, mutableListOf())
                    attackerTargets.add(nearestHurt)
                    targetHurtTimes[target] = attackerTargets

                    val attackerEyes = target.getPositionEyes(1.0f)
                    val victimPos = nearestHurt.positionVector

                    val hitboxCompensation = 0.3
                    val distance = calculateDistance(attackerEyes, victimPos) - hitboxCompensation


                    if (distance > maxReachDistance && distance < maxPossibleReachDistance) {

                        if (!recentVelocity.containsKey(target.entityId)) {

                            val excess = distance - maxReachDistance
                            val vlAmount = excess * 1.5

                            addVL(target, vlAmount, "reach distance=${String.format("%.2f", distance)} blocks")


                            lastDebugInfo = "player=${target.name}, target=${nearestHurt.name}, " +
                                    "distance=${String.format("%.2f", distance)}, " +
                                    "hitTime=${nearestHurt.hurtTime}"
                        }
                    }
                }
            }
        }


        if (attackCooldowns.containsKey(target)) {
            val cooldown = attackCooldowns[target]!! - 1
            if (cooldown <= 0) {
                attackCooldowns.remove(target)
            } else {
                attackCooldowns[target] = cooldown
            }
        }


        recentlyDamaged.entries.removeIf { currentTime - it.value > 1000 }
        if (targetHurtTimes.containsKey(target) && targetHurtTimes[target]!!.size > 10) {
            targetHurtTimes[target] = targetHurtTimes[target]!!.takeLast(10).toMutableList()
        }
    }

    override fun onPacket(packet: Packet<*>) {

        if (packet is S12PacketEntityVelocity) {
            recentVelocity[packet.entityID] = System.currentTimeMillis()
        }
    }

    private fun calculateDistance(from: Vec3, to: Vec3): Double {
        val dx = to.xCoord - from.xCoord
        val dy = to.yCoord - from.yCoord
        val dz = to.zCoord - from.zCoord
        return sqrt(dx * dx + dy * dy + dz * dz)
    }
}