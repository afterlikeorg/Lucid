package org.afterlike.lucid.util

import net.minecraft.client.Minecraft
import org.afterlike.lucid.check.CheckManager
import java.io.*
import java.util.*
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

object Config {

    var playSoundOnFlag: Boolean = true
    var verboseMode: Boolean = false
    var flagCooldown: Int = 5
    
    // Message format settings
    var messageColor: String = "3" // Default cyan color
    var messageBold: Boolean = false
    var messageSymbol: String = ">" // Alternative: "»"
    var showVLInFlag: Boolean = false
    var showWDR: Boolean = true

    private val configFile by lazy {
        try {
            val mcDir = Minecraft.getMinecraft()?.mcDataDir ?: File(".")
            File(mcDir, "lucid/config.properties").apply {

                parentFile?.mkdirs()
            }
        } catch (e: Exception) {
            logError("Error creating config file path: ${e.message}")
            File("lucid/config.properties")
        }
    }

    private val properties = Properties()

    private val configLock = ReentrantReadWriteLock()

    fun load() {
        configLock.read {
            try {
                if (configFile.exists()) {
                    BufferedReader(FileReader(configFile)).use { reader ->
                        properties.load(reader)
                    }


                    playSoundOnFlag = getProperty("playSoundOnFlag", true)
                    verboseMode = getProperty("verboseMode", false)
                    flagCooldown = getProperty("flagCooldown", 5)
                    
                    // Load message format settings
                    messageColor = getProperty("messageColor", "3")
                    messageBold = getProperty("messageBold", false)
                    messageSymbol = getProperty("messageSymbol", ">")
                    showVLInFlag = getProperty("showVLInFlag", false)
                    showWDR = getProperty("showWDR", true)


                    CheckManager.allChecks().forEach { check ->
                        try {
                            check.enabled = getProperty("check.${check.name}.enabled", check.enabled)
                            check.vlThreshold = getProperty("check.${check.name}.vlThreshold", check.vlThreshold)
                        } catch (e: Exception) {
                            logError("Error loading config for check ${check.name}: ${e.message}")
                        }
                    }
                } else {

                    save()
                }
            } catch (e: Exception) {
                logError("Error loading config: ${e.message}")
                e.printStackTrace()


                resetToDefaults()
            }
        }
    }

    fun save() {
        configLock.write {
            try {

                configFile.parentFile?.mkdirs()


                properties.setProperty("playSoundOnFlag", playSoundOnFlag.toString())
                properties.setProperty("verboseMode", verboseMode.toString())
                properties.setProperty("flagCooldown", flagCooldown.toString())
                
                // Save message format settings
                properties.setProperty("messageColor", messageColor)
                properties.setProperty("messageBold", messageBold.toString())
                properties.setProperty("messageSymbol", messageSymbol)
                properties.setProperty("showVLInFlag", showVLInFlag.toString())
                properties.setProperty("showWDR", showWDR.toString())


                CheckManager.allChecks().forEach { check ->
                    try {
                        properties.setProperty("check.${check.name}.enabled", check.enabled.toString())
                        properties.setProperty("check.${check.name}.vlThreshold", check.vlThreshold.toString())
                    } catch (e: Exception) {
                        logError("Error saving config for check ${check.name}: ${e.message}")
                    }
                }


                BufferedWriter(FileWriter(configFile)).use { writer ->
                    properties.store(writer, "Lucid Anti-Cheat Configuration")
                }
            } catch (e: Exception) {
                logError("Error saving config: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun resetToDefaults() {
        playSoundOnFlag = true
        verboseMode = false
        flagCooldown = 5
        
        // Reset message format settings
        messageColor = "3"
        messageBold = false
        messageSymbol = ">"
        showVLInFlag = false
        showWDR = true


        properties.clear()


        try {
            save()
        } catch (e: Exception) {
            logError("Error saving default config: ${e.message}")
        }
    }

    private fun getProperty(key: String, defaultValue: Boolean): Boolean {
        return try {
            properties.getProperty(key, defaultValue.toString()).toBoolean()
        } catch (e: Exception) {
            logError("Error parsing boolean property $key: ${e.message}")
            defaultValue
        }
    }

    private fun getProperty(key: String, defaultValue: Int): Int {
        return try {
            properties.getProperty(key, defaultValue.toString()).toInt()
        } catch (e: Exception) {
            logError("Error parsing integer property $key: ${e.message}")
            defaultValue
        }
    }
    
    private fun getProperty(key: String, defaultValue: String): String {
        return try {
            properties.getProperty(key, defaultValue)
        } catch (e: Exception) {
            logError("Error parsing string property $key: ${e.message}")
            defaultValue
        }
    }

    private fun logError(message: String) {
        try {
            System.err.println("[Lucid Config] $message")
        } catch (e: Exception) {

        }
    }
    
    // Get formatted prefix for chat messages
    fun getFormattedPrefix(): String {
        val boldCode = if (messageBold) "§l" else ""
        return "§${messageColor}$boldCode" + "Lucid §8" + messageSymbol + " "
    }
} 