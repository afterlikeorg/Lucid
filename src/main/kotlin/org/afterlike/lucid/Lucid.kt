package org.afterlike.lucid

import net.minecraftforge.client.ClientCommandHandler
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent
import org.afterlike.lucid.check.*
import org.afterlike.lucid.command.LucidCommand
import org.afterlike.lucid.event.EventHandler
import org.afterlike.lucid.util.Config

@Mod(modid = "lucid", useMetadata = true)
class Lucid {
    companion object {
        const val MOD_NAME = "Lucid"
        const val VERSION = "1.0"
    }

    @Mod.EventHandler
    fun preInit(event: FMLPreInitializationEvent) {
        MinecraftForge.EVENT_BUS.register(this)
    }

    @Mod.EventHandler
    fun init(event: FMLInitializationEvent) {
        try {
            val eventHandler = EventHandler()
            eventHandler.registerEvents()

            ClientCommandHandler.instance.registerCommand(LucidCommand())

            initChecks()

            Config.load()

            Runtime.getRuntime().addShutdownHook(Thread {
                try {
                    shutdown()
                } catch (e: Exception) {
                    System.err.println("[Lucid] Error during shutdown: ${e.message}")
                }
            })

            println("[Lucid] Anti-cheat initialized successfully")
        } catch (e: Exception) {
            System.err.println("[Lucid] Error during initialization: ${e.message}")
            e.printStackTrace()
        }
    }

    @Mod.EventHandler
    fun onServerStopping(event: FMLServerStoppingEvent) {
        Config.save()
        shutdown()
    }

    private fun shutdown() {
        try {
            println("[Lucid] Shutting down resources...")
            CheckManager.shutdown()
            println("[Lucid] Shutdown complete")
        } catch (e: Exception) {
            System.err.println("[Lucid] Error during shutdown: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun initChecks() {
        try {
            println("[Lucid] Initializing checks...")

            val checks = listOf(
                { AutoBlockCheck() },
                { EagleCheck() },
                { NoSlowCheck() },
                { RotationCheck() },
                { ScaffoldCheck() },
                { SprintCheck() },
                { VelocityCheck() }
            )

            var initializedCount = 0
            for (checkInitializer in checks) {
                try {
                    checkInitializer()
                    initializedCount++
                } catch (e: Exception) {
                    System.err.println("[Lucid] Failed to initialize a check: ${e.message}")
                    e.printStackTrace()
                }
            }

            println("[Lucid] Initialized $initializedCount/${checks.size} checks")

            if (initializedCount < checks.size) {
                println("[Lucid] WARNING: Some checks failed to initialize!")
            }

            if (CheckManager.allChecks().isEmpty()) {
                println("[Lucid] CRITICAL ERROR: No checks were registered!")
            }

        } catch (e: Exception) {
            System.err.println("[Lucid] Error initializing checks: ${e.message}")
            e.printStackTrace()
        }
    }
}