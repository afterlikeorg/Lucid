package org.afterlike.lucid.data.handler.impl

import best.azura.eventbus.handler.EventHandler
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.item.ItemSword
import net.minecraft.item.ItemTool
import net.minecraft.network.play.server.*
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import org.afterlike.lucid.core.event.network.ReceivePacketEvent
import org.afterlike.lucid.core.handler.DelayedTaskHandler
import org.afterlike.lucid.data.context.impl.InteractionContext.AttackType
import org.afterlike.lucid.data.handler.api.AbstractHandler
import org.afterlike.lucid.util.ChatUtil
import kotlin.math.abs


object InteractionHandler : AbstractHandler() {

    private var lastSwingTime = 0L
    private var lastSwingEntityId = 0
    private var lastPacketWasSwing = false

    private var lastHurtTime = 0L
    private var lastHurtEntityId = 0
    private var lastPacketWasHurt = false

    private var consecutiveSwingHurt = false

    // block mining tracking
    private data class MiningInfo(val entityId: Int, val pos: BlockPos, val progress: Int, val time: Long)

    private val activeMining = mutableMapOf<BlockPos, MiningInfo>()

    // block placing tracking (recent swings per player for correlation)
    private data class SwingInfo(val entityId: Int, val pos: BlockPos, val time: Long)

    private val recentSwings = mutableListOf<SwingInfo>()

    private var lastCleanup = 0L

    @EventHandler
    fun onReceivePacket(event: ReceivePacketEvent) {
        val packet = event.packet
        val currentTime = System.currentTimeMillis()

        // cleanup old data
        if (currentTime - lastCleanup > 1000) {
            activeMining.entries.removeIf { currentTime - it.value.time > 5000 }
            recentSwings.removeIf { currentTime - it.time > 500 }
            lastCleanup = currentTime
        }

        when (packet) {
            is S0BPacketAnimation -> {
                if (handleAnimation(packet, currentTime)) return // swing keeps flags
            }

            is S12PacketEntityVelocity -> handleVelocity(packet, currentTime)
            is S19PacketEntityStatus -> {
                if (handleEntityStatus(packet, currentTime)) return // hurt keeps flags
            }

            is S29PacketSoundEffect -> handleSound(packet, currentTime)
            is S25PacketBlockBreakAnim -> handleBlockBreakAnim(packet, currentTime)
            is S23PacketBlockChange -> handleBlockChange(packet, currentTime)
        }

        // reset flags after processing (unless we returned early)
        lastPacketWasHurt = false
        lastPacketWasSwing = false
        consecutiveSwingHurt = false
    }

    // attack detection

    private fun handleAnimation(packet: S0BPacketAnimation, currentTime: Long): Boolean {
        val animationType = packet.animationType

        if (animationType == 0) { // swing
            lastPacketWasHurt = false
            consecutiveSwingHurt = false
            lastPacketWasSwing = true
            lastSwingTime = currentTime
            lastSwingEntityId = packet.entityID

            scheduleTask {
                val entity = mc.theWorld?.getEntityByID(packet.entityID)
                if (entity is EntityPlayer && entity != mc.thePlayer) {
                    PlayerHandler.get(entity)?.interaction?.onSwingDetected()
                    // track for block placing correlation
                    recentSwings.add(
                        SwingInfo(
                            packet.entityID,
                            BlockPos(entity.posX, entity.posY, entity.posZ),
                            currentTime
                        )
                    )
//                    ChatUtil.sendDebug("Swing: §f${entity.name}")
                }
            }
            return true // keep flags for next packet
        }

        if (animationType == 4 || animationType == 5) { // critical (4) / enchant particle (5)
            if (currentTime - lastSwingTime < 2) {
                val type = when {
                    animationType == 4 && lastPacketWasSwing -> AttackType.DIRECT_CRITICAL
                    animationType == 4 -> AttackType.CRITICAL
                    lastPacketWasSwing -> AttackType.DIRECT_SHARPNESS
                    else -> AttackType.SHARPNESS
                }
                checkPlayerAttack(lastSwingEntityId, packet.entityID, type, null)
            }
        }

        return false // reset flags
    }

