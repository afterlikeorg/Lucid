package org.afterlike.lucid.core.type

class ConfigEntry<T>(
    val key: String,
    val defaultValue: T,
    val validator: (T) -> Boolean = { true }
) {
    var value: T = defaultValue
        set(newValue) {
            if (validator(newValue)) {
                field = newValue
            }
        }

    fun reset() {
        value = defaultValue
    }

    fun serialize(): String = value.toString()

    @Suppress("UNCHECKED_CAST")
    fun deserialize(stringValue: String): Boolean {
        return try {
            value = when (defaultValue) {
                is Boolean -> stringValue.toBoolean() as T
                is Int -> stringValue.toInt() as T
                is String -> stringValue as T
                is Double -> stringValue.toDouble() as T
                is Float -> stringValue.toFloat() as T
                else -> return false
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}