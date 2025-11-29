package org.afterlike.lucid.core.handler

import net.minecraft.client.Minecraft
import org.afterlike.lucid.check.handler.CheckHandler
import org.afterlike.lucid.core.type.ConfigEntry
import org.apache.logging.log4j.LogManager
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.util.Properties
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

object ConfigHandler {
    private val logger = LogManager.getLogger(ConfigHandler::class.java)
    private val configEntries = ConcurrentHashMap<String, ConfigEntry<*>>()
    private val configLock = ReentrantReadWriteLock()

    private val configFile by lazy {
        try {
            val mcDir = Minecraft.getMinecraft()?.mcDataDir ?: File(".")
            File(mcDir, "config/lucid.properties").apply {
                parentFile?.mkdirs()
            }
        } catch (e: Exception) {
            logger.error("Failed to create config file path: ${e.message}")
            File("config/lucid.properties")
        }
    }

    val playSoundOnFlag = register("flags.playSound", true)
    val verboseMode = register("flags.verbose", false)
    val flagCooldown = register("flags.cooldown", 5) { it in 1..60 }

    val messageColor = register("appearance.color", "3") { it.length == 1 && it[0] in "0123456789abcdef" }
    val messageBold = register("appearance.bold", false)
    val messageSymbol = register("appearance.symbol", ">") { it == ">" || it == "»" }
    val showVLInFlag = register("appearance.showVL", false)
    val showWDR = register("appearance.showWDR", true)

    private fun <T> register(key: String, defaultValue: T, validator: (T) -> Boolean = { true }): ConfigEntry<T> {
        val entry = ConfigEntry(key, defaultValue, validator)
        configEntries[key] = entry
        return entry
    }

    fun registerCheckConfig(checkName: String, enabled: Boolean, vlThreshold: Int): Pair<ConfigEntry<Boolean>, ConfigEntry<Int>> {
        val enabledEntry = register("check.$checkName.enabled", enabled)
        val thresholdEntry = register("check.$checkName.vlThreshold", vlThreshold) { it > 0 }
        return Pair(enabledEntry, thresholdEntry)
    }

    fun load() {
        configLock.read {
            try {
                if (!configFile.exists()) {
                    logger.info("Config file not found, creating with defaults")
                    save()
                    return
                }

                val properties = Properties()
                BufferedReader(FileReader(configFile)).use { reader ->
                    properties.load(reader)
                }

                var loadedCount = 0
                var failedCount = 0

                configEntries.forEach { (key, entry) ->
                    val stringValue = properties.getProperty(key)
                    if (stringValue != null) {
                        if (entry.deserialize(stringValue)) {
                            loadedCount++
                        } else {
                            logger.warn("Failed to parse config value for '$key': $stringValue")
                            failedCount++
                        }
                    }
                }

                logger.info("Loaded $loadedCount config entries${if (failedCount > 0) " ($failedCount failed)" else ""}")

                CheckHandler.getChecks().forEach { check ->
                    try {
                        val (enabledEntry, thresholdEntry) = registerCheckConfig(
                            check.name,
                            check.enabled,
                            check.violationLevelThreshold
                        )
                        check.enabled = enabledEntry.value
                        check.violationLevelThreshold = thresholdEntry.value
                    } catch (e: Exception) {
                        logger.error("Failed to load config for check ${check.name}: ${e.message}")
                    }
                }

            } catch (e: Exception) {
                logger.error("Failed to load config: ${e.message}", e)
                resetToDefaults()
            }
        }
    }

    fun save() {
        configLock.write {
            try {
                configFile.parentFile?.mkdirs()

                CheckHandler.getChecks().forEach { check ->
                    try {
                        val (enabledEntry, thresholdEntry) = registerCheckConfig(
                            check.name,
                            check.enabled,
                            check.violationLevelThreshold
                        )
                        enabledEntry.value = check.enabled
                        thresholdEntry.value = check.violationLevelThreshold
                    } catch (e: Exception) {
                        logger.error("Failed to save config for check ${check.name}: ${e.message}")
                    }
                }

                val properties = Properties()
                configEntries.forEach { (key, entry) ->
                    properties.setProperty(key, entry.serialize())
                }

                BufferedWriter(FileWriter(configFile)).use { writer ->
                    properties.store(writer, "Lucid Configuration")
                }

                logger.info("Saved ${configEntries.size} config entries")

            } catch (e: Exception) {
                logger.error("Failed to save config: ${e.message}", e)
            }
        }
    }

    fun resetToDefaults() {
        configLock.write {
            configEntries.values.forEach { it.reset() }
            logger.info("Reset all config entries to defaults")
            try {
                save()
            } catch (e: Exception) {
                logger.error("Failed to save default config: ${e.message}")
            }
        }
    }

    fun getFormattedPrefix(): String {
        val boldCode = if (messageBold.value) "§l" else ""
        return "§${messageColor.value}$boldCode" + "Lucid §8${messageSymbol.value} "
    }

    fun getEntry(key: String): ConfigEntry<*>? = configEntries[key]

    fun getAllEntries(): Map<String, ConfigEntry<*>> = configEntries.toMap()
}