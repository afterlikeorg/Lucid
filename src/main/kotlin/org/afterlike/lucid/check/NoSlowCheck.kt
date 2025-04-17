package org.afterlike.lucid.check

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemBow
import net.minecraft.item.ItemFood
import net.minecraft.item.ItemPotion
import net.minecraft.item.ItemSword

class NoSlowCheck : Check() {
    override val name = "NoSlow"
    override val description = "Detects illegal movement while using items (eating, drinking, etc.)"

    private val lastUsingTick = mutableMapOf<EntityPlayer, Int>()
    private val lastItemSwapTick = mutableMapOf<EntityPlayer, Int>()
    private val lastStopUsingTick = mutableMapOf<EntityPlayer, Int>()
    private val lastUsedItemName = mutableMapOf<EntityPlayer, String>()

    init {
        CheckManager.register(this)
        vlThreshold = 10
    }

    override fun onUpdate(target: EntityPlayer) {
        val mc = net.minecraft.client.Minecraft.getMinecraft()
        val currentTick = mc.theWorld.totalWorldTime.toInt()

        if (target == mc.thePlayer) return

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

                    addVL(target, 1.0, "sprinting while using $itemType")
                }
            }
        } else {

            val currentVL = getPlayerVL(target)
            if (currentVL > 0) {
                decayVL(target, 0.5)
            }
        }
    }
} 