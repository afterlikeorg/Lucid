package org.afterlike.lucid.core

import best.azura.eventbus.core.EventBus
import net.minecraftforge.client.ClientCommandHandler
import org.afterlike.lucid.check.handler.CheckHandler
import org.afterlike.lucid.core.command.LucidCommand
import org.afterlike.lucid.core.handler.ConfigHandler
import org.afterlike.lucid.core.handler.DelayedTaskHandler
import org.afterlike.lucid.data.handler.impl.InteractionHandler
import org.afterlike.lucid.data.handler.impl.NetworkHandler
import org.afterlike.lucid.data.handler.impl.PlayerHandler
import org.afterlike.lucid.data.handler.impl.WorldHandler
import org.apache.logging.log4j.LogManager

class Lucid {

    private val logger = LogManager.getLogger(Lucid::class.java)

    companion object {
        @JvmStatic
        val INSTANCE: Lucid = Lucid()
    }

    val eventBus = EventBus()
    val checkHandler = CheckHandler

    fun initialize() {
        val start = System.nanoTime()
        logger.info("Initializing Lucid...")

        eventBus.subscribe(DelayedTaskHandler)
        eventBus.subscribe(CheckHandler)

        // data handlers
        eventBus.subscribe(PlayerHandler)
        eventBus.subscribe(NetworkHandler)
        eventBus.subscribe(WorldHandler)
        eventBus.subscribe(InteractionHandler)

        checkHandler.initialize()

        logger.info("Initialized Lucid in {}ms.", String.format("%.2f", (System.nanoTime() - start) / 1_000_000.0))
    }

    fun lateInitialize() {
        ClientCommandHandler.instance.registerCommand(LucidCommand())
        ConfigHandler.load()
    }
}
