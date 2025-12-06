package org.afterlike.lucid.check.impl

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemBow
import net.minecraft.item.ItemFood
import net.minecraft.item.ItemPotion
import net.minecraft.item.ItemSword
import org.afterlike.lucid.check.api.AbstractCheck
import org.afterlike.lucid.core.event.world.EntityLeaveEvent
import org.afterlike.lucid.data.handler.impl.PlayerHandler
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min

class NoSlowCheck : AbstractCheck() {
    override val name = "NoSlow"
    override val description = "Detects illegal movement while using items (eating, drinking, etc.)"

    override val decayConfig = DecayConfig(
        baseRate = 0.2,
        mediumRate = 0.3,
        highRate = 0.4,
        criticalRate = 0.5,
        resetThreshold = 0.2
    )

    private val lastUsingTick = ConcurrentHashMap<EntityPlayer, Long>()
    private val lastItemSwapTick = ConcurrentHashMap<EntityPlayer, Long>()
    private val lastStopUsingTick = ConcurrentHashMap<EntityPlayer, Long>()
    private val lastUsedItemName = ConcurrentHashMap<EntityPlayer, String>()
    private val consecutiveViolations = ConcurrentHashMap<EntityPlayer, Int>()

    override fun onCheckRun(target: EntityPlayer) {
        if (target == mc.thePlayer) return

        val data = PlayerHandler.get(target) ?: return
        val ctx = data.player
        val currentTick = ctx.tick

        val isSprinting = ctx.isSprinting
        val isUsingItem = ctx.isUsingItem
        val isRiding = target.ridingEntity != null
        val heldItem = target.heldItem

        if (isUsingItem && !lastUsingTick.containsKey(target)) {
            lastUsingTick[target] = currentTick
        }

        val previousItem = lastUsedItemName[target]
        val currentItem = heldItem?.unlocalizedName ?: "empty"
        if (previousItem != null && previousItem != currentItem) {
            lastItemSwapTick[target] = currentTick
        }
        lastUsedItemName[target] = currentItem

        if (!isUsingItem && lastUsingTick.containsKey(target)) {
            lastStopUsingTick[target] = currentTick
            lastUsingTick.remove(target)
        }

        if (isUsingItem && isSprinting && !isRiding) {
            val startUsingTick = lastUsingTick[target] ?: currentTick
            val lastSwapTick = lastItemSwapTick[target] ?: 0

            if (startUsingTick - lastSwapTick > 1) {
                val stopUsingTick = lastStopUsingTick[target] ?: 0
                val ticksSinceStopUsing = currentTick - stopUsingTick

                if (ticksSinceStopUsing > 5) {
                    val itemType = when {
                        heldItem == null -> "unknown"
                        heldItem.item is ItemFood -> "food"
                        heldItem.item is ItemPotion -> "potion"
                        heldItem.item is ItemBow -> "bow"
                        heldItem.item is ItemSword -> "sword"
                        else -> "item"
                    }

                    val speed = calculateSpeed(ctx.deltaX, ctx.deltaZ)

                    val consecutive = consecutiveViolations.getOrDefault(target, 0) + 1
                    consecutiveViolations[target] = consecutive

                    val baseVL = 1.0
                    val consecutiveMultiplier = 1.0 + (min(consecutive - 1, 4) * 0.25)
                    val finalVL = baseVL * consecutiveMultiplier

                    val itemName = heldItem?.displayName ?: "unknown item"

                    addVL(
                        target, finalVL,
                        "no-slowdown | item=$itemName ($itemType) | speed=${"%.2f".format(speed)} | sprinting=true | consecutive=$consecutive | vl=${
                            "%.1f".format(
                                finalVL
                            )
                        }"
                    )
                    return
                }
            }
        }

        if (handleNoViolation(target)) {
            consecutiveViolations[target] = 0
        }
    }

    override fun onPlayerLeave(event: EntityLeaveEvent) {
        val player = event.entity
        lastUsingTick.remove(player)
        lastItemSwapTick.remove(player)
        lastStopUsingTick.remove(player)
        lastUsedItemName.remove(player)
        consecutiveViolations.remove(player)
    }
}
