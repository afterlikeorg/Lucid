package org.afterlike.lucid.check.impl

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemBow
import net.minecraft.item.ItemFood
import net.minecraft.item.ItemPotion
import net.minecraft.item.ItemSword
import org.afterlike.lucid.check.api.BaseCheck
import org.afterlike.lucid.core.handler.PlayerSampleHandler
import org.afterlike.lucid.core.event.world.EntityLeaveEvent
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min

class NoSlowCheck : BaseCheck() {
    override val name = "NoSlow"
    override val description = "Detects illegal movement while using items (eating, drinking, etc.)"

    private val lastUsingTick = ConcurrentHashMap<EntityPlayer, Long>()
    private val lastItemSwapTick = ConcurrentHashMap<EntityPlayer, Long>()
    private val lastStopUsingTick = ConcurrentHashMap<EntityPlayer, Long>()
    private val lastUsedItemName = ConcurrentHashMap<EntityPlayer, String>()
    private val consecutiveViolations = ConcurrentHashMap<EntityPlayer, Int>()

    override fun onCheckRun(target: EntityPlayer) {
        if (target == mc.thePlayer) return

        val currentSample = PlayerSampleHandler.getLatestSample(target) ?: return
        val currentTick = currentSample.tick

        val isSprinting = target.isSprinting
        val isUsingItem = target.isUsingItem
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

                    val speed = calculateSpeed(currentSample.deltaX, currentSample.deltaZ)

                    val consecutive = consecutiveViolations.getOrDefault(target, 0) + 1
                    consecutiveViolations[target] = consecutive

                    val baseVL = 1.0
                    val consecutiveMultiplier = 1.0 + (min(consecutive - 1, 4) * 0.25)
                    val finalVL = baseVL * consecutiveMultiplier

                    val itemName = heldItem?.displayName ?: "unknown item"

                    addVL(
                        target,
                        finalVL,
                        "no-slowdown | item=$itemName ($itemType) | speed=${"%.2f".format(speed)} | " +
                                "sprinting=true | consecutive=$consecutive | vl=${"%.1f".format(finalVL)}"
                    )
                }
            }
        } else {
            val currentVL = getPlayerVL(target)
            if (currentVL > 0) {
                val decayRate = when {
                    currentVL > 7.5 -> 0.5
                    currentVL > 5.0 -> 0.4
                    currentVL > 2.5 -> 0.3
                    else -> 0.2
                }

                decayVL(target, decayRate)

                if (currentVL <= violationLevelThreshold * 0.2) {
                    consecutiveViolations[target] = 0
                }
            }
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