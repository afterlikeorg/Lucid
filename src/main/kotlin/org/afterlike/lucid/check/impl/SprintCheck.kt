package org.afterlike.lucid.check.impl

import net.minecraft.entity.player.EntityPlayer
import org.afterlike.lucid.check.api.AbstractCheck

// todo: completely rewrite
class SprintCheck : AbstractCheck() {
    override val name = "Sprint"
    override val description = "Detects illegal sprinting patterns."

    override fun onCheckRun(target: EntityPlayer) {
        TODO("Not yet implemented")
    }
}