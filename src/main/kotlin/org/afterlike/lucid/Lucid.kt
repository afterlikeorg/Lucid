package org.afterlike.lucid

import net.minecraftforge.client.ClientCommandHandler
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent
import org.afterlike.lucid.check.*
import org.afterlike.lucid.check.example.ScaffoldCheck
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


            println("[Lucid] Anti-cheat initialized successfully")
        } catch (e: Exception) {
            System.err.println("[Lucid] Error during initialization: ${e.message}")
            e.printStackTrace()
        }
    }

    @Mod.EventHandler
    fun onServerStopping(event: FMLServerStoppingEvent) {

        Config.save()
    }

    private fun initChecks() {
        try {
            System.out.println("[Lucid] Initializing checks...")

            AutoBlockCheck()
            EagleCheck()
            NoSlowCheck()
            RotationCheck()
            ScaffoldCheck()
            SprintCheck()
            VelocityCheck()

            println("[Lucid] Initialized ${CheckManager.allChecks().size} checks")
        } catch (e: Exception) {
            System.err.println("[Lucid] Error initializing checks: ${e.message}")
            e.printStackTrace()
        }
    }
}