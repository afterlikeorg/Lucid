package org.afterlike.lucid.data.context.impl

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.BlockPos
import org.afterlike.lucid.data.context.api.AbstractContext

// tracks interactions between player - player, player - world
class InteractionContext : AbstractContext() {

    // attack tracking (this player as attacker)
    var lastAttackTime = 0L
    var lastAttackTarget: EntityPlayer? = null
    var lastAttackType: AttackType? = null
    var attackCount = 0
    var consecutiveAttacks = 0
    var multiTargetAttack = false

    // being attacked tracking
    var lastHitReceivedTime = 0L
    var lastAttacker: EntityPlayer? = null
    var hitReceivedCount = 0

    // packet correlation state
    var pendingSwing = false
    var pendingSwingTime = 0L
    var pendingHurt = false
    var pendingHurtTime = 0L
    var consecutiveSwingHurt = false

    var wasCritical = false
    var wasEnchantHit = false

    // block mining tracking
    var lastBlockMinedTime = 0L
    var lastBlockMinedPos: BlockPos? = null
    var blocksMined = 0
    var blocksMinedRecently = 0

    // block placing tracking
    var lastBlockPlacedTime = 0L
    var lastBlockPlacedPos: BlockPos? = null
    var blocksPlaced = 0
    var blocksPlacedRecently = 0

    private var lastMiningSecond = 0L
    private var lastPlacingSecond = 0L

    override fun update(player: EntityPlayer, worldTick: Long) {
        tick = worldTick
        timestamp = System.currentTimeMillis()

        if (timestamp - pendingSwingTime > 50) pendingSwing = false
        if (timestamp - pendingHurtTime > 50) {
            pendingHurt = false
            consecutiveSwingHurt = false
        }
        if (timestamp - lastAttackTime > 500) {
            consecutiveAttacks = 0
            multiTargetAttack = false
        }

        if (timestamp - lastMiningSecond > 1000) {
            blocksMinedRecently = 0
            lastMiningSecond = timestamp
        }
        if (timestamp - lastPlacingSecond > 1000) {
            blocksPlacedRecently = 0
            lastPlacingSecond = timestamp
        }
    }

    override fun reset() {
        lastAttackTime = 0; lastAttackTarget = null; lastAttackType = null
        attackCount = 0; consecutiveAttacks = 0; multiTargetAttack = false
        lastHitReceivedTime = 0; lastAttacker = null; hitReceivedCount = 0
        pendingSwing = false; pendingSwingTime = 0
        pendingHurt = false; pendingHurtTime = 0; consecutiveSwingHurt = false
        wasCritical = false; wasEnchantHit = false
        lastBlockMinedTime = 0; lastBlockMinedPos = null
        blocksMined = 0; blocksMinedRecently = 0; lastMiningSecond = 0
        lastBlockPlacedTime = 0; lastBlockPlacedPos = null
        blocksPlaced = 0; blocksPlacedRecently = 0; lastPlacingSecond = 0
        tick = 0; timestamp = 0
    }

    fun onSwingDetected() {
        pendingHurt = false
        consecutiveSwingHurt = false
        pendingSwing = true
        pendingSwingTime = System.currentTimeMillis()
    }

    fun onHurtDetected() {
        if (pendingSwing) consecutiveSwingHurt = true
        pendingSwing = false
        pendingHurt = true
        pendingHurtTime = System.currentTimeMillis()
    }

    fun onAttackConfirmed(target: EntityPlayer?, type: AttackType) {
        val now = System.currentTimeMillis()

        if (lastAttackTarget != null && lastAttackTarget != target && now - lastAttackTime < 100) {
            multiTargetAttack = true
        }

        consecutiveAttacks = if (now - lastAttackTime < 500) consecutiveAttacks + 1 else 1

        lastAttackTime = now
        lastAttackTarget = target
        lastAttackType = type
        attackCount++

        wasCritical = type == AttackType.CRITICAL || type == AttackType.DIRECT_CRITICAL
        wasEnchantHit = type == AttackType.SHARPNESS || type == AttackType.DIRECT_SHARPNESS
    }

    fun onHitReceived(attacker: EntityPlayer) {
        lastHitReceivedTime = System.currentTimeMillis()
        lastAttacker = attacker
        hitReceivedCount++
    }

    fun onBlockMined(pos: BlockPos) {
        lastBlockMinedTime = System.currentTimeMillis()
        lastBlockMinedPos = pos
        blocksMined++
        blocksMinedRecently++
    }

    fun onBlockPlaced(pos: BlockPos) {
        lastBlockPlacedTime = System.currentTimeMillis()
        lastBlockPlacedPos = pos
        blocksPlaced++
        blocksPlacedRecently++
    }

    fun isDirectAttack(): Boolean = pendingSwing && System.currentTimeMillis() - pendingSwingTime < 2

    enum class AttackType {
        VELOCITY, DIRECT_VELOCITY,
        CRITICAL, DIRECT_CRITICAL,
        SHARPNESS, DIRECT_SHARPNESS,
        HURT_SOUND, DIRECT_HURT_SOUND,
        DEATH_SOUND, DIRECT_DEATH_SOUND
    }
}
