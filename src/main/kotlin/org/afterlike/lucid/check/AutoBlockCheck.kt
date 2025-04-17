package org.afterlike.lucid.check

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemSword

class AutoBlockCheck : Check() {
    override val name = "AutoBlock"
    override val description = "Detects attacking while blocking with a sword"

    private val lastSwingProgress = mutableMapOf<EntityPlayer, Float>()

    init {
        CheckManager.register(this)
        vlThreshold = 20
    }

    override fun onUpdate(target: EntityPlayer) {
        val mc = net.minecraft.client.Minecraft.getMinecraft()

        if (target == mc.thePlayer) return

        val swingProgress = target.swingProgress
        val isUsingItem = target.isUsingItem
        val heldItem = target.heldItem
        val isSword = heldItem?.item is ItemSword

        val prevSwingProgress = lastSwingProgress[target] ?: 0f
        lastSwingProgress[target] = swingProgress

        if (isUsingItem && isSword) {
            if (swingProgress > 0f && swingProgress != prevSwingProgress) {
                addVL(target, 1.0, "attacking while blocking with sword")
            }
        } else {
            val currentVL = getPlayerVL(target)
            if (currentVL > 0) {
                decayVL(target, 0.5)
            }
        }
    }
} 