    private fun handleVelocity(packet: S12PacketEntityVelocity, currentTime: Long) {
        if (currentTime - lastSwingTime < 2) {
            if (packet.motionX != 0 || packet.motionY != 0 || packet.motionZ != 0) {
                val type = if (lastPacketWasSwing) AttackType.DIRECT_VELOCITY else AttackType.VELOCITY
                checkPlayerAttack(lastSwingEntityId, packet.entityID, type, null)
            }
        }
    }

    private fun handleEntityStatus(packet: S19PacketEntityStatus, currentTime: Long): Boolean {
        if (packet.opCode.toInt() == 2) { // hurt
            if (lastPacketWasSwing) consecutiveSwingHurt = true
            lastPacketWasSwing = false
            lastPacketWasHurt = true
            lastHurtTime = currentTime
            lastHurtEntityId = packet.getEntity(mc.theWorld)?.entityId ?: return false

            scheduleTask {
                val entity = mc.theWorld?.getEntityByID(lastHurtEntityId)
                if (entity is EntityPlayer) {
                    PlayerHandler.get(entity)?.interaction?.onHurtDetected()
//                    ChatUtil.sendDebug("Hurt: §f${entity.name}")
                }
            }
            return true // keep flags for next packet (sound)
        }
        return false // reset flags
    }

    private fun handleSound(packet: S29PacketSoundEffect, currentTime: Long) {
        val soundName = packet.soundName

        // check for hurt/death sounds following swing+hurt sequence
        if (lastPacketWasHurt && currentTime - lastSwingTime < 2 && currentTime - lastHurtTime < 2) {
            val soundPos = Vec3(packet.x, packet.y, packet.z)
            when (soundName) {
                "game.player.hurt" -> {
                    val type = if (consecutiveSwingHurt) AttackType.DIRECT_HURT_SOUND else AttackType.HURT_SOUND
                    checkPlayerAttack(lastSwingEntityId, lastHurtEntityId, type, soundPos)
                }

                "game.player.die" -> {
                    val type = if (consecutiveSwingHurt) AttackType.DIRECT_DEATH_SOUND else AttackType.DEATH_SOUND
                    checkPlayerAttack(lastSwingEntityId, lastHurtEntityId, type, soundPos)
                }
            }
        }
    }

    private fun checkPlayerAttack(attackerEntityId: Int, targetEntityId: Int, attackType: AttackType, soundPos: Vec3?) {
        scheduleTask {
            val attacker = mc.theWorld?.getEntityByID(attackerEntityId)
            val target = mc.theWorld?.getEntityByID(targetEntityId)

            if (attacker !is EntityPlayer || target !is EntityPlayer || attacker === target) return@scheduleTask

            // discard attacks when target is near render distance edge
            val xDiff = abs(mc.thePlayer.posX - target.posX)
            val zDiff = abs(mc.thePlayer.posZ - target.posZ)
            if (xDiff > 56 || zDiff > 56) return@scheduleTask

            // discard if too far apart
            if (attacker.getDistanceSqToEntity(target) > 64) return@scheduleTask

            // validate based on attack type
            when (attackType) {
                AttackType.HURT_SOUND, AttackType.DEATH_SOUND,
                AttackType.DIRECT_HURT_SOUND, AttackType.DIRECT_DEATH_SOUND -> {
                    if (soundPos == null) return@scheduleTask
                    if (abs(soundPos.xCoord - target.posX) >= 1 ||
                        abs(soundPos.yCoord - target.posY) >= 1 ||
                        abs(soundPos.zCoord - target.posZ) >= 1
                    ) return@scheduleTask
                    confirmAttack(attacker, target, attackType)
                }

                AttackType.VELOCITY, AttackType.DIRECT_VELOCITY -> {
                    if (mc.thePlayer == target) {
                        confirmAttack(attacker, target, attackType)
                    }
                }

                AttackType.CRITICAL, AttackType.DIRECT_CRITICAL -> {
                    // critical requires not riding
                    if (attacker.ridingEntity == null) {
                        confirmAttack(attacker, target, attackType)
                    }
                }

                AttackType.SHARPNESS, AttackType.DIRECT_SHARPNESS -> {
                    // sharpness requires enchanted sword/tool
                    val heldItem = attacker.heldItem
                    if (heldItem != null) {
                        val item = heldItem.item
                        if ((item is ItemSword || item is ItemTool) && heldItem.isItemEnchanted) {
                            confirmAttack(attacker, target, attackType)
                        }
                    }
                }
            }
        }
    }

