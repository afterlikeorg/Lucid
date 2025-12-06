package org.afterlike.lucid.check.impl

import net.minecraft.entity.player.EntityPlayer
import org.afterlike.lucid.check.api.AbstractCheck

// todo: completely rewrite
class VelocityCheck : AbstractCheck() {
    override val name = "Velocity"
    override val description = "Detects irregular knockback."

    override fun onCheckRun(target: EntityPlayer) {
        TODO("Not yet implemented")
    }
}

