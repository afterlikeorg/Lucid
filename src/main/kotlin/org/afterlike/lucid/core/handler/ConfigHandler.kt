package org.afterlike.lucid.core.handler

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import net.minecraft.client.Minecraft
import org.afterlike.lucid.check.handler.CheckHandler
import org.apache.logging.log4j.LogManager
import java.io.File

object ConfigHandler {

    private val logger = LogManager.getLogger(ConfigHandler::class.java)
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    private val configFile: File by lazy {
        val mcDir = Minecraft.getMinecraft()?.mcDataDir ?: File(".")
        File(mcDir, "config/lucid.json").apply { parentFile?.mkdirs() }
    }

    private var config = LucidConfig()

    var playSoundOnFlag: Boolean
        get() = config.settings.playSoundOnFlag
        set(value) {
            config.settings.playSoundOnFlag = value
        }

    var verboseMode: Boolean
        get() = config.settings.verboseMode
        set(value) {
            config.settings.verboseMode = value
        }

    var debugMode: Boolean
        get() = config.settings.debugMode
        set(value) {
            config.settings.debugMode = value
        }

    var flagCooldown: Int
        get() = config.settings.flagCooldown
        set(value) {
            config.settings.flagCooldown = value.coerceIn(1, 60)
        }

    var messageColor: String
        get() = config.appearance.color
        set(value) {
            if (value.length == 1 && value[0] in "0123456789abcdef") {
                config.appearance.color = value
            }
        }

    var messageBold: Boolean
        get() = config.appearance.bold
        set(value) {
            config.appearance.bold = value
        }

    var messageSymbol: String
        get() = config.appearance.symbol
        set(value) {
            if (value == ">" || value == "»") {
                config.appearance.symbol = value
            }
        }

    var showVLInFlag: Boolean
        get() = config.appearance.showVL
        set(value) {
            config.appearance.showVL = value
        }

    var showWDR: Boolean
        get() = config.appearance.showWDR
        set(value) {
            config.appearance.showWDR = value
        }

    fun load() {
        if (!configFile.exists()) {
            logger.info("Config file not found, creating with defaults")
            save()
            return
        }

        val loadedConfig = configFile.reader().use { reader ->
            gson.fromJson(reader, LucidConfig::class.java)
        }

        if (loadedConfig != null) {
            config = loadedConfig
            logger.info("Loaded config from ${configFile.path}")
        }

        syncChecksFromConfig()
    }

    fun save() {
        syncChecksToConfig()

        configFile.writer().use { writer ->
            gson.toJson(config, writer)
        }
        logger.info("Saved config to ${configFile.path}")
    }

    private fun syncChecksFromConfig() {
        CheckHandler.getChecks().forEach { check ->
            config.checks[check.name]?.let { checkConfig ->
                check.enabled = checkConfig.enabled
                check.violationLevelThreshold = checkConfig.vlThreshold
            }
        }
    }

    private fun syncChecksToConfig() {
        CheckHandler.getChecks().forEach { check ->
            config.checks[check.name] = CheckConfig(
                enabled = check.enabled,
                vlThreshold = check.violationLevelThreshold
            )
        }
    }

    fun getFormattedPrefix(): String {
        val boldCode = if (messageBold) "§l" else ""
        return "§${messageColor}$boldCode" + "Lucid §8${messageSymbol} "
    }

    data class LucidConfig(
        val settings: SettingsConfig = SettingsConfig(),
        val appearance: AppearanceConfig = AppearanceConfig(),
        val checks: MutableMap<String, CheckConfig> = mutableMapOf()
    )

    data class SettingsConfig(
        var playSoundOnFlag: Boolean = true,
        var verboseMode: Boolean = false,
        var debugMode: Boolean = false,
        var flagCooldown: Int = 5
    )

    data class AppearanceConfig(
        var color: String = "3",
        var bold: Boolean = false,
        var symbol: String = ">",
        var showVL: Boolean = false,
        var showWDR: Boolean = true
    )

    data class CheckConfig(
        var enabled: Boolean = true,
        var vlThreshold: Int = 10
    )
}