    private fun confirmAttack(attacker: EntityPlayer, target: EntityPlayer, attackType: AttackType) {
        PlayerHandler.get(attacker)?.interaction?.onAttackConfirmed(target, attackType)
        PlayerHandler.get(target)?.interaction?.onHitReceived(attacker)

        val typeStr = attackType.name.lowercase().replace("_", " ")
        ChatUtil.sendDebug("§aAttack: §f${attacker.name} §7-> §f${target.name} §7(§e$typeStr§7)")
    }


    // mining detection
    private fun handleBlockBreakAnim(packet: S25PacketBlockBreakAnim, currentTime: Long) {
        val entityId = packet.breakerId
        val pos = packet.position
        val progress = packet.progress // 0-9 for progress, 10 or 255 to remove

        if (progress !in 0..9) {
            // animation removed (block broken or mining stopped)
            activeMining.remove(pos)
            return
        }

        activeMining[pos] = MiningInfo(entityId, pos, progress, currentTime)

        // debug log high progress mining
        if (progress >= 7) {
            scheduleTask {
                val entity = mc.theWorld?.getEntityByID(entityId)
                if (entity is EntityPlayer && entity != mc.thePlayer) {
                    ChatUtil.sendDebug("Mining: §f${entity.name} §7at §e${pos.x}, ${pos.y}, ${pos.z} §7(§c${progress}/9§7)")
                }
            }
        }
    }

    private fun handleBlockChange(packet: S23PacketBlockChange, currentTime: Long) {
        val pos = packet.blockPosition
        val newBlock = packet.blockState?.block
        val wasAir = mc.theWorld?.getBlockState(pos)?.block == Blocks.air

        // block broken (was being mined, now air)
        val miningInfo = activeMining.remove(pos)
        if (miningInfo != null && newBlock == Blocks.air && currentTime - miningInfo.time < 500) {
            scheduleTask {
                val entity = mc.theWorld?.getEntityByID(miningInfo.entityId)
                if (entity is EntityPlayer && entity != mc.thePlayer) {
                    PlayerHandler.get(entity)?.interaction?.onBlockMined(pos)
                    ChatUtil.sendDebug("§aMined: §f${entity.name} §7at §e${pos.x}, ${pos.y}, ${pos.z}")
                }
            }
            return
        }

        // block placed (was air, now something)
        if (wasAir && newBlock != null && newBlock != Blocks.air) {
            scheduleTask {
                // find player who recently swung and is close to this position
                // TODO: add raycast validation
                val placer = findPlacer(pos, currentTime)
                if (placer != null && placer != mc.thePlayer) {
                    PlayerHandler.get(placer)?.interaction?.onBlockPlaced(pos)
                    ChatUtil.sendDebug("§aPlaced: §f${placer.name} §7at §e${pos.x}, ${pos.y}, ${pos.z}")
                }
            }
        }
    }

    private fun findPlacer(pos: BlockPos, currentTime: Long): EntityPlayer? {
        // find recent swing that's close to the placed block
        val swing = recentSwings.lastOrNull { info ->
            currentTime - info.time < 200 // within 200ms
        } ?: return null

        val entity = mc.theWorld?.getEntityByID(swing.entityId) as? EntityPlayer ?: return null

        // check distance (6 in case of ping spikes, TODO: possibly link with network state)
        val dist = entity.getDistanceSq(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5)
        if (dist > 36) return null

        return entity
    }

    private inline fun scheduleTask(crossinline task: () -> Unit) {
        DelayedTaskHandler.schedule(0) { task() }
    }
}
