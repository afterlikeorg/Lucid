package org.afterlike.lucid.core.type

class PlayerSample {
    var posX: Double = 0.0
    var posY: Double = 0.0
    var posZ: Double = 0.0

    var prevPosX: Double = 0.0
    var prevPosY: Double = 0.0
    var prevPosZ: Double = 0.0

    var motionX: Double = 0.0
    var motionY: Double = 0.0
    var motionZ: Double = 0.0

    var yaw: Float = 0f
    var pitch: Float = 0f

    var prevYaw: Float = 0f
    var prevPitch: Float = 0f

    var onGround: Boolean = false
    var isSprinting: Boolean = false
    var hurtTime: Int = 0

    var tick: Long = 0
    var timeStamp: Long = 0

    val deltaX: Double
        get() = posX - prevPosX

    val deltaY: Double
        get() = posY - prevPosY

    val deltaZ: Double
        get() = posZ - prevPosZ
}